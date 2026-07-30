#!/usr/bin/env bash
set -euo pipefail

# F11.3 — dump diario de MySQL + manifest de verificación + rotación
# GFS-lite + hook de copia fuera del servidor + verificación de restore
# encadenada. Pensado para correr por cron en el host del VPS (no dentro de
# un contenedor) — ver docs/RUNBOOK.md.
#
# Requiere: docker, docker compose (plugin v2), el stack de
# docker-compose.prod.yml levantado con el servicio mysql healthy.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

COMPOSE_FILE="${COMPOSE_FILE:-$REPO_ROOT/docker-compose.prod.yml}"
ENV_FILE="${ENV_FILE:-$REPO_ROOT/.env}"

if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

: "${DB_NAME:?falta DB_NAME (definir en .env o exportar antes de correr backup.sh)}"
: "${MYSQL_ROOT_PASSWORD:?falta MYSQL_ROOT_PASSWORD (definir en .env o exportar antes de correr backup.sh)}"

BACKUP_DIR="${BACKUP_DIR:-/var/backups/montanari-contable}"
DAILY_RETENTION_DAYS="${DAILY_RETENTION_DAYS:-14}"
MONTHLY_RETENTION_MONTHS="${MONTHLY_RETENTION_MONTHS:-12}"

# Tablas clave para el manifest de verificación (ver verify_restore.sh) — no
# es un listado exhaustivo de todas las tablas del sistema, es el conjunto
# que detecta un restore incompleto/corrupto.
MANIFEST_TABLES=(asiento asiento_linea cliente proveedor factura_venta factura_compra movimiento_bancario)

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

timestamp="$(date +%Y%m%d_%H%M%S)"
dump_file="$BACKUP_DIR/montanari_contable_${timestamp}.sql.gz"
manifest_file="$BACKUP_DIR/montanari_contable_${timestamp}.manifest.json"

echo "[backup] Volcando $DB_NAME -> $dump_file"
docker compose -f "$COMPOSE_FILE" exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql \
  mysqldump --single-transaction --routines --triggers --set-gtid-purged=OFF \
  -u root "$DB_NAME" \
  | gzip > "$dump_file"
chmod 600 "$dump_file"

echo "[backup] Generando manifest ($manifest_file)"
{
  echo "{"
  echo "  \"database\": \"$DB_NAME\","
  echo "  \"dump_file\": \"$(basename "$dump_file")\","
  echo "  \"created_at\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\","
  echo "  \"table_counts\": {"
  last_idx=$(( ${#MANIFEST_TABLES[@]} - 1 ))
  for i in "${!MANIFEST_TABLES[@]}"; do
    table="${MANIFEST_TABLES[$i]}"
    count="$(docker compose -f "$COMPOSE_FILE" exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql \
      mysql -N -B -u root "$DB_NAME" -e "SELECT COUNT(*) FROM ${table};" | tr -d '\r')"
    if [ "$i" -eq "$last_idx" ]; then
      echo "    \"${table}\": ${count}"
    else
      echo "    \"${table}\": ${count},"
    fi
  done
  echo "  }"
  echo "}"
} > "$manifest_file"
chmod 600 "$manifest_file"

echo "[backup] Aplicando retención (diarios: ${DAILY_RETENTION_DAYS}d, mensuales: ${MONTHLY_RETENTION_MONTHS}m)"
"$SCRIPT_DIR/prune_backups.sh" "$BACKUP_DIR" "$DAILY_RETENTION_DAYS" "$MONTHLY_RETENTION_MONTHS"

echo "[backup] Copia fuera del servidor"
if ! "$SCRIPT_DIR/offsite_hook.sh" "$dump_file"; then
  echo "[backup] ADVERTENCIA: offsite_hook.sh falló — el backup local se conserva igual." >&2
fi

echo "[backup] Verificación de restore de punta a punta"
if "$SCRIPT_DIR/verify_restore.sh" "$dump_file" "$manifest_file"; then
  echo "[backup] OK: restore verificado exitosamente."
else
  echo "[backup] ERROR: la verificación de restore falló — revisar urgente." >&2
  exit 1
fi

echo "[backup] Listo: $dump_file"

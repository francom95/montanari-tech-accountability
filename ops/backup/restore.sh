#!/usr/bin/env bash
set -euo pipefail

# Restaura un dump de backup.sh en un MySQL ya corriendo. Dos usos (ver
# docs/RUNBOOK.md § restore de desastre):
#   (a) Desastre real, contra el mysql de docker-compose.prod.yml:
#         docker compose -f docker-compose.prod.yml stop backend
#         ops/backup/restore.sh <dump.gz> --compose-service mysql
#         docker compose -f docker-compose.prod.yml start backend
#       luego verificar a mano GET /api/v1/reportes/balance-sumas-y-saldos.
#   (b) Verificación en un contenedor descartable (usado por verify_restore.sh):
#         ops/backup/restore.sh <dump.gz> --container <nombre_del_contenedor>
#
# mysqldump incluye "DROP TABLE IF EXISTS" antes de cada CREATE TABLE por
# default, así que este restore es seguro de repetir sobre un esquema ya
# poblado (recrea las tablas), no hace falta dropear la base a mano antes.

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

usage() {
  echo "Uso: $0 <dump.gz> --compose-service <servicio> | --container <nombre>" >&2
  exit 1
}

[ $# -eq 3 ] || usage
dump_file="$1"
mode="$2"
target="$3"
[ -f "$dump_file" ] || { echo "[restore] No existe: $dump_file" >&2; exit 1; }

: "${DB_NAME:?falta DB_NAME}"
: "${MYSQL_ROOT_PASSWORD:?falta MYSQL_ROOT_PASSWORD}"

echo "[restore] Restaurando $dump_file -> $target ($mode), base $DB_NAME"

case "$mode" in
  --compose-service)
    gunzip -c "$dump_file" | docker compose -f "$COMPOSE_FILE" exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$target" \
      mysql -u root "$DB_NAME"
    ;;
  --container)
    gunzip -c "$dump_file" | docker exec -i -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$target" \
      mysql -u root "$DB_NAME"
    ;;
  *)
    usage
    ;;
esac

echo "[restore] Completado."

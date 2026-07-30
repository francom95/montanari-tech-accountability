#!/usr/bin/env bash
set -euo pipefail

# Prueba de punta a punta de que un dump reciente restaura correctamente:
# levanta un MySQL descartable sin red, restaura el dump, corre dos
# chequeos (counts contra el manifest + invariante contable de balance),
# tira el contenedor. Encadenado al final de backup.sh para correr CADA
# noche, no solo una vez en el go-live — así "restore probado" es continuo,
# no un ejercicio de una sola vez.
#
# Uso: verify_restore.sh <dump.gz> <manifest.json>
# Exit 0 = PASS, no-cero = FAIL (para que cron pueda alertar).

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENV_FILE="${ENV_FILE:-$REPO_ROOT/.env}"

if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

: "${DB_NAME:?falta DB_NAME}"
: "${MYSQL_ROOT_PASSWORD:?falta MYSQL_ROOT_PASSWORD}"

usage() { echo "Uso: $0 <dump.gz> <manifest.json>" >&2; exit 1; }
[ $# -eq 2 ] || usage
dump_file="$1"
manifest_file="$2"
[ -f "$dump_file" ] || { echo "[verify_restore] No existe: $dump_file" >&2; exit 1; }
[ -f "$manifest_file" ] || { echo "[verify_restore] No existe: $manifest_file" >&2; exit 1; }

container="montanari_restore_verify_$$"
cleanup() {
  docker rm -f "$container" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "[verify_restore] Levantando MySQL descartable ($container, sin puerto publicado al host)"
# Sin -p: el contenedor no publica ningún puerto al host — solo alcanzable
# vía `docker exec` (mismo trato que el resto del stack en
# docker-compose.prod.yml). OJO: --network none se probó y se descartó — con
# --network none el propio proceso de inicialización de MySQL (que arranca
# un server temporal para correr los scripts de init) queda roto y el root
# termina sin password funcional, aunque `mysqladmin ping --silent` de un
# falso positivo (ese flag solo chequea conectividad, no autenticación).
docker run -d --name "$container" \
  -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
  -e MYSQL_DATABASE="$DB_NAME" \
  mysql:8.0 >/dev/null

echo "[verify_restore] Esperando a que levante (auth real, no solo ping — y esperando el arranque FINAL)"
# El entrypoint de mysql:8.0 arranca un server TEMPORAL para correr los
# scripts de init (que ya acepta auth con la password final) y LUEGO lo para
# y arranca el server definitivo — hay una ventana de milisegundos/segundos
# entre ambos donde el socket no responde. Un solo chequeo de auth puede caer
# justo en la ventana del server temporal y dar un falso "listo" que se cae
# un instante después (visto en la práctica: "Can't connect ... through
# socket"). Por eso se exige, además de la auth, ver DOS apariciones de
# "ready for connections" en el log (temporal + definitivo).
ready=0
for i in $(seq 1 30); do
  ready_msgs="$(docker logs "$container" 2>&1 | grep -c "ready for connections" || true)"
  if [ "$ready_msgs" -ge 2 ] && docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$container" mysql -u root -e "SELECT 1;" >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 2
done
if [ "$ready" -ne 1 ]; then
  echo "[verify_restore] FAIL: MySQL descartable no levantó a tiempo" >&2
  exit 1
fi

echo "[verify_restore] Restaurando dump"
"$SCRIPT_DIR/restore.sh" "$dump_file" --container "$container"

fail=0

echo "[verify_restore] Chequeo 1/2: counts exactos contra el manifest"
tables="$(grep -oE '"[a-z_]+": [0-9]+' "$manifest_file")"
while IFS= read -r line; do
  [ -z "$line" ] && continue
  table="$(echo "$line" | cut -d'"' -f2)"
  expected="$(echo "$line" | grep -oE '[0-9]+$')"
  actual="$(docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$container" \
    mysql -N -B -u root "$DB_NAME" -e "SELECT COUNT(*) FROM ${table};" | tr -d '\r')"
  if [ "$actual" != "$expected" ]; then
    echo "[verify_restore] MISMATCH en $table: esperado=$expected, restaurado=$actual" >&2
    fail=1
  else
    echo "[verify_restore]   $table: $actual OK"
  fi
done <<< "$tables"

echo "[verify_restore] Chequeo 2/2: invariante contable (suma debe = suma haber, asientos CONFIRMADO)"
balance_row="$(docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$container" \
  mysql -N -B -u root "$DB_NAME" -e "
    SELECT COALESCE(SUM(al.debe),0), COALESCE(SUM(al.haber),0)
    FROM asiento_linea al
    JOIN asiento a ON a.id = al.asiento_id
    WHERE a.estado = 'CONFIRMADO';
  " | tr -d '\r')"
suma_debe="$(echo "$balance_row" | cut -f1)"
suma_haber="$(echo "$balance_row" | cut -f2)"
if [ "$suma_debe" != "$suma_haber" ]; then
  echo "[verify_restore] MISMATCH de balance: debe=$suma_debe haber=$suma_haber" >&2
  fail=1
else
  echo "[verify_restore]   balance OK: debe=haber=$suma_debe"
fi

if [ "$fail" -eq 0 ]; then
  echo "[verify_restore] PASS"
  exit 0
else
  echo "[verify_restore] FAIL"
  exit 1
fi

#!/usr/bin/env bash
set -euo pipefail

# Smoke test de docker-compose.prod.yml: levanta el stack con secretos
# descartables (y un certificado autofirmado generado al vuelo, ya que en
# este test no importa que el navegador confíe en el cert — solo que TLS
# termine y nginx responda), espera health, verifica:
#   - /actuator/health responde UP (vía `docker exec`, nunca por nginx)
#   - :80 redirige 301 a https
#   - :443 responde 200 con el HTML esperado
#   - headers de seguridad presentes
#   - un asset estático se sirve con gzip
# Tira el stack al final. Correr después de cualquier cambio a
# docker-compose.prod.yml, frontend/nginx.prod.conf o backend/Dockerfile.
#
# IMPORTANTE: usa un nombre de proyecto Compose propio (-p) para no chocar
# con un stack de dev/prod ya corriendo en el mismo host — sin esto, Compose
# deriva el nombre de proyecto del nombre de carpeta y, si coincide con el de
# otro compose ya levantado, "up" RECREA esos contenedores en vez de crear
# unos nuevos (pasó una vez en desarrollo: recreó el mysql de dev con
# secretos descartables — sin pérdida de datos porque el volumen persistió,
# pero el susto es real). No correr esto en el VPS real mientras el stack de
# producción ya está levantado en los mismos puertos 80/443.

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$REPO_ROOT/docker-compose.prod.yml"
CERTS_DIR="$REPO_ROOT/certs"
ENV_FILE="$REPO_ROOT/.env.smoke_test"
PROJECT_NAME="montanari-smoke-test"

cleanup() {
  echo "[smoke_test] Bajando el stack de prueba"
  docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down -v >/dev/null 2>&1 || true
  rm -f "$ENV_FILE"
  rm -rf "$CERTS_DIR"
}
trap cleanup EXIT

echo "[smoke_test] Generando certificado autofirmado descartable en $CERTS_DIR"
mkdir -p "$CERTS_DIR"
openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
  -keyout "$CERTS_DIR/privkey.pem" -out "$CERTS_DIR/fullchain.pem" \
  -subj "/CN=localhost" >/dev/null 2>&1

echo "[smoke_test] Escribiendo secretos descartables"
cat > "$ENV_FILE" <<EOF
MYSQL_ROOT_PASSWORD=smoke_test_root_pw
DB_NAME=montanari_contable
DB_USERNAME=montanari
DB_PASSWORD=smoke_test_db_pw
JWT_SECRET=smoke_test_jwt_secret_no_usar_en_serio_0123456789
EOF

echo "[smoke_test] Levantando el stack"
docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --build

echo "[smoke_test] Esperando a que el backend responda /actuator/health (hasta ~3 minutos)"
ok=0
for i in $(seq 1 60); do
  if docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" --env-file "$ENV_FILE" exec -T backend curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; then
    ok=1
    break
  fi
  sleep 3
done
if [ "$ok" -ne 1 ]; then
  echo "[smoke_test] FAIL: backend no respondió healthy a tiempo" >&2
  docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs backend >&2
  exit 1
fi

fail=0

echo "[smoke_test] Chequeo: /actuator/health vía docker exec (nunca por nginx)"
health_body="$(docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" --env-file "$ENV_FILE" exec -T backend curl -sf http://localhost:8080/actuator/health)"
if echo "$health_body" | grep -q '"status":"UP"'; then
  echo "[smoke_test]   OK: $health_body"
else
  echo "[smoke_test]   FAIL: respuesta inesperada: $health_body" >&2
  fail=1
fi

echo "[smoke_test] Chequeo: :80 redirige 301 a https"
redirect_code="$(curl -sk -o /dev/null -w '%{http_code}' http://localhost:80/)"
if [ "$redirect_code" = "301" ]; then
  echo "[smoke_test]   OK: $redirect_code"
else
  echo "[smoke_test]   FAIL: esperado 301, obtenido $redirect_code" >&2
  fail=1
fi

echo "[smoke_test] Chequeo: :443 responde 200 con el HTML esperado"
https_body="$(curl -sk https://localhost:443/)"
https_code="$(curl -sk -o /dev/null -w '%{http_code}' https://localhost:443/)"
if [ "$https_code" = "200" ] && echo "$https_body" | grep -qi '<div id="root">'; then
  echo "[smoke_test]   OK: 200 con marca esperada en el HTML"
else
  echo "[smoke_test]   FAIL: code=$https_code, marca esperada ausente" >&2
  fail=1
fi

echo "[smoke_test] Chequeo: headers de seguridad presentes"
headers="$(curl -sk -D - -o /dev/null https://localhost:443/)"
for h in "Strict-Transport-Security" "X-Frame-Options" "X-Content-Type-Options" "Referrer-Policy" "Content-Security-Policy-Report-Only"; do
  if echo "$headers" | grep -qi "^${h}:"; then
    echo "[smoke_test]   OK: $h presente"
  else
    echo "[smoke_test]   FAIL: falta el header $h" >&2
    fail=1
  fi
done

echo "[smoke_test] Chequeo: gzip en un asset estático"
asset_path="$(echo "$https_body" | grep -oE '/assets/[^"]+\.js' | head -1 || true)"
if [ -n "$asset_path" ]; then
  encoding="$(curl -sk -H 'Accept-Encoding: gzip' -D - -o /dev/null "https://localhost:443${asset_path}" | grep -i '^Content-Encoding:' || true)"
  if echo "$encoding" | grep -qi 'gzip'; then
    echo "[smoke_test]   OK: $encoding"
  else
    echo "[smoke_test]   FAIL: sin Content-Encoding: gzip en $asset_path" >&2
    fail=1
  fi
else
  echo "[smoke_test]   ADVERTENCIA: no se encontró ningún asset /assets/*.js en el HTML para probar gzip"
fi

if [ "$fail" -eq 0 ]; then
  echo "[smoke_test] PASS"
  exit 0
else
  echo "[smoke_test] FAIL"
  exit 1
fi

# Runbook de producción — Montanari Tech Accountability

Doc viviente: se actualiza cada vez que cambia algo en el despliegue (rotación de secretos, cambio de certificado, cambio de política de backup). No confundir con `outputs/F11_3_despliegue_productivo.md`, que es el registro puntual de cuándo y cómo se construyó todo esto.

## 1. Topología

```
Internet
   │  :80 (redirect) / :443 (TLS)
   ▼
[frontend]  nginx:1.27-alpine — sirve el build de React + proxya /api/ al backend + TLS
   │  backend:8080 (red interna del compose, sin puerto publicado al host)
   ▼
[backend]   Spring Boot (perfil prod) — Actuator solo alcanzable dentro de la red
   │  mysql:3306 (red interna del compose, sin puerto publicado al host)
   ▼
[mysql]     MySQL 8 — volumen mysql_data
```

Archivos clave: `docker-compose.prod.yml`, `frontend/nginx.prod.conf`, `backend/src/main/resources/application-prod.yml`, `backend/src/main/resources/logback-spring.xml`, `ops/backup/*.sh`.

## 2. Prerrequisitos en el VPS

- Docker + Docker Compose v2 (`docker compose version`).
- Un dominio o IP apuntando al VPS (no es estrictamente necesario para TLS ya que el certificado es manual, pero sí para que los usuarios lleguen).
- El certificado TLS ya emitido por el equipo (ver §7 — **no se automatiza Let's Encrypt/certbot en este proyecto**).
- Puertos 80 y 443 abiertos en el firewall del VPS. El puerto de MySQL (3306) y el del backend (8080) **no** deben abrirse — `docker-compose.prod.yml` ya no los publica.

## 3. Deploy desde cero

1. Clonar el repo en el VPS.
2. Copiar `.env.example` a `.env` y completar TODOS los valores reales (`DB_*`, `MYSQL_ROOT_PASSWORD`, `JWT_SECRET` — generar uno nuevo con `python3 -c "import secrets; print(secrets.token_urlsafe(48))"`, nunca reusar el placeholder del `.env.example`). Agregar además `BACKUP_DIR`, `DAILY_RETENTION_DAYS`, `MONTHLY_RETENTION_MONTHS`, `LOG_RETENTION_DAYS` si se quiere cambiar algún default.
3. Crear `./certs/fullchain.pem` y `./certs/privkey.pem` (los dos nombres exactos que espera `frontend/nginx.prod.conf`) con el certificado provisto por el equipo. `chmod 600 ./certs/privkey.pem`.
4. `docker compose -f docker-compose.prod.yml up -d --build`
5. Verificar que los 3 servicios estén `healthy`: `docker compose -f docker-compose.prod.yml ps`
6. Correr el smoke test: `ops/smoke_test.sh` (ver §9).
7. **Post-deploy inmediato, antes de dar por terminado el deploy:**
   - Rotar la credencial admin sembrada (`admin@montanaritech.com` / `changeme123` — ver warning en el README) desde la propia app.
   - Verificar en la consola del navegador que la Content-Security-Policy en modo `Report-Only` (ver `frontend/nginx.prod.conf`) no reporta violaciones al navegar el flujo principal de la app; si está limpia, cambiar el header a `Content-Security-Policy` (enforcing) en `frontend/nginx.prod.conf` y recargar nginx (`docker compose -f docker-compose.prod.yml exec frontend nginx -s reload`).
8. Configurar el cron de backup (§5) y correr `ops/backup/backup.sh` manualmente una vez para confirmar que el dump + la verificación de restore pasan con datos reales.

## 4. Secretos y cómo se proveen

Todos vía variables de entorno en `.env` (nunca hardcodeados, nunca commiteados — `.env` está en `.gitignore`). Ninguna tiene default de relleno en el perfil `prod` (`application-prod.yml` exige `DB_HOST`/`DB_NAME`/`DB_USERNAME`/`DB_PASSWORD`; `application.yml` exige `JWT_SECRET` en todo perfil).

## 5. Backup — rutina y verificación

- `ops/backup/backup.sh` (cron sugerido: `0 3 * * * cd /ruta/al/repo && ops/backup/backup.sh >> /var/log/montanari-backup.log 2>&1`) — corre en el **host** (no dentro de un contenedor), usa `docker compose exec` para llegar a mysql.
- Cada corrida: dump (`mysqldump` con `--single-transaction`) → gzip → manifest de counts (`.manifest.json`) → rotación (14 diarios + 12 "primero del mes", configurable vía `DAILY_RETENTION_DAYS`/`MONTHLY_RETENTION_MONTHS`) → hook off-server (§6) → **verificación de restore automática** (`verify_restore.sh`, ver §6 de esta rutina y §8 más abajo).
- Verificación rutinaria: cada mañana revisar `/var/log/montanari-backup.log` (o donde se redirija el cron) — la última línea debe ser `[backup] Listo: ...`. Si en cambio aparece `[backup] ERROR: la verificación de restore falló`, tratar como incidente — el dump de esa noche no está confirmado como restaurable.
- **Permisos**: `BACKUP_DIR` (default `/var/backups/montanari-contable`) queda en `chmod 700`, cada dump/manifest en `chmod 600`. Fuera de cualquier volumen Docker y fuera del repo git — contiene datos financieros reales.

## 6. Backup off-server — PENDIENTE de conectar

`ops/backup/offsite_hook.sh` es un hook pluggable: hoy solo loguea "sin destino configurado" (o copia a `SECONDARY_BACKUP_DIR` si se define una ruta local secundaria). **Antes de ir a producción real, definir y conectar un destino de verdad** (ejemplos ya comentados en el propio script): un bucket S3-compatible vía `rclone`/`aws s3 cp`, o `rsync`/`scp` a un segundo servidor. Sin esto, un desastre físico en el VPS (no solo un problema de MySQL) deja sin backup recuperable.

## 7. Restore de desastre

```bash
# 1. Frenar el backend para que no escriba mientras se restaura.
docker compose -f docker-compose.prod.yml stop backend

# 2. Restaurar el dump elegido (mysqldump ya incluye DROP TABLE IF EXISTS
#    antes de cada CREATE TABLE, así que es seguro repetir sobre un esquema
#    ya poblado).
ops/backup/restore.sh /var/backups/montanari-contable/montanari_contable_AAAAMMDD_HHMMSS.sql.gz --compose-service mysql

# 3. Reiniciar el backend.
docker compose -f docker-compose.prod.yml start backend

# 4. Firma humana final: loguearse y confirmar que el balance cierra.
#    GET /api/v1/reportes/balance-sumas-y-saldos debe devolver
#    balancea:true, diferencia 0.00 — mismo chequeo que se usó en F10.3/F11.1/F11.2.
```

La verificación automática de `ops/backup/verify_restore.sh` (corrida cada noche por `backup.sh`) es justamente lo que da confianza de que este procedimiento funciona, sin esperar a un desastre real para descubrir que un dump está corrupto.

## 8. Rotación de secretos

- **JWT_SECRET**: generar uno nuevo (`python3 -c "import secrets; print(secrets.token_urlsafe(48))"`), actualizar `.env`, `docker compose -f docker-compose.prod.yml up -d backend` (recrea el contenedor). **Invalida todas las sesiones y refresh tokens activos** — avisar a los usuarios antes de rotar en horario hábil.
- **DB_PASSWORD**: cambiar el password del usuario en MySQL (`ALTER USER 'montanari'@'%' IDENTIFIED BY '...'`), actualizar `.env`, recrear el backend.
- **Credencial admin sembrada** (`admin@montanaritech.com` / `changeme123`, Flyway seed — ver warning en el README): rotar desde la propia app inmediatamente después del primer deploy (§3, paso 7), no esperar.

## 9. Reemplazo de certificado TLS

El certificado es provisto manualmente — no hay automatización de expiración/renovación en este proyecto. **Poner un recordatorio manual de calendario antes de la fecha de expiración real del certificado.**

1. Reemplazar `./certs/fullchain.pem` y `./certs/privkey.pem` con los archivos nuevos (mismos nombres exactos).
2. `chmod 600 ./certs/privkey.pem`
3. Recargar nginx sin downtime: `docker compose -f docker-compose.prod.yml exec frontend nginx -s reload`

## 10. Smoke test

`ops/smoke_test.sh` levanta `docker-compose.prod.yml` con secretos descartables y verifica: `/actuator/health` responde `UP` (vía `docker exec`, nunca por nginx), `:80` redirige 301 a `:443`, `:443` responde 200 con el HTML esperado, los headers de seguridad están presentes, los assets estáticos se sirven con gzip. Correrlo después de cualquier cambio a `docker-compose.prod.yml`, `frontend/nginx.prod.conf` o el `Dockerfile` del backend.

## 11. Troubleshooting / limitaciones conocidas

- El backup off-server no tiene destino real conectado todavía (§6) — riesgo aceptado y documentado, no oculto.
- No hay renovación automática de certificado (decisión del equipo: certificado manual, no Let's Encrypt).
- Los límites de recursos (`deploy.resources.limits`) en `docker-compose.prod.yml` son un punto de partida razonable para un VPS chico — ajustar según el tamaño real del servidor contratado.
- `verify_restore.sh` corre contra un MySQL descartable sin puerto publicado al host (nunca `--network none`: se probó y se descartó porque rompe la inicialización de root de MySQL en el contenedor — ver comentario en el script) — si el VPS tiene restricciones de recursos muy ajustadas, la verificación nocturna compite por RAM/CPU con los 3 servicios reales durante los ~1-2 minutos que tarda; considerar correrla en una ventana de baja actividad si esto se vuelve un problema.

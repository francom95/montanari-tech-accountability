# F11.3 — Despliegue productivo

Modelo asignado: Sonnet 5. Usado: Sonnet 5 (sin discrepancia).

> Insumo: sistema post-fixes [F11.2](F11_2_fixes_performance.md). Decisiones tomadas con el usuario antes de empezar: hosting = VPS Linux genérico con Docker; TLS = certificado provisto manualmente por el equipo (sin certbot/Let's Encrypt); backup off-server = hook pluggable sin destino real conectado todavía.

## Qué se construyó

### Docker Compose y Dockerfiles de producción

- [docker-compose.prod.yml](../docker-compose.prod.yml) (archivo standalone, no un override — Compose mergea `ports:` por concatenación, así que un override no puede "sacar" un puerto publicado en dev): mysql y backend sin ningún puerto publicado al host (solo alcanzables por la red interna del compose), frontend publica 80/443, `restart: always`, límites de logging (`max-size`/`max-file`) en los 3 servicios.
- [backend/Dockerfile](../backend/Dockerfile): usuario no-root (directorios creados con el owner correcto antes de `USER app`, para que los volúmenes montados hereden permisos), entrypoint en forma shell con `-XX:MaxRAMPercentage=75.0` (JDK 21 detecta el cgroup pero no fija el porcentaje solo) + `-XX:+ExitOnOutOfMemoryError`.

### Nginx + TLS

- [frontend/nginx.prod.conf](../frontend/nginx.prod.conf) (nuevo, NO reemplaza el de dev): redirect 80→443, `:443` con cert/key montados desde `./certs/fullchain.pem` y `./certs/privkey.pem` (nombres exactos que el equipo debe colocar — certificado manual, sin automatización de renovación), proxy `/api/` al backend (nunca `/actuator` — junto con que el backend no publica puerto, esto lo saca de internet), headers de seguridad (HSTS, X-Frame-Options, X-Content-Type-Options, Referrer-Policy, CSP en modo `Report-Only` primero — pasar a enforcing recién después de confirmar en la consola del navegador que nada rompe), gzip, cache de assets hasheados de Vite.

### Perfil prod endurecido

- [backend/src/main/resources/logback-spring.xml](../backend/src/main/resources/logback-spring.xml) (nuevo): rotación a archivo solo en el perfil `prod` (consola sigue activa), retención configurable vía `LOG_RETENTION_DAYS` (default 30 — son logs operativos, no el rastro de auditoría de negocio que ya vive en `AuditoriaLog`).
- [application-prod.yml](../backend/src/main/resources/application-prod.yml): `management.endpoint.health.show-details: never` explícito — defensa en profundidad además del aislamiento de red real (backend sin puerto publicado + nginx nunca proxya `/actuator`).

### Backup, restore y verificación automática (`ops/backup/`, todo nuevo)

- `backup.sh`: dump vía `docker compose exec` (sin sidecar, sin montar `docker.sock`), manifest de counts (`.manifest.json`), rotación GFS-lite (`prune_backups.sh`: 14 diarios + 12 "primero del mes", configurable), hook off-server pluggable (`offsite_hook.sh` — hoy sin destino real, ver limitación abajo), y **verificación de restore encadenada al final de cada corrida** (`verify_restore.sh`), no como un chequeo aparte de una sola vez.
- `restore.sh`: restaura contra el mysql real de prod (disaster recovery) o contra un contenedor descartable (usado por `verify_restore.sh`).
- **Bug real encontrado y corregido durante la verificación en vivo**: el primer diseño de `verify_restore.sh` levantaba el MySQL descartable con `--network none` (para no publicar ningún puerto). Se descubrió que esto rompe la inicialización de MySQL: el `mysqladmin ping --silent` daba un falso positivo de "listo" (ese flag solo chequea conectividad, no autenticación), pero la autenticación real como root fallaba con `--network none` incluso con la contraseña correcta. Se reprodujo aislando la variable (con red normal sin publicar puerto, la autenticación funciona; con `--network none`, no) y se corrigió: sin `--network none` (igual de aislado — ningún `-p` publica puerto al host), más un chequeo de arranque que exige ver **dos** apariciones de "ready for connections" en el log del contenedor (el server temporal de init + el definitivo), no solo una consulta de auth exitosa, porque hay una ventana real de milisegundos donde el server temporal ya autentica pero se está por reiniciar.

### Smoke test (`ops/smoke_test.sh`, nuevo)

Levanta `docker-compose.prod.yml` con secretos descartables y un certificado autofirmado, verifica `/actuator/health` (vía `docker exec`, nunca por nginx), redirect 80→443, 200 en `:443` con el HTML esperado, headers de seguridad, gzip. Usa un nombre de proyecto Compose propio (`-p montanari-smoke-test`) — **esto se corrigió después de un susto real durante la verificación**: sin un nombre de proyecto explícito, Compose deriva el nombre del proyecto del nombre de carpeta, que coincide entre `docker-compose.yml` (dev) y `docker-compose.prod.yml`; la primera corrida del smoke test recreó los contenedores de dev reales (mysql/backend) con secretos descartables. No hubo pérdida de datos porque el volumen con nombre coincidente (`mysql_data`) persistió y MySQL ignora `MYSQL_ROOT_PASSWORD` en un volumen ya inicializado, pero se corrigió de raíz dándole al smoke test su propio namespace de proyecto (y por lo tanto sus propios volúmenes descartables).

### Runbook

[docs/RUNBOOK.md](../docs/RUNBOOK.md): topología, prerrequisitos, deploy desde cero, rutina de backup, restore de desastre, rotación de secretos (JWT_SECRET, DB_PASSWORD, credencial admin sembrada), reemplazo de certificado, troubleshooting y limitaciones conocidas.

## Verificación real (no solo scripts escritos — corridos de punta a punta)

- **Backup + restore + verificación, contra los datos reales del sistema** (no fixtures): 847 asientos, 1.924 líneas, 19 clientes, 13 proveedores, 60 facturas de venta, 108 facturas de compra, 492 movimientos bancarios — todos los counts coincidieron exactos entre el dump y el restore, y el invariante de balance cerró exacto: `Σdebe = Σhaber = $305.621.023,15`, diferencia $0,00.
- **Smoke test de `docker-compose.prod.yml`**: PASS — `/actuator/health` → `UP`, `:80` → 301, `:443` → 200 con el HTML esperado, los 5 headers de seguridad presentes, gzip confirmado en el asset estático.
- **Suite completa de backend**: 688/688 tests (el único error es el ambiental de Testcontainers ya documentado en sesiones previas, sin relación con estos cambios) — sin regresiones.
- Ambas verificaciones en vivo expusieron y corrigieron 2 bugs reales del propio trabajo de este paso (detallados arriba): la carrera de inicialización de MySQL con `--network none`, y la colisión de nombre de proyecto Compose entre dev y prod.

## Pendiente antes de ir a producción real

- **Backup off-server sin destino conectado** (decisión explícita del usuario para este paso): `ops/backup/offsite_hook.sh` hoy solo loguea o copia a una ruta local secundaria opcional. Hay que conectarlo a un destino real (S3-compatible, rsync a un segundo servidor, etc.) antes de depender de esto en serio — documentado en el runbook, no oculto.
- El certificado TLS es manual — no hay renovación automática; el runbook pide un recordatorio de calendario para la fecha de expiración real.
- Los límites de recursos (`deploy.resources.limits`) en `docker-compose.prod.yml` son un punto de partida razonable para un VPS chico — ajustar según el tamaño real del servidor que se contrate.

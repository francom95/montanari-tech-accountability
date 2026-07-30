#!/usr/bin/env bash
set -euo pipefail

# Hook pluggable de copia "fuera del servidor" (F11.3). Hoy NO hay destino
# remoto real conectado — decisión explícita tomada con el usuario para no
# referenciar credenciales/infra que todavía no existen. Ver docs/RUNBOOK.md
# § backup off-server: esto hay que conectarlo a un destino real antes de ir
# a producción de verdad.
#
# Contrato: recibe en $1 la ruta absoluta al dump .sql.gz recién creado.
# Debe salir con código 0 si hizo "algo razonable" con la copia (incluso si
# ese "algo" es solo loguear que no hay destino todavía). Un exit no-cero
# dispara una advertencia en backup.sh, pero NUNCA borra el backup local.
#
# Para conectar un destino real, reemplazar el cuerpo de este script por,
# por ejemplo:
#   rclone copy "$dump_file" remote:montanari-contable-backups/
#   aws s3 cp "$dump_file" s3://mi-bucket/montanari-contable/
#   rsync -avz -e ssh "$dump_file" usuario@otro-host:/backups/montanari-contable/

dump_file="${1:?Uso: offsite_hook.sh <ruta al dump .sql.gz>}"

if [ -n "${SECONDARY_BACKUP_DIR:-}" ]; then
  mkdir -p "$SECONDARY_BACKUP_DIR"
  cp "$dump_file" "$SECONDARY_BACKUP_DIR/"
  echo "[offsite_hook] Copiado a destino local secundario: $SECONDARY_BACKUP_DIR/$(basename "$dump_file")"
else
  echo "[offsite_hook] Sin destino remoto configurado todavía — ver docs/RUNBOOK.md § backup off-server."
fi

exit 0

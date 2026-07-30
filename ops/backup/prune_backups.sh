#!/usr/bin/env bash
set -euo pipefail

# Retención GFS-lite para los dumps de backup.sh:
#   - Últimos <daily_retention_days> días: se conservan TODOS los dumps.
#   - Entre eso y <monthly_retention_months> meses atrás: se conserva solo
#     el dump más antiguo de cada mes calendario ("primero del mes").
#   - Más viejo que eso: se borra.
# Cada dump tiene un .manifest.json hermano (mismo nombre base) que se borra
# junto con su .sql.gz.
#
# Requiere GNU date (`date -d`) — asumido disponible en el VPS Linux target.

usage() {
  echo "Uso: $0 <backup_dir> <daily_retention_days> <monthly_retention_months>" >&2
  exit 1
}

[ $# -eq 3 ] || usage
backup_dir="$1"
daily_days="$2"
monthly_months="$3"

daily_cutoff_epoch=$(date -d "-${daily_days} days" +%s)
monthly_cutoff_epoch=$(date -d "-${monthly_months} months" +%s)

declare -A month_earliest_file
declare -A month_earliest_epoch

shopt -s nullglob
dumps=("$backup_dir"/montanari_contable_*.sql.gz)

# Primera pasada: borrar lo que ya está fuera de toda retención, y
# encontrar el dump más antiguo de cada mes dentro de la ventana mensual.
for f in "${dumps[@]}"; do
  base="$(basename "$f")"
  datepart="${base#montanari_contable_}"
  datepart="${datepart%.sql.gz}"
  ymd="${datepart%%_*}"
  [ "${#ymd}" -eq 8 ] || continue
  file_epoch=$(date -d "${ymd}" +%s 2>/dev/null) || continue
  yyyymm="${ymd:0:6}"

  if [ "$file_epoch" -ge "$daily_cutoff_epoch" ]; then
    continue # dentro de la ventana diaria: se conserva tal cual
  fi

  if [ "$file_epoch" -lt "$monthly_cutoff_epoch" ]; then
    echo "[prune] Borrando (fuera de retención): $base"
    rm -f "$f" "$backup_dir/${base%.sql.gz}.manifest.json"
    continue
  fi

  if [ -z "${month_earliest_epoch[$yyyymm]:-}" ] || [ "$file_epoch" -lt "${month_earliest_epoch[$yyyymm]}" ]; then
    month_earliest_epoch[$yyyymm]="$file_epoch"
    month_earliest_file[$yyyymm]="$f"
  fi
done

# Segunda pasada: dentro de la ventana mensual, borrar todo lo que no sea el
# "primero del mes" que se identificó arriba.
for f in "${dumps[@]}"; do
  [ -f "$f" ] || continue # puede haberse borrado en la primera pasada
  base="$(basename "$f")"
  datepart="${base#montanari_contable_}"
  datepart="${datepart%.sql.gz}"
  ymd="${datepart%%_*}"
  [ "${#ymd}" -eq 8 ] || continue
  file_epoch=$(date -d "${ymd}" +%s 2>/dev/null) || continue
  yyyymm="${ymd:0:6}"

  [ "$file_epoch" -ge "$daily_cutoff_epoch" ] && continue
  [ "$file_epoch" -lt "$monthly_cutoff_epoch" ] && continue

  if [ "$f" != "${month_earliest_file[$yyyymm]:-}" ]; then
    echo "[prune] Borrando (no es el primero del mes $yyyymm): $base"
    rm -f "$f" "$backup_dir/${base%.sql.gz}.manifest.json"
  fi
done

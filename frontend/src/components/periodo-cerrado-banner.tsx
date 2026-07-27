import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"

/** F9.3: banner de reintento con motivo cuando una mutación gateada devuelve PERIODO_CERRADO (ver use-periodo-cerrado-override). */
export function PeriodoCerradoBanner({
  mensaje, motivo, onMotivoChange, onConfirmar, onCancelar, confirmando,
}: {
  mensaje: string
  motivo: string
  onMotivoChange: (v: string) => void
  onConfirmar: () => void
  onCancelar: () => void
  confirmando: boolean
}) {
  return (
    <div className="flex flex-wrap items-center gap-2 rounded-md border border-destructive/40 bg-destructive/5 px-3 py-2 text-sm">
      <span className="text-destructive">{mensaje}</span>
      <Input
        autoFocus
        placeholder="Motivo del override…"
        value={motivo}
        onChange={(e) => onMotivoChange(e.target.value)}
        className="h-8 w-56"
      />
      <Button size="sm" disabled={confirmando || !motivo.trim()} onClick={onConfirmar}>
        Confirmar igual
      </Button>
      <Button variant="outline" size="sm" onClick={onCancelar}>Cancelar</Button>
    </div>
  )
}

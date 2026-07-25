import { Button } from "@/components/ui/button"
import type { Alerta, SeveridadAlerta } from "@/types/alerta"

const ETIQUETA_SEVERIDAD: Record<SeveridadAlerta, string> = {
  CRITICA: "text-destructive",
  ADVERTENCIA: "text-amber-600 dark:text-amber-500",
  INFO: "text-muted-foreground",
}

export function AlertasLista({
  alertas,
  onMarcarLeida,
  marcandoId,
  vacio,
}: {
  alertas: Alerta[]
  onMarcarLeida: (id: number) => void
  marcandoId?: number
  vacio: string
}) {
  if (alertas.length === 0) {
    return <p className="text-sm text-muted-foreground">{vacio}</p>
  }

  return (
    <ul className="space-y-2">
      {alertas.map((a) => (
        <li key={a.id} className="flex items-start justify-between gap-3 border-b border-border pb-2 last:border-0 last:pb-0">
          <div className="min-w-0">
            <p className={`text-sm font-medium ${ETIQUETA_SEVERIDAD[a.severidad]}`}>{a.mensaje}</p>
            <p className="text-xs text-muted-foreground">{a.fecha}</p>
          </div>
          {!a.leida && (
            <Button variant="outline" size="sm" disabled={marcandoId === a.id} onClick={() => onMarcarLeida(a.id)}>
              Marcar leída
            </Button>
          )}
        </li>
      ))}
    </ul>
  )
}

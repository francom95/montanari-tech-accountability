import { Bell } from "lucide-react"
import { useEffect, useRef, useState } from "react"

import { AlertasLista } from "@/components/alertas-lista"
import { useAlertas, useContadorAlertas, useMarcarLeidaAlerta } from "@/hooks/use-alerta"

/**
 * Campana + panel de alertas del header (F9.1). No hay ningún primitive de
 * Popover/Dropdown en el proyecto (verificado antes de implementar) — el
 * panel es un div posicionado a mano, cerrado con un listener de
 * "mousedown" fuera del contenedor.
 */
export function AlertasBadge() {
  const [abierto, setAbierto] = useState(false)
  const contenedorRef = useRef<HTMLDivElement>(null)

  const contador = useContadorAlertas()
  const alertas = useAlertas({ estado: "ACTIVA", size: 20, enabled: abierto })
  const marcarLeida = useMarcarLeidaAlerta()

  useEffect(() => {
    if (!abierto) return
    function alClickFuera(e: MouseEvent) {
      if (contenedorRef.current && !contenedorRef.current.contains(e.target as Node)) {
        setAbierto(false)
      }
    }
    document.addEventListener("mousedown", alClickFuera)
    return () => document.removeEventListener("mousedown", alClickFuera)
  }, [abierto])

  const noLeidas = contador.data?.noLeidas ?? 0

  return (
    <div ref={contenedorRef} className="relative">
      <button
        type="button"
        aria-label="Alertas"
        onClick={() => setAbierto((v) => !v)}
        className="relative inline-flex size-8 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
      >
        <Bell className="size-4" />
        {noLeidas > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex size-4 items-center justify-center rounded-full bg-destructive text-[10px] font-medium text-destructive-foreground">
            {noLeidas > 9 ? "9+" : noLeidas}
          </span>
        )}
      </button>

      {abierto && (
        <div className="absolute right-0 top-full z-50 mt-2 w-80 rounded-lg border border-border bg-popover p-3 shadow-lg">
          <p className="mb-2 text-sm font-semibold text-foreground">Alertas activas</p>
          {alertas.isLoading ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : (
            <AlertasLista
              alertas={alertas.data?.content ?? []}
              onMarcarLeida={(id) => marcarLeida.mutate(id)}
              marcandoId={marcarLeida.isPending ? marcarLeida.variables : undefined}
              vacio="Sin alertas activas."
            />
          )}
        </div>
      )}
    </div>
  )
}

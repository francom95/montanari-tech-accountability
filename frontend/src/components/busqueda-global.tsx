import { Search } from "lucide-react"
import { useEffect, useRef, useState } from "react"
import { useNavigate } from "react-router-dom"

import { useBusquedaGlobal } from "@/hooks/use-busqueda"
import { rutaParaResultadoBusqueda } from "@/lib/ruta-busqueda"
import { ETIQUETA_TIPO_BUSQUEDA, type TipoEntidadBusqueda } from "@/types/busqueda"

/**
 * Búsqueda global "Lupita" (F9.2): overlay centrado, mismo patrón manual que
 * `alertas-badge.tsx` de F9.1 (no hay Dialog/Modal en el proyecto). Primer
 * listener de teclado global (Ctrl/Cmd+K) — se complementa con un ícono
 * visible en el header para discoverabilidad.
 */
export function BusquedaGlobal() {
  const [abierto, setAbierto] = useState(false)
  const [termino, setTermino] = useState("")
  const [terminoDebounced, setTerminoDebounced] = useState("")
  const inputRef = useRef<HTMLInputElement>(null)
  const navigate = useNavigate()

  const busqueda = useBusquedaGlobal(terminoDebounced)

  useEffect(() => {
    const id = setTimeout(() => setTerminoDebounced(termino), 300)
    return () => clearTimeout(id)
  }, [termino])

  useEffect(() => {
    function alPresionarTecla(e: KeyboardEvent) {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
        e.preventDefault()
        setAbierto((v) => !v)
      } else if (e.key === "Escape") {
        setAbierto(false)
      }
    }
    document.addEventListener("keydown", alPresionarTecla)
    return () => document.removeEventListener("keydown", alPresionarTecla)
  }, [])

  useEffect(() => {
    if (abierto) {
      inputRef.current?.focus()
    } else {
      setTermino("")
      setTerminoDebounced("")
    }
  }, [abierto])

  function irAResultado(ruta: string) {
    setAbierto(false)
    navigate(ruta)
  }

  if (!abierto) {
    return (
      <button
        type="button"
        aria-label="Buscar (Ctrl+K)"
        onClick={() => setAbierto(true)}
        className="inline-flex size-8 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
      >
        <Search className="size-4" />
      </button>
    )
  }

  const grupos = busqueda.data?.grupos ?? {}
  const tipos = Object.keys(grupos) as TipoEntidadBusqueda[]

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center bg-black/40 pt-24" onClick={() => setAbierto(false)}>
      <div
        className="w-full max-w-lg rounded-lg border border-border bg-popover p-3 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <input
          ref={inputRef}
          type="text"
          value={termino}
          onChange={(e) => setTermino(e.target.value)}
          placeholder="Buscar por descripción, número, importe, fecha o CUIT…"
          className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        />

        <div className="mt-3 max-h-96 overflow-y-auto">
          {termino.trim().length < 2 ? (
            <p className="p-2 text-sm text-muted-foreground">Escribí al menos 2 caracteres…</p>
          ) : busqueda.isLoading ? (
            <p className="p-2 text-sm text-muted-foreground">Buscando…</p>
          ) : tipos.length === 0 ? (
            <p className="p-2 text-sm text-muted-foreground">Sin resultados.</p>
          ) : (
            <div className="space-y-3">
              {tipos.map((tipo) => {
                const grupo = grupos[tipo]!
                return (
                  <div key={tipo}>
                    <p className="px-2 text-xs font-semibold uppercase text-muted-foreground">
                      {ETIQUETA_TIPO_BUSQUEDA[tipo]} {grupo.total > grupo.items.length && `(${grupo.total})`}
                    </p>
                    <ul>
                      {grupo.items.map((item) => (
                        <li key={`${item.tipo}-${item.id}`}>
                          <button
                            type="button"
                            onClick={() => irAResultado(rutaParaResultadoBusqueda(item))}
                            className="flex w-full flex-col items-start gap-0.5 rounded-md px-2 py-1.5 text-left text-sm hover:bg-muted"
                          >
                            <span className="text-foreground">{item.resumen}</span>
                            {item.fecha && <span className="text-xs text-muted-foreground">{item.fecha}</span>}
                          </button>
                        </li>
                      ))}
                    </ul>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

import { useState } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { RequireAdmin } from "@/components/require-admin"
import {
  useCerrarPeriodo,
  useGenerarAutomaticosPeriodos,
  useMarcarEnRevisionPeriodo,
  usePeriodoResumen,
  usePeriodos,
  useReabrirPeriodo,
} from "@/hooks/use-periodo"
import type { EstadoPeriodo } from "@/types/periodo"

const ESTADO_LABEL: Record<EstadoPeriodo, string> = {
  ABIERTO: "Abierto", EN_REVISION: "En revisión", CERRADO: "Cerrado",
}

const MESES = [
  "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic",
]

const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"
const inputClase = "h-8"

export function PeriodosPage() {
  return (
    <RequireAdmin>
      <GestionPeriodos />
    </RequireAdmin>
  )
}

function GestionPeriodos() {
  const [estadoFiltro, setEstadoFiltro] = useState<EstadoPeriodo | "">("")
  const [seleccionado, setSeleccionado] = useState<number | null>(null)
  const [accion, setAccion] = useState<{ id: number; tipo: "cerrar" | "reabrir" } | null>(null)
  const [motivoInput, setMotivoInput] = useState("")

  const query = usePeriodos({ estado: estadoFiltro || undefined, size: 100 })
  const resumen = usePeriodoResumen(seleccionado ?? undefined)

  const generar = useGenerarAutomaticosPeriodos()
  const cerrar = useCerrarPeriodo()
  const marcarEnRevision = useMarcarEnRevisionPeriodo()
  const reabrir = useReabrirPeriodo()

  function confirmarAccion() {
    if (!accion || !motivoInput.trim()) return
    const mutacion = accion.tipo === "cerrar" ? cerrar : reabrir
    mutacion.mutate({ id: accion.id, motivo: motivoInput }, {
      onSuccess: () => { setAccion(null); setMotivoInput("") },
    })
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-semibold text-foreground">Períodos contables</h1>
          <p className="text-sm text-muted-foreground">
            Cerrar un período bloquea la carga/edición/anulación de asientos, facturas, cobros y pagos con fecha
            dentro de ese mes — salvo override de administrador con motivo (F9.3). Consultar, importar y exportar
            nunca se bloquean.
          </p>
        </div>
        <Button disabled={generar.isPending} onClick={() => generar.mutate()}>
          {generar.isPending ? "Generando…" : "Generar períodos"}
        </Button>
      </div>

      {generar.data && (
        <p className="text-sm text-muted-foreground">
          {generar.data.generados > 0
            ? `Se generaron ${generar.data.generados} período(s) nuevo(s) a partir de los asientos existentes.`
            : "No hay períodos nuevos para generar: todos los meses con asientos ya tienen una fila."}
        </p>
      )}

      <div className="grid gap-6 lg:grid-cols-[2fr_1fr]">
        <Card>
          <CardHeader><CardTitle>Listado</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            <select
              value={estadoFiltro}
              onChange={(e) => setEstadoFiltro(e.target.value as EstadoPeriodo | "")}
              className={`${selectClase} max-w-48`}
            >
              <option value="">Todos los estados</option>
              {(["ABIERTO", "EN_REVISION", "CERRADO"] as EstadoPeriodo[]).map((e) => (
                <option key={e} value={e}>{ESTADO_LABEL[e]}</option>
              ))}
            </select>

            {query.isLoading ? (
              <p className="text-sm text-muted-foreground">Cargando…</p>
            ) : (
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr className="border-b border-border">
                    <th className="py-2 pr-4 font-medium">Período</th>
                    <th className="py-2 pr-4 font-medium">Estado</th>
                    <th className="py-2 font-medium">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {query.data?.content.map((p) => (
                    <tr
                      key={p.id}
                      className={`cursor-pointer border-b border-border last:border-0 ${seleccionado === p.id ? "bg-muted/50" : ""}`}
                      onClick={() => setSeleccionado(p.id)}
                    >
                      <td className="py-2 pr-4">{MESES[p.mes - 1]}/{p.anio}</td>
                      <td className="py-2 pr-4">{ESTADO_LABEL[p.estado]}</td>
                      <td className="py-2" onClick={(e) => e.stopPropagation()}>
                        {accion?.id === p.id ? (
                          <div className="flex flex-wrap items-center gap-2">
                            <Input
                              autoFocus
                              placeholder={accion.tipo === "cerrar" ? "Motivo del cierre…" : "Motivo de la reapertura…"}
                              value={motivoInput}
                              onChange={(e) => setMotivoInput(e.target.value)}
                              className={`${inputClase} w-48`}
                            />
                            <Button
                              size="sm"
                              disabled={cerrar.isPending || reabrir.isPending || !motivoInput.trim()}
                              onClick={confirmarAccion}
                            >
                              Confirmar
                            </Button>
                            <Button variant="outline" size="sm" onClick={() => { setAccion(null); setMotivoInput("") }}>
                              Cancelar
                            </Button>
                          </div>
                        ) : (
                          <div className="flex flex-wrap gap-2">
                            {p.estado !== "CERRADO" && (
                              <Button variant="outline" size="sm" onClick={() => { setAccion({ id: p.id, tipo: "cerrar" }); setMotivoInput("") }}>
                                Cerrar
                              </Button>
                            )}
                            {p.estado === "ABIERTO" && (
                              <Button variant="outline" size="sm" disabled={marcarEnRevision.isPending} onClick={() => marcarEnRevision.mutate(p.id)}>
                                Marcar en revisión
                              </Button>
                            )}
                            {p.estado === "CERRADO" && (
                              <Button variant="outline" size="sm" onClick={() => { setAccion({ id: p.id, tipo: "reabrir" }); setMotivoInput("") }}>
                                Reabrir
                              </Button>
                            )}
                          </div>
                        )}
                      </td>
                    </tr>
                  ))}
                  {query.data?.content.length === 0 && (
                    <tr><td colSpan={3} className="py-4 text-center text-sm text-muted-foreground">Sin períodos. Usá "Generar períodos" para crearlos a partir de los asientos existentes.</td></tr>
                  )}
                </tbody>
              </table>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Resumen del período</CardTitle></CardHeader>
          <CardContent className="space-y-4 text-sm">
            {seleccionado === null ? (
              <p className="text-muted-foreground">Seleccioná un período para ver sus liquidaciones y conciliaciones.</p>
            ) : resumen.isLoading ? (
              <p className="text-muted-foreground">Cargando…</p>
            ) : (
              <>
                <div>
                  <h3 className="mb-2 font-medium text-foreground">Liquidaciones</h3>
                  {resumen.data?.liquidaciones.length ? (
                    <ul className="space-y-1">
                      {resumen.data.liquidaciones.map((l) => (
                        <li key={`${l.tipo}-${l.id}`} className="flex justify-between">
                          <span>{l.tipo} ({l.estado})</span>
                          <span>{l.saldoAPagar.toFixed(2)}</span>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="text-muted-foreground">Sin liquidaciones en este período.</p>
                  )}
                </div>
                <div>
                  <h3 className="mb-2 font-medium text-foreground">Conciliación bancaria</h3>
                  {resumen.data?.conciliaciones.length ? (
                    <ul className="space-y-1">
                      {resumen.data.conciliaciones.map((c) => (
                        <li key={c.cuentaBancariaId} className="flex justify-between">
                          <span>{c.cuentaBancariaAlias}</span>
                          <span className={c.diferencia !== 0 ? "font-semibold text-destructive" : ""}>
                            {c.diferencia.toFixed(2)} {c.monedaCodigo}
                          </span>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="text-muted-foreground">Sin cuentas bancarias activas.</p>
                  )}
                </div>
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

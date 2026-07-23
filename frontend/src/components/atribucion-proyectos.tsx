import { useState } from "react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { useAtribucion, useGuardarAtribucion, usePrevisualizarAtribucion } from "@/hooks/use-atribucion-impuesto"
import { useProyectos } from "@/hooks/use-proyecto"
import { CRITERIOS } from "@/types/atribucion-impuesto"
import type { Atribucion, CriterioAtribucion, TipoLiquidacion } from "@/types/atribucion-impuesto"

const pesos = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" })
const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"

/**
 * Sección "atribución a proyectos" de una liquidación confirmada (F6.3). Reusa
 * el mismo componente para IVA e IIBB — la liquidación se referencia por tipo + id.
 */
export function AtribucionProyectos({
  tipo,
  liquidacionId,
}: {
  tipo: TipoLiquidacion
  liquidacionId: number
}) {
  const atribucion = useAtribucion(tipo, liquidacionId)
  const proyectos = useProyectos({ activo: true, page: 0, size: 200 })
  const previsualizar = usePrevisualizarAtribucion(tipo, liquidacionId)
  const guardar = useGuardarAtribucion(tipo, liquidacionId)

  const [criterio, setCriterio] = useState<CriterioAtribucion | null>(null)
  const [proyectoDirecto, setProyectoDirecto] = useState("")
  const [porcentajes, setPorcentajes] = useState<Record<number, string>>({})
  const [vista, setVista] = useState<Atribucion | null>(null)

  const actual = vista ?? atribucion.data
  const criterioEfectivo = criterio ?? atribucion.data?.criterio ?? "FACTURACION"

  function armarInput() {
    if (criterioEfectivo === "DIRECTO") {
      return { criterio: criterioEfectivo, proyectoIdDirecto: Number(proyectoDirecto) }
    }
    if (criterioEfectivo === "PORCENTAJE_MANUAL") {
      return {
        criterio: criterioEfectivo,
        porcentajes: Object.entries(porcentajes)
          .filter(([, v]) => Number(v) > 0)
          .map(([proyectoId, v]) => ({ proyectoId: Number(proyectoId), porcentaje: Number(v) })),
      }
    }
    return { criterio: criterioEfectivo }
  }

  if (!atribucion.data) return null

  return (
    <div className="space-y-3 rounded-lg border border-border p-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="text-sm font-medium">
          Atribución a proyectos — impuesto {pesos.format(atribucion.data.montoTotal)}
          {atribucion.data.guardada ? " · guardada" : " · sin guardar"}
        </span>
      </div>

      {atribucion.data.montoTotal <= 0 ? (
        <p className="text-sm text-muted-foreground">
          Esta liquidación no arroja saldo a pagar, así que no hay impuesto que atribuir.
        </p>
      ) : (
        <>
          <div className="grid gap-2 sm:grid-cols-3">
            <label className="flex flex-col text-xs text-muted-foreground">
              Criterio de prorrateo
              <select
                value={criterioEfectivo}
                onChange={(e) => {
                  setCriterio(e.target.value as CriterioAtribucion)
                  setVista(null)
                }}
                className={selectClase}
              >
                {CRITERIOS.map((c) => (
                  <option key={c.value} value={c.value}>{c.label}</option>
                ))}
              </select>
            </label>

            {criterioEfectivo === "DIRECTO" && (
              <label className="flex flex-col text-xs text-muted-foreground">
                Proyecto (100%)
                <select value={proyectoDirecto} onChange={(e) => setProyectoDirecto(e.target.value)} className={selectClase}>
                  <option value="">Seleccionar…</option>
                  {(proyectos.data?.content ?? []).map((p) => (
                    <option key={p.id} value={p.id.toString()}>{p.nombre}</option>
                  ))}
                </select>
              </label>
            )}

            <div className="flex items-end gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={previsualizar.isPending}
                onClick={() => previsualizar.mutate(armarInput(), { onSuccess: (d) => setVista(d) })}
              >
                Previsualizar
              </Button>
              <Button
                size="sm"
                disabled={guardar.isPending}
                onClick={() => guardar.mutate(armarInput(), { onSuccess: (d) => setVista(d) })}
              >
                Guardar atribución
              </Button>
            </div>
          </div>

          {criterioEfectivo === "PORCENTAJE_MANUAL" && (
            <div className="grid gap-2 sm:grid-cols-2">
              {(proyectos.data?.content ?? []).map((p) => (
                <label key={p.id} className="flex items-center gap-2 text-xs text-muted-foreground">
                  <span className="w-40 truncate">{p.nombre}</span>
                  <Input
                    type="number"
                    value={porcentajes[p.id] ?? ""}
                    onChange={(e) => setPorcentajes((prev) => ({ ...prev, [p.id]: e.target.value }))}
                    placeholder="%"
                    className="h-7 w-20 text-right"
                  />
                </label>
              ))}
            </div>
          )}

          {(actual?.advertencias ?? []).length > 0 && (
            <ul className="space-y-1 rounded-lg border border-amber-500/40 bg-amber-500/10 p-2 text-xs">
              {actual!.advertencias.map((a) => <li key={a}>⚠ {a}</li>)}
            </ul>
          )}

          <table className="w-full text-left text-sm">
            <thead className="text-muted-foreground">
              <tr className="border-b border-border">
                <th className="py-1.5 pr-4 font-medium">Proyecto</th>
                <th className="py-1.5 pr-4 text-right font-medium">%</th>
                <th className="py-1.5 pr-4 text-right font-medium">Monto</th>
              </tr>
            </thead>
            <tbody>
              {(actual?.lineas ?? []).map((l) => (
                <tr key={l.proyectoId} className="border-b border-border last:border-0">
                  <td className="py-1.5 pr-4">{l.proyectoNombre}</td>
                  <td className="py-1.5 pr-4 text-right tabular-nums">{l.porcentaje.toFixed(2)}%</td>
                  <td className="py-1.5 pr-4 text-right tabular-nums">{pesos.format(l.monto)}</td>
                </tr>
              ))}
              {(actual?.lineas ?? []).length === 0 && (
                <tr><td colSpan={3} className="py-2 text-center text-muted-foreground">Sin distribución todavía.</td></tr>
              )}
            </tbody>
          </table>
        </>
      )}
    </div>
  )
}

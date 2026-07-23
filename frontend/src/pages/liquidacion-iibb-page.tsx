import { useState } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import {
  useAjustarComponenteIibb,
  useAnularLiquidacionIibb,
  useConfirmarLiquidacionIibb,
  useCrearLiquidacionIibb,
  useEditarJurisdiccionIibb,
  useLiquidacionesIibb,
  usePrevisualizacionIibb,
  useRecalcularLiquidacionIibb,
} from "@/hooks/use-liquidacion-iibb"
import type { ComponenteIibb, JurisdiccionIibb, LiquidacionIibb } from "@/types/liquidacion-iibb"

const MESES = [
  "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
  "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
]

const pesos = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" })

function formatearPesos(valor: number) {
  return pesos.format(valor)
}

export function LiquidacionIibbPage() {
  const hoy = new Date()
  const [anio, setAnio] = useState(hoy.getFullYear())
  const [mes, setMes] = useState(hoy.getMonth() + 1)
  const [seleccionadaId, setSeleccionadaId] = useState<number | null>(null)

  const liquidaciones = useLiquidacionesIibb({ anio })
  const previsualizacion = usePrevisualizacionIibb(anio, mes)
  const crear = useCrearLiquidacionIibb()

  const seleccionada = liquidaciones.data?.content.find((l) => l.id === seleccionadaId)
  const yaLiquidado = liquidaciones.data?.content.some((l) => l.mes === mes && l.estado !== "ANULADO")

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-lg font-semibold text-foreground">Liquidación de IIBB (Convenio Multilateral)</h1>
        <p className="text-sm text-muted-foreground">
          La base de ventas del período se reparte entre jurisdicciones por coeficiente (default = participación por
          destino, editable). Cargá las deducciones a mano por jurisdicción; al confirmar se genera el asiento.
        </p>
      </div>

      <Card>
        <CardHeader><CardTitle>Período</CardTitle></CardHeader>
        <CardContent className="grid gap-3 sm:grid-cols-4">
          <label className="flex flex-col text-xs text-muted-foreground">
            Año
            <Input type="number" value={anio} onChange={(e) => setAnio(Number(e.target.value))} className="h-8" />
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Mes
            <select
              value={mes}
              onChange={(e) => setMes(Number(e.target.value))}
              className="h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"
            >
              {MESES.map((m, i) => <option key={m} value={i + 1}>{m}</option>)}
            </select>
          </label>
          <div className="flex items-end sm:col-span-2">
            <Button
              disabled={yaLiquidado || crear.isPending}
              onClick={() => crear.mutate({ anio, mes }, { onSuccess: (l) => setSeleccionadaId(l.id) })}
            >
              {yaLiquidado ? "Ya hay una liquidación de este mes" : "Liquidar este período"}
            </Button>
          </div>
        </CardContent>
      </Card>

      {!seleccionada && previsualizacion.data && (
        <Card>
          <CardHeader>
            <CardTitle>
              Previsualización de {MESES[previsualizacion.data.mes - 1]} {previsualizacion.data.anio} — base total{" "}
              {formatearPesos(previsualizacion.data.baseTotal)}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <Advertencias items={previsualizacion.data.advertencias} />
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground">
                <tr className="border-b border-border">
                  <th className="py-2 pr-4 font-medium">Jurisdicción</th>
                  <th className="py-2 pr-4 text-right font-medium">Coef.</th>
                  <th className="py-2 pr-4 text-right font-medium">Base imponible</th>
                  <th className="py-2 pr-4 text-right font-medium">Alícuota</th>
                  <th className="py-2 pr-4 text-right font-medium">Determinado</th>
                </tr>
              </thead>
              <tbody>
                {previsualizacion.data.jurisdicciones.map((j) => (
                  <tr key={j.jurisdiccionId} className="border-b border-border last:border-0">
                    <td className="py-2 pr-4">{j.jurisdiccionCodigo} — {j.jurisdiccionNombre}</td>
                    <td className="py-2 pr-4 text-right tabular-nums">{j.coeficiente.toFixed(4)}</td>
                    <td className="py-2 pr-4 text-right tabular-nums">{formatearPesos(j.baseImponible)}</td>
                    <td className="py-2 pr-4 text-right tabular-nums">{j.alicuota.toFixed(2)}%</td>
                    <td className="py-2 pr-4 text-right tabular-nums">{formatearPesos(j.impuestoDeterminado)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      )}

      {seleccionada && <LiquidacionDetalle liquidacion={seleccionada} />}

      <Card>
        <CardHeader><CardTitle>Liquidaciones de {anio}</CardTitle></CardHeader>
        <CardContent>
          <table className="w-full text-left text-sm">
            <thead className="text-muted-foreground">
              <tr className="border-b border-border">
                <th className="py-2 pr-4 font-medium">Período</th>
                <th className="py-2 pr-4 font-medium">Estado</th>
                <th className="py-2 pr-4 text-right font-medium">A pagar</th>
                <th className="py-2 pr-4 text-right font-medium">A favor</th>
                <th className="py-2 pr-4 font-medium">Asiento</th>
                <th className="py-2 pr-4 font-medium"></th>
              </tr>
            </thead>
            <tbody>
              {(liquidaciones.data?.content ?? []).map((l) => (
                <tr key={l.id} className="border-b border-border last:border-0">
                  <td className="py-2 pr-4">{MESES[l.mes - 1]} {l.anio}</td>
                  <td className="py-2 pr-4">{l.estado}</td>
                  <td className="py-2 pr-4 text-right tabular-nums">{formatearPesos(l.saldoAPagarTotal)}</td>
                  <td className="py-2 pr-4 text-right tabular-nums">{formatearPesos(l.saldoAFavorTotal)}</td>
                  <td className="py-2 pr-4">{l.asientoNumero ?? "—"}</td>
                  <td className="py-2 pr-4">
                    <Button variant="outline" size="sm" onClick={() => setSeleccionadaId(l.id)}>Ver</Button>
                  </td>
                </tr>
              ))}
              {liquidaciones.data?.content.length === 0 && (
                <tr><td colSpan={6} className="py-4 text-center text-muted-foreground">Todavía no hay liquidaciones de {anio}.</td></tr>
              )}
            </tbody>
          </table>
        </CardContent>
      </Card>
    </div>
  )
}

function Advertencias({ items }: { items: string[] }) {
  if (items.length === 0) return null
  return (
    <ul className="space-y-1 rounded-lg border border-amber-500/40 bg-amber-500/10 p-3 text-sm">
      {items.map((a) => <li key={a}>⚠ {a}</li>)}
    </ul>
  )
}

function LiquidacionDetalle({ liquidacion }: { liquidacion: LiquidacionIibb }) {
  const esBorrador = liquidacion.estado === "BORRADOR"
  const recalcular = useRecalcularLiquidacionIibb()
  const confirmar = useConfirmarLiquidacionIibb()
  const anular = useAnularLiquidacionIibb()

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          IIBB {MESES[liquidacion.mes - 1]} {liquidacion.anio} — {liquidacion.estado} · base total{" "}
          {formatearPesos(liquidacion.baseTotal)}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <Advertencias items={liquidacion.advertencias} />

        {liquidacion.jurisdicciones.map((j) => (
          <JurisdiccionCard key={j.id} liquidacionId={liquidacion.id} jurisdiccion={j} editable={esBorrador} />
        ))}

        <div className="flex items-baseline justify-between rounded-lg border border-border p-3">
          <span className="text-sm font-medium">Total del período</span>
          <span className="text-sm tabular-nums">
            A pagar {formatearPesos(liquidacion.saldoAPagarTotal)} · A favor {formatearPesos(liquidacion.saldoAFavorTotal)}
          </span>
        </div>

        <div className="flex gap-2">
          {esBorrador && (
            <>
              <Button variant="outline" disabled={recalcular.isPending} onClick={() => recalcular.mutate(liquidacion.id)}>
                Recalcular base
              </Button>
              <Button disabled={confirmar.isPending} onClick={() => confirmar.mutate(liquidacion.id)}>
                Confirmar y generar asiento
              </Button>
            </>
          )}
          {liquidacion.estado === "CONFIRMADO" && (
            <Button
              variant="outline"
              disabled={anular.isPending}
              onClick={() => {
                const motivo = window.prompt("Motivo para des-confirmar la liquidación:")
                if (motivo) anular.mutate({ id: liquidacion.id, motivo })
              }}
            >
              Des-confirmar (anula el asiento)
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  )
}

function JurisdiccionCard({
  liquidacionId,
  jurisdiccion,
  editable,
}: {
  liquidacionId: number
  jurisdiccion: JurisdiccionIibb
  editable: boolean
}) {
  const editar = useEditarJurisdiccionIibb()
  const [coef, setCoef] = useState(jurisdiccion.coeficiente.toString())
  const [alicuota, setAlicuota] = useState(jurisdiccion.alicuota.toString())
  const cambio = Number(coef) !== jurisdiccion.coeficiente || Number(alicuota) !== jurisdiccion.alicuota

  return (
    <div className="rounded-lg border border-border p-3">
      <div className="mb-2 flex flex-wrap items-center gap-3">
        <span className="font-medium">{jurisdiccion.jurisdiccionCodigo} — {jurisdiccion.jurisdiccionNombre}</span>
        <label className="flex items-center gap-1 text-xs text-muted-foreground">
          Coef.
          {editable ? (
            <Input value={coef} onChange={(e) => setCoef(e.target.value)} type="number" className="h-7 w-24 text-right" />
          ) : <span className="tabular-nums">{jurisdiccion.coeficiente.toFixed(4)}</span>}
        </label>
        <label className="flex items-center gap-1 text-xs text-muted-foreground">
          Alícuota %
          {editable ? (
            <Input value={alicuota} onChange={(e) => setAlicuota(e.target.value)} type="number" className="h-7 w-20 text-right" />
          ) : <span className="tabular-nums">{jurisdiccion.alicuota.toFixed(2)}</span>}
        </label>
        {editable && cambio && (
          <Button
            size="sm"
            disabled={editar.isPending}
            onClick={() =>
              editar.mutate({ id: liquidacionId, jurLiqId: jurisdiccion.id, coeficiente: Number(coef), alicuota: Number(alicuota) })
            }
          >
            Aplicar
          </Button>
        )}
      </div>

      <table className="w-full text-left text-sm">
        <tbody>
          <tr className="border-b border-border">
            <td className="py-1.5 pr-4">Base imponible</td>
            <td className="py-1.5 pr-4 text-right tabular-nums">{formatearPesos(jurisdiccion.baseImponible)}</td>
            <td className="py-1.5"></td>
          </tr>
          <tr className="border-b border-border">
            <td className="py-1.5 pr-4 font-medium">Impuesto determinado</td>
            <td className="py-1.5 pr-4 text-right font-medium tabular-nums">{formatearPesos(jurisdiccion.impuestoDeterminado)}</td>
            <td className="py-1.5"></td>
          </tr>
          {jurisdiccion.componentes.map((c) => (
            <FilaComponente
              key={c.id}
              liquidacionId={liquidacionId}
              jurLiqId={jurisdiccion.id}
              componente={c}
              editable={editable}
            />
          ))}
        </tbody>
      </table>

      <div className="mt-2 text-right text-sm font-medium">
        {jurisdiccion.saldoAPagar > 0
          ? `Saldo a pagar: ${formatearPesos(jurisdiccion.saldoAPagar)}`
          : jurisdiccion.saldoAFavor > 0
            ? `Saldo a favor (arrastra): ${formatearPesos(jurisdiccion.saldoAFavor)}`
            : "Sin saldo"}
      </div>
    </div>
  )
}

function FilaComponente({
  liquidacionId,
  jurLiqId,
  componente,
  editable,
}: {
  liquidacionId: number
  jurLiqId: number
  componente: ComponenteIibb
  editable: boolean
}) {
  const ajustar = useAjustarComponenteIibb()
  const [ajuste, setAjuste] = useState(componente.importeAjuste.toString())
  const [motivo, setMotivo] = useState(componente.motivoAjuste ?? "")
  const cambio = Number(ajuste) !== componente.importeAjuste

  return (
    <tr className="border-b border-border last:border-0">
      <td className="py-1.5 pr-4">{componente.descripcion}</td>
      <td className="py-1.5 pr-4 text-right">
        {editable ? (
          <Input value={ajuste} onChange={(e) => setAjuste(e.target.value)} type="number" className="h-7 w-28 text-right" />
        ) : (
          <span className="tabular-nums">{formatearPesos(componente.importeFinal)}</span>
        )}
      </td>
      <td className="py-1.5">
        {editable && cambio && (
          <div className="flex items-center gap-1">
            {componente.importeCalculado !== 0 && (
              <Input value={motivo} onChange={(e) => setMotivo(e.target.value)} placeholder="Motivo" className="h-7 w-40" />
            )}
            <Button
              size="sm"
              disabled={ajustar.isPending}
              onClick={() =>
                ajustar.mutate({
                  id: liquidacionId,
                  jurLiqId,
                  componenteId: componente.id,
                  importeAjuste: Number(ajuste),
                  motivoAjuste: motivo || undefined,
                })
              }
            >
              Guardar
            </Button>
          </div>
        )}
      </td>
    </tr>
  )
}

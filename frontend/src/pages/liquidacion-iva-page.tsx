import { Fragment, useState } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import {
  useAgregarComponenteIva,
  useAjustarComponenteIva,
  useAnularLiquidacionIva,
  useConfirmarLiquidacionIva,
  useCrearLiquidacionIva,
  useEliminarComponenteIva,
  useLiquidacionesIva,
  usePrevisualizacionIva,
  useRecalcularLiquidacionIva,
  descargarLiquidacionesIvaExcel,
  descargarLiquidacionesIvaPdf,
} from "@/hooks/use-liquidacion-iva"
import { AtribucionProyectos } from "@/components/atribucion-proyectos"
import { useCuentasContables } from "@/hooks/use-cuenta-contable"
import { ETAPA_DE } from "@/types/liquidacion-iva"
import type {
  ComponenteLiquidacionIva,
  LiquidacionIva,
  TipoComponenteIva,
} from "@/types/liquidacion-iva"

const MESES = [
  "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
  "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
]

const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"

const pesos = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" })

function formatearPesos(valor: number) {
  return pesos.format(valor)
}

export function LiquidacionIvaPage() {
  const hoy = new Date()
  const [anio, setAnio] = useState(hoy.getFullYear())
  const [mes, setMes] = useState(hoy.getMonth() + 1)
  const [seleccionadaId, setSeleccionadaId] = useState<number | null>(null)
  const [descargando, setDescargando] = useState<"excel" | "pdf" | null>(null)

  const liquidaciones = useLiquidacionesIva({ anio })
  const previsualizacion = usePrevisualizacionIva(anio, mes)
  const crear = useCrearLiquidacionIva()

  async function exportar(formato: "excel" | "pdf") {
    setDescargando(formato)
    try {
      if (formato === "excel") await descargarLiquidacionesIvaExcel({ anio })
      else await descargarLiquidacionesIvaPdf({ anio })
    } finally {
      setDescargando(null)
    }
  }

  const seleccionada = liquidaciones.data?.content.find((l) => l.id === seleccionadaId)
  const yaLiquidado = liquidaciones.data?.content.some(
    (l) => l.mes === mes && l.estado !== "ANULADO",
  )

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-lg font-semibold text-foreground">Liquidación de IVA</h1>
        <p className="text-sm text-muted-foreground">
          Los importes se calculan desde los asientos confirmados del período, en las dos etapas del art. 24 de la
          Ley de IVA. Podés ajustar cada concepto (con motivo) antes de confirmar; al confirmar se genera el asiento
          contra el pasivo fiscal o el saldo a favor que corresponda.
        </p>
      </div>

      <Card>
        <CardHeader><CardTitle>Período</CardTitle></CardHeader>
        <CardContent className="grid gap-3 sm:grid-cols-4">
          <label className="flex flex-col text-xs text-muted-foreground">
            Año
            <Input
              type="number"
              value={anio}
              onChange={(e) => setAnio(Number(e.target.value))}
              className="h-8"
            />
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Mes
            <select value={mes} onChange={(e) => setMes(Number(e.target.value))} className={selectClase}>
              {MESES.map((m, i) => (
                <option key={m} value={i + 1}>{m}</option>
              ))}
            </select>
          </label>
          <div className="flex items-end sm:col-span-2">
            <Button
              disabled={yaLiquidado || crear.isPending}
              onClick={() =>
                crear.mutate({ anio, mes }, { onSuccess: (l) => setSeleccionadaId(l.id) })
              }
            >
              {yaLiquidado ? "Ya hay una liquidación de este mes" : "Liquidar este período"}
            </Button>
          </div>
        </CardContent>
      </Card>

      {!seleccionada && previsualizacion.data && (
        <PrevisualizacionSection />
      )}

      {seleccionada && <LiquidacionDetalle liquidacion={seleccionada} />}

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>Liquidaciones de {anio}</CardTitle>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={descargando !== null || !liquidaciones.data} onClick={() => exportar("excel")}>
              {descargando === "excel" ? "Exportando…" : "Exportar Excel"}
            </Button>
            <Button variant="outline" size="sm" disabled={descargando !== null || !liquidaciones.data} onClick={() => exportar("pdf")}>
              {descargando === "pdf" ? "Exportando…" : "Exportar PDF"}
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <table className="w-full text-left text-sm">
            <thead className="text-muted-foreground">
              <tr className="border-b border-border">
                <th className="py-2 pr-4 font-medium">Período</th>
                <th className="py-2 pr-4 font-medium">Estado</th>
                <th className="py-2 pr-4 text-right font-medium">A pagar</th>
                <th className="py-2 pr-4 text-right font-medium">Saldo técnico</th>
                <th className="py-2 pr-4 text-right font-medium">Libre disp.</th>
                <th className="py-2 pr-4 font-medium">Asiento</th>
                <th className="py-2 pr-4 font-medium"></th>
              </tr>
            </thead>
            <tbody>
              {(liquidaciones.data?.content ?? []).map((l) => (
                <tr key={l.id} className="border-b border-border last:border-0">
                  <td className="py-2 pr-4">{MESES[l.mes - 1]} {l.anio}</td>
                  <td className="py-2 pr-4">{l.estado}</td>
                  <td className="py-2 pr-4 text-right tabular-nums">{formatearPesos(l.saldoAPagar)}</td>
                  <td className="py-2 pr-4 text-right tabular-nums">{formatearPesos(l.saldoAFavor)}</td>
                  <td className="py-2 pr-4 text-right tabular-nums">{formatearPesos(l.saldoLibreDisponibilidad)}</td>
                  <td className="py-2 pr-4">{l.asientoNumero ?? "—"}</td>
                  <td className="py-2 pr-4">
                    <Button variant="outline" size="sm" onClick={() => setSeleccionadaId(l.id)}>
                      Ver
                    </Button>
                  </td>
                </tr>
              ))}
              {liquidaciones.data?.content.length === 0 && (
                <tr>
                  <td colSpan={7} className="py-4 text-center text-muted-foreground">
                    Todavía no hay liquidaciones de {anio}.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </CardContent>
      </Card>
    </div>
  )

  function PrevisualizacionSection() {
    const p = previsualizacion.data!
    return (
      <Card>
        <CardHeader>
          <CardTitle>
            Previsualización de {MESES[p.mes - 1]} {p.anio} — todavía no liquidado
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <Advertencias items={p.advertencias} />
          <table className="w-full text-left text-sm">
            <tbody>
              {p.componentes.map((c) => (
                <tr key={c.tipo} className="border-b border-border last:border-0">
                  <td className="py-2 pr-4">{c.descripcion}</td>
                  <td className="py-2 pr-4 text-right tabular-nums">{formatearPesos(c.importe)}</td>
                  <td className="py-2 pr-4 text-right text-xs text-muted-foreground">
                    {c.detalle.length > 0 ? `${c.detalle.length} imputación(es)` : "—"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <Resultado saldoAPagar={p.saldoAPagar} saldoAFavor={p.saldoAFavor} saldoLibreDisponibilidad={p.saldoLibreDisponibilidad} />
        </CardContent>
      </Card>
    )
  }
}

function Advertencias({ items }: { items: string[] }) {
  if (items.length === 0) return null
  return (
    <ul className="space-y-1 rounded-lg border border-amber-500/40 bg-amber-500/10 p-3 text-sm">
      {items.map((a) => <li key={a}>⚠ {a}</li>)}
    </ul>
  )
}

/**
 * Los tres resultados del art. 24. El técnico y el de libre disponibilidad
 * pueden convivir en un mismo mes, así que se muestran por separado y no como
 * un único "saldo a favor": el técnico solo se computa contra IVA futuro,
 * mientras que el de libre disponibilidad además se compensa con otros
 * impuestos, se transfiere y se puede pedir devuelto.
 */
function Resultado({
  saldoAPagar,
  saldoAFavor,
  saldoLibreDisponibilidad,
}: {
  saldoAPagar: number
  saldoAFavor: number
  saldoLibreDisponibilidad: number
}) {
  const nada = saldoAPagar === 0 && saldoAFavor === 0 && saldoLibreDisponibilidad === 0
  return (
    <div className="space-y-2 rounded-lg border border-border p-3">
      {saldoAPagar > 0 && (
        <Linea titulo="Saldo a pagar" importe={saldoAPagar} destacado />
      )}
      {saldoAFavor > 0 && (
        <Linea
          titulo="Saldo técnico a favor"
          detalle="Solo se computa contra el IVA de períodos siguientes."
          importe={saldoAFavor}
          destacado={saldoAPagar === 0}
        />
      )}
      {saldoLibreDisponibilidad > 0 && (
        <Linea
          titulo="Saldo de libre disponibilidad"
          detalle="Compensable con otros impuestos, transferible y con derecho a devolución."
          importe={saldoLibreDisponibilidad}
          destacado={saldoAPagar === 0 && saldoAFavor === 0}
        />
      )}
      {nada && <p className="text-sm text-muted-foreground">El período no arroja saldo a pagar ni a favor.</p>}
    </div>
  )
}

function Linea({
  titulo,
  detalle,
  importe,
  destacado,
}: {
  titulo: string
  detalle?: string
  importe: number
  destacado?: boolean
}) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <div>
        <span className="text-sm font-medium">{titulo}</span>
        {detalle && <p className="text-xs text-muted-foreground">{detalle}</p>}
      </div>
      <span className={destacado ? "text-lg font-semibold tabular-nums" : "text-sm font-medium tabular-nums"}>
        {formatearPesos(importe)}
      </span>
    </div>
  )
}

function LiquidacionDetalle({ liquidacion }: { liquidacion: LiquidacionIva }) {
  const esBorrador = liquidacion.estado === "BORRADOR"
  const recalcular = useRecalcularLiquidacionIva()
  const confirmar = useConfirmarLiquidacionIva()
  const anular = useAnularLiquidacionIva()
  const agregar = useAgregarComponenteIva()
  const eliminar = useEliminarComponenteIva()

  const cuentasContables = useCuentasContables({ activo: true, page: 0, size: 500 })
  const cuentasImputables = (cuentasContables.data?.content ?? []).filter((c) => c.imputable)

  const [nuevoTipo, setNuevoTipo] = useState<TipoComponenteIva>("OTRO_TECNICO")
  const [nuevaDescripcion, setNuevaDescripcion] = useState("")
  const [nuevoImporte, setNuevoImporte] = useState("")
  const [nuevaCuenta, setNuevaCuenta] = useState("")

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          {MESES[liquidacion.mes - 1]} {liquidacion.anio} — {liquidacion.estado}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <Advertencias items={liquidacion.advertencias} />

        <table className="w-full text-left text-sm">
          <thead className="text-muted-foreground">
            <tr className="border-b border-border">
              <th className="py-2 pr-4 font-medium">Concepto</th>
              <th className="py-2 pr-4 text-right font-medium">Calculado</th>
              <th className="py-2 pr-4 text-right font-medium">Ajuste</th>
              <th className="py-2 pr-4 text-right font-medium">Final</th>
              <th className="py-2 pr-4 font-medium">Motivo</th>
              <th className="py-2 pr-4 font-medium"></th>
            </tr>
          </thead>
          <tbody>
            {(["TECNICA", "INGRESOS_DIRECTOS"] as const).map((etapa) => {
              const delaEtapa = liquidacion.componentes.filter((c) => ETAPA_DE[c.tipo] === etapa)
              if (delaEtapa.length === 0) return null
              return (
                <Fragment key={etapa}>
                  <tr className="border-b border-border bg-muted/40">
                    <td colSpan={6} className="py-1.5 pr-4 text-xs font-medium uppercase tracking-wide text-muted-foreground">
                      {etapa === "TECNICA"
                        ? "Etapa técnica — determina el impuesto del período"
                        : "Ingresos directos — percepciones y retenciones sufridas"}
                    </td>
                  </tr>
                  {delaEtapa.map((c) => (
                    <FilaComponente
                      key={c.id}
                      liquidacionId={liquidacion.id}
                      componente={c}
                      editable={esBorrador}
                      onEliminar={() => eliminar.mutate({ id: liquidacion.id, componenteId: c.id })}
                    />
                  ))}
                </Fragment>
              )
            })}
          </tbody>
        </table>

        <Resultado
          saldoAPagar={liquidacion.saldoAPagar}
          saldoAFavor={liquidacion.saldoAFavor}
          saldoLibreDisponibilidad={liquidacion.saldoLibreDisponibilidad}
        />

        {liquidacion.estado === "CONFIRMADO" && (
          <AtribucionProyectos tipo="IVA" liquidacionId={liquidacion.id} />
        )}

        {esBorrador && (
          <div className="space-y-3 rounded-lg border border-border p-3">
            <p className="text-xs text-muted-foreground">
              Agregar un concepto que el sistema no calcula (restituciones u otros ajustes). La cuenta contable es
              obligatoria: sin ella el asiento no balancearía.
            </p>
            <div className="grid gap-2 sm:grid-cols-5">
              <select value={nuevoTipo} onChange={(e) => setNuevoTipo(e.target.value as TipoComponenteIva)} className={selectClase}>
                <option value="OTRO_TECNICO">Otro concepto (etapa técnica)</option>
                <option value="OTRO_INGRESO_DIRECTO">Otro ingreso directo (percepción/retención)</option>
              </select>
              <Input value={nuevaDescripcion} onChange={(e) => setNuevaDescripcion(e.target.value)} placeholder="Descripción" className="h-8" />
              <Input value={nuevoImporte} onChange={(e) => setNuevoImporte(e.target.value)} placeholder="Importe" type="number" className="h-8" />
              <select value={nuevaCuenta} onChange={(e) => setNuevaCuenta(e.target.value)} className={selectClase}>
                <option value="">Cuenta contable…</option>
                {cuentasImputables.map((c) => (
                  <option key={c.id} value={c.id.toString()}>{c.codigo} — {c.nombre}</option>
                ))}
              </select>
              <Button
                size="sm"
                disabled={!nuevaDescripcion || !nuevoImporte || !nuevaCuenta || agregar.isPending}
                onClick={() =>
                  agregar.mutate(
                    {
                      id: liquidacion.id,
                      tipo: nuevoTipo,
                      descripcion: nuevaDescripcion,
                      importe: Number(nuevoImporte),
                      cuentaContableId: Number(nuevaCuenta),
                    },
                    {
                      onSuccess: () => {
                        setNuevaDescripcion("")
                        setNuevoImporte("")
                        setNuevaCuenta("")
                      },
                    },
                  )
                }
              >
                Agregar
              </Button>
            </div>
          </div>
        )}

        <div className="flex gap-2">
          {esBorrador && (
            <>
              <Button variant="outline" disabled={recalcular.isPending} onClick={() => recalcular.mutate(liquidacion.id)}>
                Recalcular desde los asientos
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

function FilaComponente({
  liquidacionId,
  componente,
  editable,
  onEliminar,
}: {
  liquidacionId: number
  componente: ComponenteLiquidacionIva
  editable: boolean
  onEliminar: () => void
}) {
  const ajustar = useAjustarComponenteIva()
  const [ajuste, setAjuste] = useState(componente.importeAjuste.toString())
  const [motivo, setMotivo] = useState(componente.motivoAjuste ?? "")
  const cambio = Number(ajuste) !== componente.importeAjuste

  return (
    <tr className="border-b border-border last:border-0">
      <td className="py-2 pr-4">
        {componente.descripcion}
        {componente.cuentaContableCodigo && (
          <span className="ml-1 text-xs text-muted-foreground">({componente.cuentaContableCodigo})</span>
        )}
      </td>
      <td className="py-2 pr-4 text-right tabular-nums">{formatearPesos(componente.importeCalculado)}</td>
      <td className="py-2 pr-4 text-right">
        {editable ? (
          <Input value={ajuste} onChange={(e) => setAjuste(e.target.value)} type="number" className="h-8 w-28 text-right" />
        ) : (
          <span className="tabular-nums">{formatearPesos(componente.importeAjuste)}</span>
        )}
      </td>
      <td className="py-2 pr-4 text-right font-medium tabular-nums">{formatearPesos(componente.importeFinal)}</td>
      <td className="py-2 pr-4">
        {editable ? (
          <Input value={motivo} onChange={(e) => setMotivo(e.target.value)} placeholder="Motivo del ajuste" className="h-8" />
        ) : (
          componente.motivoAjuste ?? "—"
        )}
      </td>
      <td className="py-2 pr-4">
        {editable && (
          <div className="flex gap-1">
            {cambio && (
              <Button
                size="sm"
                disabled={ajustar.isPending}
                onClick={() =>
                  ajustar.mutate({
                    id: liquidacionId,
                    componenteId: componente.id,
                    importeAjuste: Number(ajuste),
                    motivoAjuste: motivo,
                  })
                }
              >
                Guardar
              </Button>
            )}
            {componente.manual && (
              <Button size="sm" variant="outline" onClick={onEliminar}>Quitar</Button>
            )}
          </div>
        )}
      </td>
    </tr>
  )
}

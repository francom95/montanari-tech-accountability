import { zodResolver } from "@hookform/resolvers/zod"
import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from "@tanstack/react-table"
import { useMemo, useState } from "react"
import { useFieldArray, useForm, useWatch, type Control } from "react-hook-form"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { ConfiguracionCobranzaCard } from "@/components/configuracion-cobranza-card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useClientes } from "@/hooks/use-cliente"
import {
  useAnularCobro,
  useAplicarAnticipoCobro,
  useCobros,
  useConfirmarCobro,
  useCrearCobro,
  useEditarCobro,
  useEliminarCobro,
} from "@/hooks/use-cobro"
import { useCuentasBancarias } from "@/hooks/use-cuenta-bancaria"
import { useFacturasVenta } from "@/hooks/use-factura-venta"
import { useMonedas } from "@/hooks/use-monedas"
import type { Cobro, EstadoCobro, TipoRetencionCobro } from "@/types/cobro"
import { TIPOS_RETENCION_COBRO } from "@/types/cobro"

const ESTADOS: EstadoCobro[] = ["BORRADOR", "CONFIRMADO", "ANULADO"]
const ESTADO_LABEL: Record<EstadoCobro, string> = { BORRADOR: "Borrador", CONFIRMADO: "Confirmado", ANULADO: "Anulado" }
const RETENCION_LABEL: Record<TipoRetencionCobro, string> = { RETENCION_GANANCIAS: "Retención de Ganancias", RETENCION_IVA: "Retención de IVA" }

const esquemaLinea = z.object({
  facturaVentaId: z.string().min(1, "Obligatoria"),
  montoImputadoOriginal: z.string().min(1, "Obligatorio"),
  recargoMoraOriginal: z.string().optional(),
})

const esquemaTributo = z.object({
  tipo: z.string().min(1),
  importe: z.string().min(1, "Obligatorio"),
})

const esquema = z.object({
  clienteId: z.string().min(1, "El cliente es obligatorio"),
  fecha: z.string().min(1, "La fecha es obligatoria"),
  monedaId: z.string().min(1, "La moneda es obligatoria"),
  tipoCambio: z.string().min(1, "El tipo de cambio es obligatorio"),
  cuentaBancariaId: z.string().min(1, "La cuenta bancaria es obligatoria"),
  total: z.string().min(1, "El total cobrado es obligatorio"),
  observaciones: z.string().optional(),
  lineas: z.array(esquemaLinea),
  tributos: z.array(esquemaTributo),
})

type Valores = z.infer<typeof esquema>
type LineaValores = Valores["lineas"][number]
type TributoValores = Valores["tributos"][number]

const LINEA_VACIA: LineaValores = { facturaVentaId: "", montoImputadoOriginal: "", recargoMoraOriginal: "" }
const TRIBUTO_VACIO: TributoValores = { tipo: "RETENCION_GANANCIAS", importe: "" }

const VACIO: Valores = {
  clienteId: "", fecha: "", monedaId: "", tipoCambio: "1", cuentaBancariaId: "",
  total: "", observaciones: "", lineas: [], tributos: [],
}

function cobroAValores(c: Cobro): Valores {
  return {
    clienteId: c.clienteId.toString(),
    fecha: c.fecha,
    monedaId: c.monedaId.toString(),
    tipoCambio: c.tipoCambio.toString(),
    cuentaBancariaId: c.cuentaBancariaId.toString(),
    total: c.total.toString(),
    observaciones: c.observaciones ?? "",
    lineas: c.lineas.map((l) => ({
      facturaVentaId: l.facturaVentaId.toString(),
      montoImputadoOriginal: l.montoImputadoOriginal.toString(),
      recargoMoraOriginal: l.recargoMoraOriginal?.toString() ?? "",
    })),
    tributos: c.tributos.map((t) => ({ tipo: t.tipo, importe: t.importe.toString() })),
  }
}

const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"
const inputClase = "h-8"

export function CobrosPage() {
  const [page, setPage] = useState(0)
  const [estadoFiltro, setEstadoFiltro] = useState<EstadoCobro | "">("")
  const [editando, setEditando] = useState<Cobro | null>(null)
  const [mostrarForm, setMostrarForm] = useState(false)
  const [anulando, setAnulando] = useState<number | null>(null)
  const [motivoAnulacion, setMotivoAnulacion] = useState("")
  const [aplicandoAnticipo, setAplicandoAnticipo] = useState<number | null>(null)
  const [facturaAnticipo, setFacturaAnticipo] = useState("")
  const [montoAnticipo, setMontoAnticipo] = useState("")
  const [fechaAnticipo, setFechaAnticipo] = useState("")

  const query = useCobros({ estado: estadoFiltro || undefined, page, size: 10 })
  const crear = useCrearCobro()
  const editar = useEditarCobro()
  const eliminar = useEliminarCobro()
  const confirmar = useConfirmarCobro()
  const anular = useAnularCobro()
  const aplicarAnticipo = useAplicarAnticipoCobro()

  const clientes = useClientes({ activo: true, page: 0, size: 200 })
  const cuentasBancarias = useCuentasBancarias({ activo: true, page: 0, size: 100 })
  const monedas = useMonedas({ page: 0, size: 20 })
  const monedaArsId = monedas.data?.content?.find((m) => m.codigo === "ARS")?.id
  const facturasConfirmadas = useFacturasVenta({ estado: "CONFIRMADO", page: 0, size: 500 })

  const form = useForm<Valores>({ resolver: zodResolver(esquema), defaultValues: VACIO })
  const lineas = useFieldArray({ control: form.control, name: "lineas" })
  const tributos = useFieldArray({ control: form.control, name: "tributos" })

  function nuevoCobro() {
    setEditando(null)
    form.reset({ ...VACIO, monedaId: monedaArsId ? monedaArsId.toString() : "" })
    setMostrarForm(true)
  }

  function iniciarEdicion(c: Cobro) {
    setEditando(c)
    form.reset(cobroAValores(c))
    setMostrarForm(true)
  }

  function cancelar() {
    setMostrarForm(false)
    setEditando(null)
  }

  function onSubmit(valores: Valores) {
    const payload = {
      clienteId: Number(valores.clienteId),
      fecha: valores.fecha,
      monedaId: Number(valores.monedaId),
      tipoCambio: Number(valores.tipoCambio),
      cuentaBancariaId: Number(valores.cuentaBancariaId),
      total: Number(valores.total),
      observaciones: valores.observaciones || undefined,
      lineas: valores.lineas.map((l) => ({
        facturaVentaId: Number(l.facturaVentaId),
        montoImputadoOriginal: Number(l.montoImputadoOriginal),
        recargoMoraOriginal: l.recargoMoraOriginal ? Number(l.recargoMoraOriginal) : undefined,
      })),
      tributos: valores.tributos.map((t) => ({ tipo: t.tipo as TipoRetencionCobro, importe: Number(t.importe) })),
    }
    if (editando) {
      editar.mutate({ id: editando.id, valores: payload }, { onSuccess: cancelar })
    } else {
      crear.mutate(payload, { onSuccess: cancelar })
    }
  }

  function confirmarAnulacion(id: number) {
    if (!motivoAnulacion.trim()) return
    anular.mutate({ id, motivo: motivoAnulacion }, { onSuccess: () => { setAnulando(null); setMotivoAnulacion("") } })
  }

  function confirmarAplicacionAnticipo(id: number) {
    if (!facturaAnticipo || !montoAnticipo || !fechaAnticipo) return
    aplicarAnticipo.mutate(
      { id, facturaVentaId: Number(facturaAnticipo), monto: Number(montoAnticipo), fecha: fechaAnticipo },
      { onSuccess: () => { setAplicandoAnticipo(null); setFacturaAnticipo(""); setMontoAnticipo(""); setFechaAnticipo("") } }
    )
  }

  const lineasObservadas = useWatch({ control: form.control, name: "lineas" })
  const tributosObservados = useWatch({ control: form.control, name: "tributos" })
  const totalObservado = useWatch({ control: form.control, name: "total" })
  const fechaObservada = useWatch({ control: form.control, name: "fecha" })
  const totales = useMemo(() => {
    let imputado = 0
    for (const l of lineasObservadas ?? []) imputado += Number(l.montoImputadoOriginal) || 0
    let retenciones = 0
    for (const t of tributosObservados ?? []) retenciones += Number(t.importe) || 0
    const total = Number(totalObservado) || 0
    return { imputado, retenciones, anticipo: Math.max(0, total - imputado), fondos: total - retenciones }
  }, [lineasObservadas, tributosObservados, totalObservado])

  const columnas = useMemo<ColumnDef<Cobro>[]>(
    () => [
      { header: "Fecha", accessorKey: "fecha" },
      { header: "Cliente", accessorKey: "clienteNombre" },
      { header: "Total", accessorKey: "total", cell: (info) => (info.getValue() as number).toFixed(2) },
      { header: "Anticipo", accessorKey: "montoAnticipoDisponible", cell: (info) => (info.getValue() as number).toFixed(2) },
      { header: "Estado", accessorKey: "estado", cell: (info) => ESTADO_LABEL[info.getValue() as EstadoCobro] },
      { header: "N° Asiento", accessorKey: "asientoNumero", cell: (info) => info.getValue() ?? "—" },
      {
        header: "Acciones",
        id: "acciones",
        cell: ({ row }) => {
          const c = row.original
          if (anulando === c.id) {
            return (
              <div className="flex items-center gap-2">
                <Input autoFocus placeholder="Motivo…" value={motivoAnulacion} onChange={(e) => setMotivoAnulacion(e.target.value)} className={`${inputClase} w-40`} />
                <Button size="sm" disabled={anular.isPending || !motivoAnulacion.trim()} onClick={() => confirmarAnulacion(c.id)}>Confirmar</Button>
                <Button variant="outline" size="sm" onClick={() => { setAnulando(null); setMotivoAnulacion("") }}>Cancelar</Button>
              </div>
            )
          }
          if (aplicandoAnticipo === c.id) {
            return (
              <div className="flex flex-wrap items-center gap-2">
                <select value={facturaAnticipo} onChange={(e) => setFacturaAnticipo(e.target.value)} className={`${selectClase} w-40`}>
                  <option value="">Factura…</option>
                  {facturasConfirmadas.data?.content?.filter((f) => f.clienteId === c.clienteId).map((f) => (
                    <option key={f.id} value={f.id.toString()}>{f.numero}</option>
                  ))}
                </select>
                <Input placeholder="Monto" value={montoAnticipo} onChange={(e) => setMontoAnticipo(e.target.value)} className={`${inputClase} w-24`} />
                <Input type="date" value={fechaAnticipo} onChange={(e) => setFechaAnticipo(e.target.value)} className={`${inputClase} w-36`} />
                <Button size="sm" disabled={aplicarAnticipo.isPending} onClick={() => confirmarAplicacionAnticipo(c.id)}>Aplicar</Button>
                <Button variant="outline" size="sm" onClick={() => setAplicandoAnticipo(null)}>Cancelar</Button>
              </div>
            )
          }
          if (c.estado === "ANULADO") return <span className="text-xs text-muted-foreground">Anulado</span>
          return (
            <div className="flex flex-wrap gap-2">
              <Button variant="outline" size="sm" onClick={() => iniciarEdicion(c)}>{c.estado === "BORRADOR" ? "Editar" : "Ver"}</Button>
              {c.estado === "BORRADOR" && (
                <>
                  <Button variant="outline" size="sm" disabled={confirmar.isPending} onClick={() => confirmar.mutate(c.id)}>Confirmar</Button>
                  <Button variant="outline" size="sm" disabled={eliminar.isPending} onClick={() => eliminar.mutate(c.id)}>Eliminar</Button>
                </>
              )}
              {c.estado === "CONFIRMADO" && (
                <>
                  {c.montoAnticipoDisponible > 0 && (
                    <Button variant="outline" size="sm" onClick={() => setAplicandoAnticipo(c.id)}>Aplicar anticipo</Button>
                  )}
                  <Button variant="outline" size="sm" disabled={anular.isPending} onClick={() => setAnulando(c.id)}>Anular</Button>
                </>
              )}
            </div>
          )
        },
      },
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [confirmar.isPending, eliminar.isPending, anular.isPending, anulando, motivoAnulacion, aplicandoAnticipo, facturaAnticipo, montoAnticipo, fechaAnticipo, aplicarAnticipo.isPending, facturasConfirmadas.data]
  )

  const tabla = useReactTable({
    data: query.data?.content ?? [],
    columns: columnas,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
    pageCount: query.data?.totalPages ?? 0,
  })

  const soloLectura = editando != null && editando.estado !== "BORRADOR"

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-semibold text-foreground">Cobros</h1>
          <p className="text-sm text-muted-foreground">Imputa contra una o varias facturas de venta; el remanente queda como anticipo del cliente (F4.1/F4.4).</p>
        </div>
        {!mostrarForm && <Button onClick={nuevoCobro}>Nuevo cobro</Button>}
      </div>

      {!mostrarForm && <ConfiguracionCobranzaCard />}

      {mostrarForm && (
        <Card>
          <CardHeader>
            <CardTitle>
              {editando ? `${soloLectura ? "Ver" : "Editar"} cobro #${editando.id}${editando.asientoNumero ? ` — Asiento N° ${editando.asientoNumero}` : ""}` : "Nuevo cobro (borrador)"}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <Form {...form}>
              <fieldset disabled={soloLectura} className="space-y-4">
                <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                  <div className="grid gap-4 sm:grid-cols-4">
                    <FormField control={form.control} name="clienteId" render={({ field }) => (
                      <FormItem>
                        <FormLabel>Cliente</FormLabel>
                        <FormControl>
                          <select {...field} disabled={clientes.isLoading || soloLectura} className={selectClase}>
                            <option value="">Seleccionar…</option>
                            {clientes.data?.content?.map((c) => <option key={c.id} value={c.id.toString()}>{c.nombre}</option>)}
                          </select>
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )} />
                    <FormField control={form.control} name="fecha" render={({ field }) => (
                      <FormItem><FormLabel>Fecha</FormLabel><FormControl><Input {...field} type="date" className={inputClase} /></FormControl><FormMessage /></FormItem>
                    )} />
                    <FormField control={form.control} name="monedaId" render={({ field }) => (
                      <FormItem>
                        <FormLabel>Moneda</FormLabel>
                        <FormControl>
                          <select {...field} disabled={monedas.isLoading} className={selectClase}>
                            <option value="">Seleccionar…</option>
                            {monedas.data?.content?.map((m) => <option key={m.id} value={m.id.toString()}>{m.codigo}</option>)}
                          </select>
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )} />
                    <FormField control={form.control} name="tipoCambio" render={({ field }) => (
                      <FormItem><FormLabel>Tipo de cambio</FormLabel><FormControl><Input {...field} type="number" step="0.000001" className={inputClase} /></FormControl><FormMessage /></FormItem>
                    )} />
                  </div>
                  <div className="grid gap-4 sm:grid-cols-3">
                    <FormField control={form.control} name="cuentaBancariaId" render={({ field }) => (
                      <FormItem>
                        <FormLabel>Cuenta bancaria (medio de fondos)</FormLabel>
                        <FormControl>
                          <select {...field} disabled={cuentasBancarias.isLoading} className={selectClase}>
                            <option value="">Seleccionar…</option>
                            {cuentasBancarias.data?.content?.map((cb) => <option key={cb.id} value={cb.id.toString()}>{cb.alias}</option>)}
                          </select>
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )} />
                    <FormField control={form.control} name="total" render={({ field }) => (
                      <FormItem><FormLabel>Total cobrado (bruto)</FormLabel><FormControl><Input {...field} type="number" step="0.01" className={inputClase} /></FormControl><FormMessage /></FormItem>
                    )} />
                    <FormField control={form.control} name="observaciones" render={({ field }) => (
                      <FormItem><FormLabel>Observaciones</FormLabel><FormControl><Input {...field} className={inputClase} /></FormControl><FormMessage /></FormItem>
                    )} />
                  </div>

                  <div>
                    <h3 className="mb-2 text-sm font-medium">Imputación contra facturas (opcional — el remanente queda como anticipo)</h3>
                    <div className="overflow-x-auto">
                      <table className="w-full min-w-[500px] text-left text-sm">
                        <thead className="text-muted-foreground">
                          <tr className="border-b border-border">
                            <th className="py-2 pr-2 font-medium">Factura de venta</th>
                            <th className="py-2 pr-2 font-medium">Monto imputado</th>
                            <th className="py-2 pr-2 font-medium">Recargo por mora</th>
                            <th className="py-2 pr-2 font-medium"></th>
                          </tr>
                        </thead>
                        <tbody>
                          {lineas.fields.map((campo, indice) => (
                            <LineaImputacionRow key={campo.id} control={form.control} indice={indice} soloLectura={soloLectura}
                              facturas={facturasConfirmadas.data?.content ?? []} facturasCargando={facturasConfirmadas.isLoading}
                              fechaCobro={fechaObservada} onQuitar={() => lineas.remove(indice)} />
                          ))}
                        </tbody>
                      </table>
                    </div>
                    {!soloLectura && <Button type="button" variant="outline" size="sm" className="mt-2" onClick={() => lineas.append({ ...LINEA_VACIA })}>Agregar imputación</Button>}
                  </div>

                  <div>
                    <h3 className="mb-2 text-sm font-medium">Retenciones sufridas (opcional)</h3>
                    <div className="overflow-x-auto">
                      <table className="w-full min-w-[400px] text-left text-sm">
                        <thead className="text-muted-foreground">
                          <tr className="border-b border-border">
                            <th className="py-2 pr-2 font-medium">Tipo</th>
                            <th className="py-2 pr-2 font-medium">Importe</th>
                            <th className="py-2 pr-2 font-medium"></th>
                          </tr>
                        </thead>
                        <tbody>
                          {tributos.fields.map((campo, indice) => (
                            <TributoRow key={campo.id} control={form.control} indice={indice} soloLectura={soloLectura} onQuitar={() => tributos.remove(indice)} />
                          ))}
                        </tbody>
                      </table>
                    </div>
                    {!soloLectura && <Button type="button" variant="outline" size="sm" className="mt-2" onClick={() => tributos.append({ ...TRIBUTO_VACIO })}>Agregar retención</Button>}
                  </div>

                  <div className="flex gap-4 text-sm">
                    <span>Imputado: <strong>{totales.imputado.toFixed(2)}</strong></span>
                    <span>Anticipo: <strong>{totales.anticipo.toFixed(2)}</strong></span>
                    <span>Retenciones: <strong>{totales.retenciones.toFixed(2)}</strong></span>
                    <span>Ingresa a fondos: <strong>{totales.fondos.toFixed(2)}</strong></span>
                  </div>

                  {!soloLectura && (
                    <div className="flex gap-2">
                      <Button type="submit" disabled={crear.isPending || editar.isPending}>{editando ? "Guardar borrador" : "Crear borrador"}</Button>
                      <Button type="button" variant="outline" onClick={cancelar}>Cancelar</Button>
                    </div>
                  )}
                </form>
              </fieldset>
            </Form>

            {editando && editando.aplicacionesAnticipo.length > 0 && (
              <div className="space-y-1 border-t border-border pt-4">
                <h3 className="text-sm font-medium">Aplicaciones de anticipo registradas</h3>
                <ul className="space-y-1 text-sm text-muted-foreground">
                  {editando.aplicacionesAnticipo.map((a) => (
                    <li key={a.id}>Factura {a.facturaVentaNumero}: {a.montoOriginal.toFixed(2)} ({a.fecha}) — Asiento N° {a.asientoNumero}</li>
                  ))}
                </ul>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader><CardTitle>Listado</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <select value={estadoFiltro} onChange={(e) => { setEstadoFiltro(e.target.value as EstadoCobro | ""); setPage(0) }} className={`${selectClase} max-w-40`}>
            <option value="">Todos los estados</option>
            {ESTADOS.map((e) => <option key={e} value={e}>{ESTADO_LABEL[e]}</option>)}
          </select>
          {query.isLoading ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : (
            <>
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground">
                  {tabla.getHeaderGroups().map((hg) => (
                    <tr key={hg.id} className="border-b border-border">
                      {hg.headers.map((h) => <th key={h.id} className="py-2 pr-4 font-medium">{flexRender(h.column.columnDef.header, h.getContext())}</th>)}
                    </tr>
                  ))}
                </thead>
                <tbody>
                  {tabla.getRowModel().rows.map((row) => (
                    <tr key={row.id} className="border-b border-border last:border-0">
                      {row.getVisibleCells().map((cell) => <td key={cell.id} className="py-2 pr-4">{flexRender(cell.column.columnDef.cell, cell.getContext())}</td>)}
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="flex items-center gap-2">
                <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}>Anterior</Button>
                <span className="text-sm text-muted-foreground">Página {page + 1} de {query.data?.totalPages || 1}</span>
                <Button variant="outline" size="sm" disabled={page + 1 >= (query.data?.totalPages ?? 1)} onClick={() => setPage((p) => p + 1)}>Siguiente</Button>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function LineaImputacionRow({ control, indice, soloLectura, facturas, facturasCargando, fechaCobro, onQuitar }: {
  control: Control<Valores>; indice: number; soloLectura: boolean
  facturas: { id: number; numero: string; clienteId: number; fechaVencimiento?: string | null }[]; facturasCargando: boolean
  fechaCobro?: string; onQuitar: () => void
}) {
  const facturaVentaId = useWatch({ control, name: `lineas.${indice}.facturaVentaId` })
  const factura = facturas.find((f) => f.id.toString() === facturaVentaId)
  const diasAtraso = (() => {
    if (!factura?.fechaVencimiento || !fechaCobro) return null
    const dias = Math.round((new Date(fechaCobro).getTime() - new Date(factura.fechaVencimiento).getTime()) / 86_400_000)
    return dias > 0 ? dias : null
  })()

  return (
    <tr className="border-b border-border last:border-0 align-top">
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.facturaVentaId`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={facturasCargando || soloLectura} className={`${selectClase} min-w-40`}>
                <option value="">Seleccionar…</option>
                {facturas.map((f) => <option key={f.id} value={f.id.toString()}>{f.numero}</option>)}
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.montoImputadoOriginal`} render={({ field }) => (
          <FormItem><FormControl><Input {...field} type="number" step="0.01" disabled={soloLectura} className={`${inputClase} w-32`} /></FormControl><FormMessage /></FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.recargoMoraOriginal`} render={({ field }) => (
          <FormItem>
            <FormControl><Input {...field} type="number" step="0.01" disabled={soloLectura} className={`${inputClase} w-28`} /></FormControl>
            {diasAtraso != null && <p className="text-xs text-muted-foreground">{diasAtraso} día(s) de atraso</p>}
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        {!soloLectura && <Button type="button" variant="outline" size="sm" onClick={onQuitar}>Quitar</Button>}
      </td>
    </tr>
  )
}

function TributoRow({ control, indice, soloLectura, onQuitar }: { control: Control<Valores>; indice: number; soloLectura: boolean; onQuitar: () => void }) {
  return (
    <tr className="border-b border-border last:border-0 align-top">
      <td className="py-2 pr-2">
        <FormField control={control} name={`tributos.${indice}.tipo`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={soloLectura} className={`${selectClase} min-w-48`}>
                {TIPOS_RETENCION_COBRO.map((t) => <option key={t} value={t}>{RETENCION_LABEL[t]}</option>)}
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`tributos.${indice}.importe`} render={({ field }) => (
          <FormItem><FormControl><Input {...field} type="number" step="0.01" disabled={soloLectura} className={`${inputClase} w-28`} /></FormControl><FormMessage /></FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        {!soloLectura && <Button type="button" variant="outline" size="sm" onClick={onQuitar}>Quitar</Button>}
      </td>
    </tr>
  )
}

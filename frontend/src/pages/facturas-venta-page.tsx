import { zodResolver } from "@hookform/resolvers/zod"
import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from "@tanstack/react-table"
import { useMemo, useState } from "react"
import { useFieldArray, useForm, useWatch, type Control } from "react-hook-form"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { descargarAdjunto, useAdjuntos, useEliminarAdjunto, useSubirAdjunto } from "@/hooks/use-adjunto"
import { useClientes } from "@/hooks/use-cliente"
import { useCuentasContables } from "@/hooks/use-cuenta-contable"
import {
  useAnularFacturaVenta,
  useConfirmarFacturaVenta,
  useCrearFacturaVenta,
  useEditarFacturaVenta,
  useEliminarFacturaVenta,
  useFacturasVenta,
} from "@/hooks/use-factura-venta"
import { useJurisdiccions } from "@/hooks/use-jurisdiccion"
import { useMonedas } from "@/hooks/use-monedas"
import { useProyectos } from "@/hooks/use-proyecto"
import type { EstadoFactura, FacturaVenta, TipoComprobante, TipoIngreso, TipoLineaFactura } from "@/types/factura-venta"
import { TIPOS_COMPROBANTE } from "@/types/factura-venta"

const ESTADOS: EstadoFactura[] = ["BORRADOR", "CONFIRMADO", "ANULADO"]
const ESTADO_LABEL: Record<EstadoFactura, string> = { BORRADOR: "Borrador", CONFIRMADO: "Confirmado", ANULADO: "Anulado" }
const ALICUOTAS = ["0", "2.5", "5", "10.5", "21", "27"]
const TIPOS_LINEA: TipoLineaFactura[] = ["GRAVADO", "NO_GRAVADO", "EXENTO"]

const esquemaLinea = z.object({
  descripcion: z.string().min(1, "Obligatoria"),
  tipo: z.string().min(1),
  importeNeto: z.string().min(1, "Obligatorio"),
  alicuotaIva: z.string().min(1, "Obligatoria"),
  tipoIngreso: z.string(),
  cuentaContableId: z.string().optional(),
})

const esquema = z.object({
  clienteId: z.string().min(1, "El cliente es obligatorio"),
  proyectoId: z.string().optional(),
  fecha: z.string().min(1, "La fecha es obligatoria"),
  fechaVencimiento: z.string().optional(),
  tipoComprobante: z.string().min(1),
  puntoVenta: z.string().optional(),
  numero: z.string().min(1, "El número es obligatorio"),
  jurisdiccionDestinoId: z.string().optional(),
  monedaId: z.string().min(1, "La moneda es obligatoria"),
  tipoCambio: z.string().min(1, "El tipo de cambio es obligatorio"),
  observaciones: z.string().optional(),
  lineas: z.array(esquemaLinea),
})

type Valores = z.infer<typeof esquema>
type LineaValores = Valores["lineas"][number]

const LINEA_VACIA: LineaValores = {
  descripcion: "", tipo: "GRAVADO", importeNeto: "", alicuotaIva: "21", tipoIngreso: "VENTA", cuentaContableId: "",
}

function facturaAValores(f: FacturaVenta): Valores {
  return {
    clienteId: f.clienteId.toString(),
    proyectoId: f.proyectoId?.toString() ?? "",
    fecha: f.fecha,
    fechaVencimiento: f.fechaVencimiento ?? "",
    tipoComprobante: f.tipoComprobante,
    puntoVenta: f.puntoVenta ?? "",
    numero: f.numero,
    jurisdiccionDestinoId: f.jurisdiccionDestinoId?.toString() ?? "",
    monedaId: f.monedaId.toString(),
    tipoCambio: f.tipoCambio.toString(),
    observaciones: f.observaciones ?? "",
    lineas: f.lineas.map((l) => ({
      descripcion: l.descripcion,
      tipo: l.tipo,
      importeNeto: l.importeNeto.toString(),
      alicuotaIva: l.alicuotaIva.toString(),
      tipoIngreso: l.tipoIngreso,
      cuentaContableId: l.cuentaContableId?.toString() ?? "",
    })),
  }
}

const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"
const inputClase = "h-8"

export function FacturasVentaPage() {
  const [page, setPage] = useState(0)
  const [texto, setTexto] = useState("")
  const [estadoFiltro, setEstadoFiltro] = useState<EstadoFactura | "">("")
  const [editando, setEditando] = useState<FacturaVenta | null>(null)
  const [mostrarForm, setMostrarForm] = useState(false)
  const [anulando, setAnulando] = useState<number | null>(null)
  const [motivoAnulacion, setMotivoAnulacion] = useState("")

  const query = useFacturasVenta({ texto, estado: estadoFiltro || undefined, page, size: 10 })
  const crear = useCrearFacturaVenta()
  const editar = useEditarFacturaVenta()
  const eliminar = useEliminarFacturaVenta()
  const confirmar = useConfirmarFacturaVenta()
  const anular = useAnularFacturaVenta()

  const clientes = useClientes({ activo: true, page: 0, size: 200 })
  const proyectos = useProyectos({ activo: true, page: 0, size: 200 })
  const jurisdicciones = useJurisdiccions({ page: 0, size: 100 })
  const monedas = useMonedas({ page: 0, size: 20 })
  const monedaArsId = monedas.data?.content?.find((m) => m.codigo === "ARS")?.id

  const form = useForm<Valores>({
    resolver: zodResolver(esquema),
    defaultValues: {
      clienteId: "", proyectoId: "", fecha: "", fechaVencimiento: "", tipoComprobante: "FACTURA_B",
      puntoVenta: "", numero: "", jurisdiccionDestinoId: "", monedaId: "", tipoCambio: "1",
      observaciones: "", lineas: [],
    },
  })
  const lineas = useFieldArray({ control: form.control, name: "lineas" })

  const adjuntos = useAdjuntos("FacturaVenta", editando?.id)
  const subirAdjunto = useSubirAdjunto()
  const eliminarAdjunto = useEliminarAdjunto()

  function nuevaFactura() {
    setEditando(null)
    form.reset({
      clienteId: "", proyectoId: "", fecha: "", fechaVencimiento: "", tipoComprobante: "FACTURA_B",
      puntoVenta: "", numero: "", jurisdiccionDestinoId: "", monedaId: monedaArsId ? monedaArsId.toString() : "",
      tipoCambio: "1", observaciones: "", lineas: [],
    })
    setMostrarForm(true)
  }

  function iniciarEdicion(f: FacturaVenta) {
    setEditando(f)
    form.reset(facturaAValores(f))
    setMostrarForm(true)
  }

  function cancelar() {
    setMostrarForm(false)
    setEditando(null)
  }

  function agregarLinea() {
    lineas.append({ ...LINEA_VACIA })
  }

  function onSubmit(valores: Valores) {
    const payload = {
      clienteId: Number(valores.clienteId),
      proyectoId: valores.proyectoId ? Number(valores.proyectoId) : undefined,
      fecha: valores.fecha,
      fechaVencimiento: valores.fechaVencimiento || undefined,
      tipoComprobante: valores.tipoComprobante as TipoComprobante,
      puntoVenta: valores.puntoVenta || undefined,
      numero: valores.numero,
      jurisdiccionDestinoId: valores.jurisdiccionDestinoId ? Number(valores.jurisdiccionDestinoId) : undefined,
      monedaId: Number(valores.monedaId),
      tipoCambio: Number(valores.tipoCambio),
      observaciones: valores.observaciones || undefined,
      lineas: valores.lineas.map((l) => ({
        descripcion: l.descripcion,
        tipo: l.tipo as TipoLineaFactura,
        importeNeto: Number(l.importeNeto),
        alicuotaIva: Number(l.alicuotaIva),
        tipoIngreso: l.tipoIngreso as TipoIngreso,
        cuentaContableId: l.cuentaContableId ? Number(l.cuentaContableId) : undefined,
      })),
    }
    if (editando) {
      editar.mutate({ id: editando.id, valores: payload }, { onSuccess: cancelar })
    } else {
      crear.mutate(payload, { onSuccess: cancelar })
    }
  }

  function confirmarAnulacion(id: number) {
    if (!motivoAnulacion.trim()) return
    anular.mutate({ id, motivo: motivoAnulacion }, {
      onSuccess: () => { setAnulando(null); setMotivoAnulacion("") },
    })
  }

  const lineasObservadas = useWatch({ control: form.control, name: "lineas" })
  const totales = useMemo(() => {
    let neto = 0, iva = 0
    for (const l of lineasObservadas ?? []) {
      const n = Number(l.importeNeto) || 0
      const a = Number(l.alicuotaIva) || 0
      neto += n
      iva += n * a / 100
    }
    return { neto, iva, total: neto + iva }
  }, [lineasObservadas])

  const columnas = useMemo<ColumnDef<FacturaVenta>[]>(
    () => [
      { header: "Fecha", accessorKey: "fecha" },
      { header: "Tipo", accessorKey: "tipoComprobante" },
      { header: "Número", accessorKey: "numero" },
      { header: "Cliente", accessorKey: "clienteNombre" },
      { header: "Total", accessorKey: "total", cell: (info) => (info.getValue() as number).toFixed(2) },
      { header: "Estado", accessorKey: "estado", cell: (info) => ESTADO_LABEL[info.getValue() as EstadoFactura] },
      { header: "N° Asiento", accessorKey: "asientoNumero", cell: (info) => info.getValue() ?? "—" },
      {
        header: "Acciones",
        id: "acciones",
        cell: ({ row }) => {
          const f = row.original
          if (anulando === f.id) {
            return (
              <div className="flex items-center gap-2">
                <Input autoFocus placeholder="Motivo…" value={motivoAnulacion} onChange={(e) => setMotivoAnulacion(e.target.value)} className={`${inputClase} w-40`} />
                <Button size="sm" disabled={anular.isPending || !motivoAnulacion.trim()} onClick={() => confirmarAnulacion(f.id)}>Confirmar</Button>
                <Button variant="outline" size="sm" onClick={() => { setAnulando(null); setMotivoAnulacion("") }}>Cancelar</Button>
              </div>
            )
          }
          if (f.estado === "ANULADO") return <span className="text-xs text-muted-foreground">Anulada</span>
          return (
            <div className="flex flex-wrap gap-2">
              <Button variant="outline" size="sm" onClick={() => iniciarEdicion(f)}>{f.estado === "BORRADOR" ? "Editar" : "Ver"}</Button>
              {f.estado === "BORRADOR" && (
                <>
                  <Button variant="outline" size="sm" disabled={confirmar.isPending} onClick={() => confirmar.mutate(f.id)}>Confirmar</Button>
                  <Button variant="outline" size="sm" disabled={eliminar.isPending} onClick={() => eliminar.mutate(f.id)}>Eliminar</Button>
                </>
              )}
              {f.estado === "CONFIRMADO" && (
                <Button variant="outline" size="sm" disabled={anular.isPending} onClick={() => setAnulando(f.id)}>Anular</Button>
              )}
            </div>
          )
        },
      },
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [confirmar.isPending, eliminar.isPending, anular.isPending, anulando, motivoAnulacion]
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
          <h1 className="text-lg font-semibold text-foreground">Facturas de venta</h1>
          <p className="text-sm text-muted-foreground">Genera automáticamente el asiento al confirmar (F4.1/F4.2).</p>
        </div>
        {!mostrarForm && <Button onClick={nuevaFactura}>Nueva factura</Button>}
      </div>

      {mostrarForm && (
        <Card>
          <CardHeader>
            <CardTitle>
              {editando ? `${soloLectura ? "Ver" : "Editar"} factura ${editando.numero}${editando.asientoNumero ? ` — Asiento N° ${editando.asientoNumero}` : ""}` : "Nueva factura (borrador)"}
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
                    <FormField control={form.control} name="proyectoId" render={({ field }) => (
                      <FormItem>
                        <FormLabel>Proyecto</FormLabel>
                        <FormControl>
                          <select {...field} disabled={proyectos.isLoading || soloLectura} className={selectClase}>
                            <option value="">Sin proyecto</option>
                            {proyectos.data?.content?.map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
                          </select>
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )} />
                    <FormField control={form.control} name="fecha" render={({ field }) => (
                      <FormItem><FormLabel>Fecha</FormLabel><FormControl><Input {...field} type="date" className={inputClase} /></FormControl><FormMessage /></FormItem>
                    )} />
                    <FormField control={form.control} name="fechaVencimiento" render={({ field }) => (
                      <FormItem><FormLabel>Vencimiento</FormLabel><FormControl><Input {...field} type="date" className={inputClase} /></FormControl><FormMessage /></FormItem>
                    )} />
                  </div>
                  <div className="grid gap-4 sm:grid-cols-4">
                    <FormField control={form.control} name="tipoComprobante" render={({ field }) => (
                      <FormItem>
                        <FormLabel>Tipo de comprobante</FormLabel>
                        <FormControl>
                          <select {...field} className={selectClase}>
                            {TIPOS_COMPROBANTE.map((t) => <option key={t} value={t}>{t}</option>)}
                          </select>
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )} />
                    <FormField control={form.control} name="puntoVenta" render={({ field }) => (
                      <FormItem><FormLabel>Punto de venta</FormLabel><FormControl><Input {...field} placeholder="0001" className={inputClase} /></FormControl><FormMessage /></FormItem>
                    )} />
                    <FormField control={form.control} name="numero" render={({ field }) => (
                      <FormItem><FormLabel>Número</FormLabel><FormControl><Input {...field} placeholder="00000123" className={inputClase} /></FormControl><FormMessage /></FormItem>
                    )} />
                    <FormField control={form.control} name="jurisdiccionDestinoId" render={({ field }) => (
                      <FormItem>
                        <FormLabel>Jurisdicción destino</FormLabel>
                        <FormControl>
                          <select {...field} disabled={jurisdicciones.isLoading} className={selectClase}>
                            <option value="">Sin especificar</option>
                            {jurisdicciones.data?.content?.map((j) => <option key={j.id} value={j.id.toString()}>{j.nombre}</option>)}
                          </select>
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )} />
                  </div>
                  <div className="grid gap-4 sm:grid-cols-3">
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
                    <FormField control={form.control} name="observaciones" render={({ field }) => (
                      <FormItem><FormLabel>Observaciones</FormLabel><FormControl><Input {...field} className={inputClase} /></FormControl><FormMessage /></FormItem>
                    )} />
                  </div>

                  <div className="overflow-x-auto">
                    <table className="w-full min-w-[1000px] text-left text-sm">
                      <thead className="text-muted-foreground">
                        <tr className="border-b border-border">
                          <th className="py-2 pr-2 font-medium">Descripción</th>
                          <th className="py-2 pr-2 font-medium">Tipo</th>
                          <th className="py-2 pr-2 font-medium">Importe neto</th>
                          <th className="py-2 pr-2 font-medium">Alícuota IVA</th>
                          <th className="py-2 pr-2 font-medium">Tipo de ingreso</th>
                          <th className="py-2 pr-2 font-medium">Cuenta (override)</th>
                          <th className="py-2 pr-2 font-medium"></th>
                        </tr>
                      </thead>
                      <tbody>
                        {lineas.fields.map((campo, indice) => (
                          <LineaFacturaRow key={campo.id} control={form.control} indice={indice} soloLectura={soloLectura} onQuitar={() => lineas.remove(indice)} />
                        ))}
                      </tbody>
                    </table>
                  </div>

                  <div className="flex items-center gap-4">
                    {!soloLectura && <Button type="button" variant="outline" size="sm" onClick={agregarLinea}>Agregar línea</Button>}
                    <div className="flex gap-4 text-sm">
                      <span>Neto: <strong>{totales.neto.toFixed(2)}</strong></span>
                      <span>IVA: <strong>{totales.iva.toFixed(2)}</strong></span>
                      <span>Total: <strong>{totales.total.toFixed(2)}</strong></span>
                    </div>
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

            {editando && (
              <div className="space-y-2 border-t border-border pt-4">
                <h3 className="text-sm font-medium">Adjuntos (comprobante, opcional)</h3>
                <ul className="space-y-1">
                  {adjuntos.data?.map((a) => (
                    <li key={a.id} className="flex items-center gap-2 text-sm">
                      <button type="button" className="text-primary hover:underline" onClick={() => descargarAdjunto(a.id, a.nombreArchivo)}>{a.nombreArchivo}</button>
                      <span className="text-xs text-muted-foreground">({(a.tamanio / 1024).toFixed(0)} KB)</span>
                      <Button variant="outline" size="sm" onClick={() => eliminarAdjunto.mutate(a.id)}>Quitar</Button>
                    </li>
                  ))}
                </ul>
                <input
                  type="file"
                  accept="application/pdf"
                  onChange={(e) => {
                    const archivo = e.target.files?.[0]
                    if (archivo && editando) subirAdjunto.mutate({ entidadTipo: "FacturaVenta", entidadId: editando.id, archivo })
                    e.target.value = ""
                  }}
                  className="text-sm"
                />
              </div>
            )}
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader><CardTitle>Listado</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="flex gap-2">
            <Input placeholder="Buscar por número o cliente…" value={texto} onChange={(e) => { setTexto(e.target.value); setPage(0) }} className="max-w-xs" />
            <select value={estadoFiltro} onChange={(e) => { setEstadoFiltro(e.target.value as EstadoFactura | ""); setPage(0) }} className={`${selectClase} max-w-40`}>
              <option value="">Todos los estados</option>
              {ESTADOS.map((e) => <option key={e} value={e}>{ESTADO_LABEL[e]}</option>)}
            </select>
          </div>
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

function LineaFacturaRow({ control, indice, soloLectura, onQuitar }: { control: Control<Valores>; indice: number; soloLectura: boolean; onQuitar: () => void }) {
  const cuentas = useCuentasContables({ activo: true, page: 0, size: 500 })
  const cuentasImputables = useMemo(() => (cuentas.data?.content ?? []).filter((c) => c.imputable), [cuentas.data])

  return (
    <tr className="border-b border-border last:border-0 align-top">
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.descripcion`} render={({ field }) => (
          <FormItem><FormControl><Input {...field} disabled={soloLectura} className={`${inputClase} min-w-48`} /></FormControl><FormMessage /></FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.tipo`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={soloLectura} className={`${selectClase} w-28`}>
                {TIPOS_LINEA.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.importeNeto`} render={({ field }) => (
          <FormItem><FormControl><Input {...field} type="number" step="0.01" disabled={soloLectura} className={`${inputClase} w-28`} /></FormControl><FormMessage /></FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.alicuotaIva`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={soloLectura} className={`${selectClase} w-20`}>
                {ALICUOTAS.map((a) => <option key={a} value={a}>{a}%</option>)}
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.tipoIngreso`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={soloLectura} className={`${selectClase} w-32`}>
                <option value="VENTA">Venta</option>
                <option value="OTRA_VENTA">Otra venta</option>
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.cuentaContableId`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={cuentas.isLoading || soloLectura} className={`${selectClase} min-w-48`}>
                <option value="">Automático (mapeo)</option>
                {cuentasImputables.map((c) => <option key={c.id} value={c.id.toString()}>{c.codigo} — {c.nombre}</option>)}
              </select>
            </FormControl>
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

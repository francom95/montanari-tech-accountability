import { zodResolver } from "@hookform/resolvers/zod"
import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from "@tanstack/react-table"
import { useMemo, useState } from "react"
import { useFieldArray, useForm, useWatch, type Control } from "react-hook-form"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useCurrentUser } from "@/hooks/use-auth"
import {
  useAnularAsiento,
  useAsientos,
  useConfirmarAsiento,
  useCrearAsiento,
  useDuplicarAsiento,
  useEditarAsiento,
  useEditarAsientoConfirmado,
  useEliminarAsiento,
} from "@/hooks/use-asiento"
import { useClientes } from "@/hooks/use-cliente"
import { useCuentasBancarias } from "@/hooks/use-cuenta-bancaria"
import { useCuentasContables } from "@/hooks/use-cuenta-contable"
import { useEtapas } from "@/hooks/use-etapa"
import { useMonedas } from "@/hooks/use-monedas"
import { useProveedores } from "@/hooks/use-proveedor"
import { useProyectos } from "@/hooks/use-proyecto"
import type { Asiento, EstadoAsiento } from "@/types/asiento"

const ESTADOS: EstadoAsiento[] = ["BORRADOR", "CONFIRMADO", "ANULADO"]
const ESTADO_LABEL: Record<EstadoAsiento, string> = { BORRADOR: "Borrador", CONFIRMADO: "Confirmado", ANULADO: "Anulado" }
const ORIGENES = [
  "MANUAL", "AJUSTE", "APERTURA", "IMPORTACION", "FACTURA_VENTA", "FACTURA_COMPRA",
  "COBRO", "PAGO", "LIQUIDACION_IVA", "LIQUIDACION_IIBB", "RESUMEN_TARJETA", "MOVIMIENTO_BANCARIO",
]

const esquemaLinea = z.object({
  id: z.number().nullable().optional(),
  generadaAuto: z.boolean().optional(),
  cuentaContableId: z.string().min(1, "Obligatoria"),
  debe: z.string(),
  haber: z.string(),
  monedaId: z.string().min(1, "Obligatoria"),
  tipoCambio: z.string().optional(),
  importeOriginal: z.string().optional(),
  leyenda: z.string().optional(),
  proyectoId: z.string().optional(),
  etapaId: z.string().optional(),
  clienteId: z.string().optional(),
  proveedorId: z.string().optional(),
  cuentaBancariaId: z.string().optional(),
})

const esquema = z.object({
  fecha: z.string().min(1, "La fecha es obligatoria"),
  descripcion: z.string().min(1, "La descripción es obligatoria").max(500),
  observaciones: z.string().optional(),
  lineas: z.array(esquemaLinea),
})

type Valores = z.infer<typeof esquema>
type LineaValores = Valores["lineas"][number]

const LINEA_VACIA: LineaValores = {
  id: null, generadaAuto: false,
  cuentaContableId: "", debe: "", haber: "", monedaId: "", tipoCambio: "", importeOriginal: "",
  leyenda: "", proyectoId: "", etapaId: "", clienteId: "", proveedorId: "", cuentaBancariaId: "",
}

function asientoAValores(a: Asiento): Valores {
  return {
    fecha: a.fecha,
    descripcion: a.descripcion,
    observaciones: a.observaciones ?? "",
    lineas: a.lineas.map((l) => ({
      id: l.id,
      generadaAuto: l.generadaAuto,
      cuentaContableId: l.cuentaContableId.toString(),
      debe: l.debe ? l.debe.toString() : "",
      haber: l.haber ? l.haber.toString() : "",
      monedaId: l.monedaId.toString(),
      tipoCambio: l.tipoCambio?.toString() ?? "",
      importeOriginal: l.importeOriginal?.toString() ?? "",
      leyenda: l.leyenda ?? "",
      proyectoId: l.proyectoId?.toString() ?? "",
      etapaId: l.etapaId?.toString() ?? "",
      clienteId: l.clienteId?.toString() ?? "",
      proveedorId: l.proveedorId?.toString() ?? "",
      cuentaBancariaId: l.cuentaBancariaId?.toString() ?? "",
    })),
  }
}

const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"
const inputClase = "h-8"

export function AsientosPage() {
  const [page, setPage] = useState(0)
  const [texto, setTexto] = useState("")
  const [estadoFiltro, setEstadoFiltro] = useState<EstadoAsiento | "">("")
  const [origenFiltro, setOrigenFiltro] = useState("")
  const [numeroFiltro, setNumeroFiltro] = useState("")
  const [fechaDesdeFiltro, setFechaDesdeFiltro] = useState("")
  const [fechaHastaFiltro, setFechaHastaFiltro] = useState("")
  const [cuentaFiltro, setCuentaFiltro] = useState("")
  const [importeFiltro, setImporteFiltro] = useState("")
  const [proyectoFiltro, setProyectoFiltro] = useState("")
  const [clienteFiltro, setClienteFiltro] = useState("")
  const [proveedorFiltro, setProveedorFiltro] = useState("")

  const [editando, setEditando] = useState<Asiento | null>(null)
  const [modoEdicion, setModoEdicion] = useState<"borrador" | "confirmado" | null>(null)
  const [mostrarForm, setMostrarForm] = useState(false)
  const [anulando, setAnulando] = useState<number | null>(null)
  const [motivoAnulacionInput, setMotivoAnulacionInput] = useState("")

  const usuario = useCurrentUser()
  const esAdmin = usuario.data?.rol === "ADMINISTRADOR"

  const query = useAsientos({
    texto, estado: estadoFiltro || undefined, origen: origenFiltro || undefined,
    numero: numeroFiltro ? Number(numeroFiltro) : undefined,
    fechaDesde: fechaDesdeFiltro || undefined, fechaHasta: fechaHastaFiltro || undefined,
    cuentaContableId: cuentaFiltro ? Number(cuentaFiltro) : undefined,
    importe: importeFiltro ? Number(importeFiltro) : undefined,
    proyectoId: proyectoFiltro ? Number(proyectoFiltro) : undefined,
    clienteId: clienteFiltro ? Number(clienteFiltro) : undefined,
    proveedorId: proveedorFiltro ? Number(proveedorFiltro) : undefined,
    page, size: 10,
  })
  const crear = useCrearAsiento()
  const editar = useEditarAsiento()
  const editarConfirmado = useEditarAsientoConfirmado()
  const confirmar = useConfirmarAsiento()
  const eliminar = useEliminarAsiento()
  const duplicar = useDuplicarAsiento()
  const anular = useAnularAsiento()

  const monedas = useMonedas({ page: 0, size: 20 })
  const monedaArsId = monedas.data?.content?.find((m) => m.codigo === "ARS")?.id
  const cuentasParaFiltro = useCuentasContables({ page: 0, size: 500 })
  const proyectosParaFiltro = useProyectos({ page: 0, size: 200 })
  const clientesParaFiltro = useClientes({ page: 0, size: 200 })
  const proveedoresParaFiltro = useProveedores({ page: 0, size: 200 })

  const form = useForm<Valores>({
    resolver: zodResolver(esquema),
    defaultValues: { fecha: "", descripcion: "", observaciones: "", lineas: [] },
  })
  const lineas = useFieldArray({ control: form.control, name: "lineas" })

  function nuevoAsiento() {
    setEditando(null)
    setModoEdicion("borrador")
    form.reset({ fecha: "", descripcion: "", observaciones: "", lineas: [] })
    setMostrarForm(true)
  }

  function iniciarEdicion(a: Asiento) {
    setEditando(a)
    setModoEdicion(a.estado === "CONFIRMADO" ? "confirmado" : "borrador")
    form.reset(asientoAValores(a))
    setMostrarForm(true)
  }

  function cancelar() {
    setMostrarForm(false)
    setEditando(null)
    setModoEdicion(null)
  }

  function agregarLinea() {
    lineas.append({ ...LINEA_VACIA, monedaId: monedaArsId ? monedaArsId.toString() : "" })
  }

  function onSubmit(valores: Valores) {
    const lineasComunes = valores.lineas.map((l) => ({
      cuentaContableId: Number(l.cuentaContableId),
      debe: l.debe ? Number(l.debe) : 0,
      haber: l.haber ? Number(l.haber) : 0,
      monedaId: Number(l.monedaId),
      tipoCambio: l.tipoCambio ? Number(l.tipoCambio) : undefined,
      importeOriginal: l.importeOriginal ? Number(l.importeOriginal) : undefined,
      leyenda: l.leyenda || undefined,
      proyectoId: l.proyectoId ? Number(l.proyectoId) : undefined,
      etapaId: l.etapaId ? Number(l.etapaId) : undefined,
      clienteId: l.clienteId ? Number(l.clienteId) : undefined,
      proveedorId: l.proveedorId ? Number(l.proveedorId) : undefined,
      cuentaBancariaId: l.cuentaBancariaId ? Number(l.cuentaBancariaId) : undefined,
    }))
    const base = { fecha: valores.fecha, descripcion: valores.descripcion, observaciones: valores.observaciones || undefined }

    if (modoEdicion === "confirmado" && editando) {
      editarConfirmado.mutate(
        { id: editando.id, valores: { ...base, lineas: valores.lineas.map((l, i) => ({ id: l.id ?? null, ...lineasComunes[i] })) } },
        { onSuccess: cancelar }
      )
    } else if (editando) {
      editar.mutate({ id: editando.id, valores: { ...base, lineas: lineasComunes } }, { onSuccess: cancelar })
    } else {
      crear.mutate({ ...base, lineas: lineasComunes }, { onSuccess: cancelar })
    }
  }

  function confirmarAnulacion(id: number) {
    if (!motivoAnulacionInput.trim()) return
    anular.mutate({ id, motivo: motivoAnulacionInput }, {
      onSuccess: () => { setAnulando(null); setMotivoAnulacionInput("") },
    })
  }

  const lineasObservadas = useWatch({ control: form.control, name: "lineas" })
  const totales = useMemo(() => {
    const debe = (lineasObservadas ?? []).reduce((acc, l) => acc + (Number(l.debe) || 0), 0)
    const haber = (lineasObservadas ?? []).reduce((acc, l) => acc + (Number(l.haber) || 0), 0)
    return { debe, haber, diferencia: debe - haber }
  }, [lineasObservadas])

  const columnas = useMemo<ColumnDef<Asiento>[]>(
    () => [
      { header: "N°", accessorKey: "numero", cell: (info) => info.getValue() ?? "—" },
      { header: "Fecha", accessorKey: "fecha" },
      { header: "Descripción", accessorKey: "descripcion" },
      { header: "Estado", accessorKey: "estado", cell: (info) => ESTADO_LABEL[info.getValue() as EstadoAsiento] },
      { header: "Origen", accessorKey: "origen" },
      { header: "Debe", accessorKey: "totalDebe", cell: (info) => (info.getValue() as number).toFixed(2) },
      { header: "Haber", accessorKey: "totalHaber", cell: (info) => (info.getValue() as number).toFixed(2) },
      {
        header: "Acciones",
        id: "acciones",
        cell: ({ row }) => {
          const a = row.original

          if (anulando === a.id) {
            return (
              <div className="flex items-center gap-2">
                <Input
                  autoFocus
                  placeholder="Motivo de la anulación…"
                  value={motivoAnulacionInput}
                  onChange={(e) => setMotivoAnulacionInput(e.target.value)}
                  className={`${inputClase} w-48`}
                />
                <Button size="sm" disabled={anular.isPending || !motivoAnulacionInput.trim()} onClick={() => confirmarAnulacion(a.id)}>
                  Confirmar
                </Button>
                <Button variant="outline" size="sm" onClick={() => { setAnulando(null); setMotivoAnulacionInput("") }}>Cancelar</Button>
              </div>
            )
          }

          if (a.estado === "ANULADO") {
            return <span className="text-xs text-muted-foreground" title={a.motivoAnulacion ?? undefined}>Anulado{a.motivoAnulacion ? `: ${a.motivoAnulacion}` : ""}</span>
          }

          return (
            <div className="flex flex-wrap gap-2">
              {a.estado === "BORRADOR" && (
                <>
                  <Button variant="outline" size="sm" onClick={() => iniciarEdicion(a)}>Editar</Button>
                  <Button variant="outline" size="sm" disabled={confirmar.isPending} onClick={() => confirmar.mutate(a.id)}>Confirmar</Button>
                  <Button variant="outline" size="sm" disabled={eliminar.isPending} onClick={() => eliminar.mutate(a.id)}>Eliminar</Button>
                </>
              )}
              {a.estado === "CONFIRMADO" && (
                <>
                  <Button variant="outline" size="sm" onClick={() => iniciarEdicion(a)}>Editar</Button>
                  <Button variant="outline" size="sm" disabled={anular.isPending} onClick={() => setAnulando(a.id)}>Anular</Button>
                </>
              )}
              <Button variant="outline" size="sm" disabled={duplicar.isPending} onClick={() => duplicar.mutate(a.id)}>Duplicar</Button>
            </div>
          )
        },
      },
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [confirmar.isPending, eliminar.isPending, duplicar.isPending, anular.isPending, anulando, motivoAnulacionInput]
  )

  const tabla = useReactTable({
    data: query.data?.content ?? [],
    columns: columnas,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
    pageCount: query.data?.totalPages ?? 0,
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-semibold text-foreground">Asientos contables</h1>
          <p className="text-sm text-muted-foreground">
            Carga manual, búsqueda avanzada, duplicación, edición de confirmados y anulación (F3.4/F3.5).
          </p>
        </div>
        {!mostrarForm && <Button onClick={nuevoAsiento}>Nuevo asiento</Button>}
      </div>

      {mostrarForm && (
        <Card>
          <CardHeader>
            <CardTitle>
              {modoEdicion === "confirmado"
                ? `Editar asiento confirmado N° ${editando?.numero ?? "s/n"}`
                : editando ? `Editar borrador N° ${editando.numero ?? "s/n"}` : "Nuevo asiento (borrador)"}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                {modoEdicion === "confirmado" && (
                  <p className="rounded-md bg-muted px-3 py-2 text-xs text-muted-foreground">
                    Este asiento ya está confirmado: el rebalanceo se vuelve a validar al guardar.
                    {!esAdmin && " Las líneas generadas automáticamente no se pueden modificar ni quitar."}
                  </p>
                )}
                <div className="grid gap-4 sm:grid-cols-3">
                  <FormField control={form.control} name="fecha" render={({ field }) => (
                    <FormItem><FormLabel>Fecha</FormLabel><FormControl><Input {...field} type="date" className={inputClase} /></FormControl><FormMessage /></FormItem>
                  )} />
                  <FormField control={form.control} name="descripcion" render={({ field }) => (
                    <FormItem className="sm:col-span-2"><FormLabel>Descripción</FormLabel><FormControl><Input {...field} className={inputClase} /></FormControl><FormMessage /></FormItem>
                  )} />
                </div>
                <FormField control={form.control} name="observaciones" render={({ field }) => (
                  <FormItem><FormLabel>Observaciones</FormLabel><FormControl><Input {...field} className={inputClase} /></FormControl><FormMessage /></FormItem>
                )} />

                <div className="overflow-x-auto">
                  <table className="w-full min-w-[1400px] text-left text-sm">
                    <thead className="text-muted-foreground">
                      <tr className="border-b border-border">
                        <th className="py-2 pr-2 font-medium">Cuenta</th>
                        <th className="py-2 pr-2 font-medium">Debe</th>
                        <th className="py-2 pr-2 font-medium">Haber</th>
                        <th className="py-2 pr-2 font-medium">Moneda</th>
                        <th className="py-2 pr-2 font-medium">TC</th>
                        <th className="py-2 pr-2 font-medium">Importe original</th>
                        <th className="py-2 pr-2 font-medium">Leyenda</th>
                        <th className="py-2 pr-2 font-medium">Proyecto</th>
                        <th className="py-2 pr-2 font-medium">Etapa</th>
                        <th className="py-2 pr-2 font-medium">Cliente</th>
                        <th className="py-2 pr-2 font-medium">Proveedor</th>
                        <th className="py-2 pr-2 font-medium">Destino de fondos</th>
                        <th className="py-2 pr-2 font-medium"></th>
                      </tr>
                    </thead>
                    <tbody>
                      {lineas.fields.map((campo, indice) => (
                        <LineaAsientoRow
                          key={campo.id}
                          control={form.control}
                          indice={indice}
                          soloLectura={Boolean(campo.generadaAuto) && !esAdmin}
                          onQuitar={() => lineas.remove(indice)}
                        />
                      ))}
                    </tbody>
                  </table>
                </div>

                <div className="flex items-center gap-4">
                  <Button type="button" variant="outline" size="sm" onClick={agregarLinea}>Agregar línea</Button>
                  <div className="flex gap-4 text-sm">
                    <span>Debe: <strong>{totales.debe.toFixed(2)}</strong></span>
                    <span>Haber: <strong>{totales.haber.toFixed(2)}</strong></span>
                    <span className={totales.diferencia !== 0 ? "font-semibold text-destructive" : "text-muted-foreground"}>
                      Diferencia: {totales.diferencia.toFixed(2)}
                    </span>
                  </div>
                </div>

                <div className="flex gap-2">
                  <Button type="submit" disabled={crear.isPending || editar.isPending || editarConfirmado.isPending}>
                    {modoEdicion === "confirmado" ? "Guardar cambios" : editando ? "Guardar borrador" : "Crear borrador"}
                  </Button>
                  <Button type="button" variant="outline" onClick={cancelar}>Cancelar</Button>
                </div>
              </form>
            </Form>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader><CardTitle>Listado</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap gap-2">
            <Input placeholder="Buscar por descripción o leyenda…" value={texto} onChange={(e) => { setTexto(e.target.value); setPage(0) }} className="max-w-xs" />
            <select value={estadoFiltro} onChange={(e) => { setEstadoFiltro(e.target.value as EstadoAsiento | ""); setPage(0) }} className={`${selectClase} max-w-40`}>
              <option value="">Todos los estados</option>
              {ESTADOS.map((e) => <option key={e} value={e}>{ESTADO_LABEL[e]}</option>)}
            </select>
            <select value={origenFiltro} onChange={(e) => { setOrigenFiltro(e.target.value); setPage(0) }} className={`${selectClase} max-w-44`}>
              <option value="">Todos los orígenes</option>
              {ORIGENES.map((o) => <option key={o} value={o}>{o}</option>)}
            </select>
            <Input placeholder="N° de asiento" value={numeroFiltro} onChange={(e) => { setNumeroFiltro(e.target.value); setPage(0) }} className="max-w-32" />
          </div>
          <div className="flex flex-wrap items-end gap-2">
            <label className="flex flex-col text-xs text-muted-foreground">
              Fecha desde
              <Input type="date" value={fechaDesdeFiltro} onChange={(e) => { setFechaDesdeFiltro(e.target.value); setPage(0) }} className={`${inputClase} w-36`} />
            </label>
            <label className="flex flex-col text-xs text-muted-foreground">
              Fecha hasta
              <Input type="date" value={fechaHastaFiltro} onChange={(e) => { setFechaHastaFiltro(e.target.value); setPage(0) }} className={`${inputClase} w-36`} />
            </label>
            <label className="flex flex-col text-xs text-muted-foreground">
              Importe
              <Input type="number" step="0.01" value={importeFiltro} onChange={(e) => { setImporteFiltro(e.target.value); setPage(0) }} className={`${inputClase} w-28`} />
            </label>
            <label className="flex flex-col text-xs text-muted-foreground">
              Cuenta
              <select value={cuentaFiltro} onChange={(e) => { setCuentaFiltro(e.target.value); setPage(0) }} className={`${selectClase} min-w-48`} disabled={cuentasParaFiltro.isLoading}>
                <option value="">Todas</option>
                {cuentasParaFiltro.data?.content?.map((c) => <option key={c.id} value={c.id.toString()}>{c.codigo} — {c.nombre}</option>)}
              </select>
            </label>
            <label className="flex flex-col text-xs text-muted-foreground">
              Proyecto
              <select value={proyectoFiltro} onChange={(e) => { setProyectoFiltro(e.target.value); setPage(0) }} className={`${selectClase} min-w-36`} disabled={proyectosParaFiltro.isLoading}>
                <option value="">Todos</option>
                {proyectosParaFiltro.data?.content?.map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
              </select>
            </label>
            <label className="flex flex-col text-xs text-muted-foreground">
              Cliente
              <select value={clienteFiltro} onChange={(e) => { setClienteFiltro(e.target.value); setPage(0) }} className={`${selectClase} min-w-36`} disabled={clientesParaFiltro.isLoading}>
                <option value="">Todos</option>
                {clientesParaFiltro.data?.content?.map((c) => <option key={c.id} value={c.id.toString()}>{c.nombre}</option>)}
              </select>
            </label>
            <label className="flex flex-col text-xs text-muted-foreground">
              Proveedor
              <select value={proveedorFiltro} onChange={(e) => { setProveedorFiltro(e.target.value); setPage(0) }} className={`${selectClase} min-w-36`} disabled={proveedoresParaFiltro.isLoading}>
                <option value="">Todos</option>
                {proveedoresParaFiltro.data?.content?.map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
              </select>
            </label>
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

function LineaAsientoRow({ control, indice, soloLectura, onQuitar }: { control: Control<Valores>; indice: number; soloLectura: boolean; onQuitar: () => void }) {
  const cuentas = useCuentasContables({ activo: true, page: 0, size: 500 })
  const cuentasImputables = useMemo(
    () => (cuentas.data?.content ?? []).filter((c) => c.imputable),
    [cuentas.data]
  )
  const monedas = useMonedas({ page: 0, size: 20 })
  const proyectos = useProyectos({ activo: true, page: 0, size: 200 })
  const clientes = useClientes({ activo: true, page: 0, size: 200 })
  const proveedores = useProveedores({ activo: true, page: 0, size: 200 })
  const cuentasBancarias = useCuentasBancarias({ activo: true, page: 0, size: 100 })

  const proyectoId = useWatch({ control, name: `lineas.${indice}.proyectoId` })
  const proyectoIdNumero = proyectoId ? Number(proyectoId) : undefined
  const etapas = useEtapas(proyectoIdNumero, { activo: true, page: 0, size: 200 })

  return (
    <tr className="border-b border-border last:border-0 align-top">
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.cuentaContableId`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={cuentas.isLoading || soloLectura} className={`${selectClase} min-w-56`}>
                <option value="">Seleccionar…</option>
                {cuentasImputables.map((c) => <option key={c.id} value={c.id.toString()}>{c.codigo} — {c.nombre}</option>)}
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.debe`} render={({ field }) => (
          <FormItem><FormControl><Input {...field} type="number" step="0.01" disabled={soloLectura} className={`${inputClase} w-28`} /></FormControl><FormMessage /></FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.haber`} render={({ field }) => (
          <FormItem><FormControl><Input {...field} type="number" step="0.01" disabled={soloLectura} className={`${inputClase} w-28`} /></FormControl><FormMessage /></FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.monedaId`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={monedas.isLoading || soloLectura} className={`${selectClase} w-20`}>
                <option value="">—</option>
                {monedas.data?.content?.map((m) => <option key={m.id} value={m.id.toString()}>{m.codigo}</option>)}
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.tipoCambio`} render={({ field }) => (
          <FormItem><FormControl><Input {...field} type="number" step="0.000001" placeholder="auto" disabled={soloLectura} className={`${inputClase} w-24`} /></FormControl><FormMessage /></FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.importeOriginal`} render={({ field }) => (
          <FormItem><FormControl><Input {...field} type="number" step="0.01" disabled={soloLectura} className={`${inputClase} w-28`} /></FormControl><FormMessage /></FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.leyenda`} render={({ field }) => (
          <FormItem><FormControl><Input {...field} disabled={soloLectura} className={`${inputClase} w-40`} /></FormControl><FormMessage /></FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.proyectoId`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={proyectos.isLoading || soloLectura} className={`${selectClase} min-w-40`}>
                <option value="">Sin proyecto</option>
                {proyectos.data?.content?.map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.etapaId`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={!proyectoIdNumero || etapas.isLoading || soloLectura} className={`${selectClase} min-w-36`}>
                <option value="">Sin etapa</option>
                {etapas.data?.content?.map((e) => <option key={e.id} value={e.id.toString()}>{e.nombre}</option>)}
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.clienteId`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={clientes.isLoading || soloLectura} className={`${selectClase} min-w-36`}>
                <option value="">Sin cliente</option>
                {clientes.data?.content?.map((c) => <option key={c.id} value={c.id.toString()}>{c.nombre}</option>)}
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.proveedorId`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={proveedores.isLoading || soloLectura} className={`${selectClase} min-w-36`}>
                <option value="">Sin proveedor</option>
                {proveedores.data?.content?.map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.cuentaBancariaId`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={cuentasBancarias.isLoading || soloLectura} className={`${selectClase} min-w-40`}>
                <option value="">Sin destino de fondos</option>
                {cuentasBancarias.data?.content?.map((c) => <option key={c.id} value={c.id.toString()}>{c.alias}</option>)}
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        {soloLectura ? (
          <span className="text-xs text-muted-foreground" title="Generada automáticamente: solo un administrador puede modificarla o quitarla">Automática</span>
        ) : (
          <Button type="button" variant="outline" size="sm" onClick={onQuitar}>Quitar</Button>
        )}
      </td>
    </tr>
  )
}

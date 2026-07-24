import { zodResolver } from "@hookform/resolvers/zod"
import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from "@tanstack/react-table"
import { useMemo, useState } from "react"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import {
  useCambiarEstadoCompromiso,
  useCompromisos,
  useCrearCompromiso,
  useEditarCompromiso,
  useEliminarCompromiso,
} from "@/hooks/use-compromiso"
import { useMonedas } from "@/hooks/use-monedas"
import { useProveedores } from "@/hooks/use-proveedor"
import { useProyectos } from "@/hooks/use-proyecto"
import {
  ESTADOS_COMPROMISO,
  TIPOS_COMPROMISO,
  type Compromiso,
  type EstadoCompromiso,
  type TipoCompromiso,
} from "@/types/compromiso"

const TIPO_LABEL: Record<TipoCompromiso, string> = {
  CUOTA_PLAN_DE_PAGOS: "Cuota de plan de pagos", VENCIMIENTO_IMPOSITIVO: "Vencimiento impositivo",
  IVA_DIFERIDO: "IVA diferido", IIBB: "IIBB", PAGO_A_PROVEEDOR: "Pago a proveedor", SUELDOS: "Sueldos",
  CARGAS_SOCIALES: "Cargas sociales", CONTADOR: "Contador", COMISION_BANCARIA: "Comisión bancaria",
  COMISION_POR_VENTA: "Comisión por venta", SUSCRIPCION: "Suscripción", TARJETA: "Tarjeta", OTRO_EGRESO: "Otro egreso",
}

const ESTADO_LABEL: Record<EstadoCompromiso, string> = { PENDIENTE: "Pendiente", RESUELTO: "Resuelto", CANCELADO: "Cancelado" }

const selectClase = "h-8 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"

const esquema = z.object({
  concepto: z.string().min(1, "El concepto es obligatorio").max(200),
  tipo: z.enum(TIPOS_COMPROMISO as [TipoCompromiso, ...TipoCompromiso[]]),
  fechaPrevista: z.string().min(1, "La fecha prevista es obligatoria"),
  importe: z.string().min(1, "El importe es obligatorio"),
  monedaId: z.string().min(1, "La moneda es obligatoria"),
  proveedorId: z.string().optional(),
  proyectoId: z.string().optional(),
  estado: z.enum(ESTADOS_COMPROMISO as [EstadoCompromiso, ...EstadoCompromiso[]]),
  observaciones: z.string().optional(),
  generarVencimiento: z.boolean(),
})

type Valores = z.infer<typeof esquema>

const VACIO: Valores = {
  concepto: "", tipo: "OTRO_EGRESO", fechaPrevista: "", importe: "", monedaId: "", proveedorId: "",
  proyectoId: "", estado: "PENDIENTE", observaciones: "", generarVencimiento: false,
}

export function CompromisosPage() {
  const [page, setPage] = useState(0)
  const [texto, setTexto] = useState("")
  const [editando, setEditando] = useState<Compromiso | null>(null)

  const query = useCompromisos({ texto, page, size: 10 })
  const monedasQuery = useMonedas({ page: 0, size: 20, activo: true })
  const proveedoresQuery = useProveedores({ page: 0, size: 200, activo: true })
  const proyectosQuery = useProyectos({ page: 0, size: 200, activo: true })
  const crear = useCrearCompromiso()
  const editar = useEditarCompromiso()
  const cambiarEstado = useCambiarEstadoCompromiso()
  const eliminar = useEliminarCompromiso()

  const form = useForm<Valores>({ resolver: zodResolver(esquema), defaultValues: VACIO })

  function iniciarEdicion(c: Compromiso) {
    setEditando(c)
    form.reset({
      concepto: c.concepto,
      tipo: c.tipo,
      fechaPrevista: c.fechaPrevista,
      importe: c.importe.toString(),
      monedaId: c.monedaId.toString(),
      proveedorId: c.proveedorId?.toString() ?? "",
      proyectoId: c.proyectoId?.toString() ?? "",
      estado: c.estado,
      observaciones: c.observaciones ?? "",
      generarVencimiento: false,
    })
  }

  function cancelarEdicion() {
    setEditando(null)
    form.reset(VACIO)
  }

  function onSubmit(valores: Valores) {
    const base = {
      concepto: valores.concepto,
      tipo: valores.tipo,
      fechaPrevista: valores.fechaPrevista,
      importe: Number(valores.importe),
      monedaId: Number(valores.monedaId),
      proveedorId: valores.proveedorId ? Number(valores.proveedorId) : undefined,
      proyectoId: valores.proyectoId ? Number(valores.proyectoId) : undefined,
      observaciones: valores.observaciones || undefined,
    }
    if (editando) {
      editar.mutate({ id: editando.id, valores: { ...base, estado: valores.estado } }, { onSuccess: cancelarEdicion })
    } else {
      crear.mutate({ ...base, generarVencimiento: valores.generarVencimiento }, { onSuccess: cancelarEdicion })
    }
  }

  const columnas = useMemo<ColumnDef<Compromiso>[]>(
    () => [
      { header: "Fecha prevista", accessorKey: "fechaPrevista" },
      { header: "Concepto", accessorKey: "concepto" },
      { header: "Tipo", accessorKey: "tipo", cell: (info) => TIPO_LABEL[info.getValue() as TipoCompromiso] },
      { header: "Importe", accessorKey: "importe", cell: (info) => `${(info.getValue() as number).toFixed(2)} ${info.row.original.monedaCodigo}` },
      { header: "Estado", accessorKey: "estado", cell: (info) => ESTADO_LABEL[info.getValue() as EstadoCompromiso] },
      { header: "Vencimiento", accessorKey: "vencimientoGeneradoId", cell: (info) => (info.getValue() ? `Generado (#${info.getValue()})` : "—") },
      {
        header: "Acciones",
        id: "acciones",
        cell: ({ row }) => {
          const c = row.original
          return (
            <div className="flex flex-wrap gap-2">
              <Button variant="outline" size="sm" onClick={() => iniciarEdicion(c)}>Editar</Button>
              <Button variant="outline" size="sm" disabled={cambiarEstado.isPending} onClick={() => cambiarEstado.mutate({ id: c.id, activo: c.activo })}>
                {c.activo ? "Desactivar" : "Activar"}
              </Button>
              <Button variant="outline" size="sm" disabled={eliminar.isPending} onClick={() => eliminar.mutate(c.id)}>Eliminar</Button>
            </div>
          )
        },
      },
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [cambiarEstado.isPending, eliminar.isPending]
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
      <div>
        <h1 className="text-lg font-semibold text-foreground">Presupuesto de pagos</h1>
        <p className="text-sm text-muted-foreground">Compromisos de pago futuro (F8.2) — alimentan el flujo de caja proyectado (F8.3) junto con los vencimientos del calendario (F8.1).</p>
      </div>

      <Card>
        <CardHeader><CardTitle>{editando ? `Editar compromiso #${editando.id}` : "Nuevo compromiso"}</CardTitle></CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="concepto" render={({ field }) => (
                  <FormItem><FormLabel>Concepto</FormLabel><FormControl><Input {...field} placeholder="Cuota AFIP julio" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="tipo" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Tipo</FormLabel>
                    <FormControl>
                      <select {...field} className={selectClase}>
                        {TIPOS_COMPROMISO.map((t) => <option key={t} value={t}>{TIPO_LABEL[t]}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="fechaPrevista" render={({ field }) => (
                  <FormItem><FormLabel>Fecha prevista</FormLabel><FormControl><Input {...field} type="date" /></FormControl><FormMessage /></FormItem>
                )} />
              </div>
              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="importe" render={({ field }) => (
                  <FormItem><FormLabel>Importe</FormLabel><FormControl><Input {...field} type="number" step="0.01" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="monedaId" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Moneda</FormLabel>
                    <FormControl>
                      <select {...field} disabled={monedasQuery.isLoading} className={selectClase}>
                        <option value="">Seleccionar…</option>
                        {monedasQuery.data?.content?.map((m) => <option key={m.id} value={m.id.toString()}>{m.codigo}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                {editando && (
                  <FormField control={form.control} name="estado" render={({ field }) => (
                    <FormItem>
                      <FormLabel>Estado</FormLabel>
                      <FormControl>
                        <select {...field} className={selectClase}>
                          {ESTADOS_COMPROMISO.map((e) => <option key={e} value={e}>{ESTADO_LABEL[e]}</option>)}
                        </select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )} />
                )}
              </div>
              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="proveedorId" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Proveedor (opcional)</FormLabel>
                    <FormControl>
                      <select {...field} disabled={proveedoresQuery.isLoading} className={selectClase}>
                        <option value="">—</option>
                        {proveedoresQuery.data?.content?.map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="proyectoId" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Proyecto (opcional)</FormLabel>
                    <FormControl>
                      <select {...field} disabled={proyectosQuery.isLoading} className={selectClase}>
                        <option value="">—</option>
                        {proyectosQuery.data?.content?.map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="observaciones" render={({ field }) => (
                  <FormItem><FormLabel>Observaciones</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                )} />
              </div>
              {!editando && (
                <FormField control={form.control} name="generarVencimiento" render={({ field }) => (
                  <FormItem className="flex flex-row items-center gap-2">
                    <FormControl>
                      <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                    </FormControl>
                    <FormLabel className="!mt-0">Generar su vencimiento en el calendario (F8.1)</FormLabel>
                  </FormItem>
                )} />
              )}
              <div className="flex gap-2">
                <Button type="submit" disabled={crear.isPending || editar.isPending}>{editando ? "Guardar" : "Crear"}</Button>
                {editando && <Button type="button" variant="outline" onClick={cancelarEdicion}>Cancelar</Button>}
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>Listado</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <Input placeholder="Buscar…" value={texto} onChange={(e) => { setTexto(e.target.value); setPage(0) }} className="max-w-xs" />
          {query.isLoading ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[800px] text-left text-sm">
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
              </div>
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

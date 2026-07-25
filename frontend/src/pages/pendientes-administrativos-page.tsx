import { zodResolver } from "@hookform/resolvers/zod"
import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from "@tanstack/react-table"
import { useMemo, useState } from "react"
import { useForm } from "react-hook-form"
import { Link, useSearchParams } from "react-router-dom"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useClientes } from "@/hooks/use-cliente"
import {
  useCambiarEstadoPendienteAdministrativo,
  useCrearPendienteAdministrativo,
  useEditarPendienteAdministrativo,
  useEliminarPendienteAdministrativo,
  usePendienteAdministrativo,
  usePendientesAdministrativos,
} from "@/hooks/use-pendiente-administrativo"
import { useProveedores } from "@/hooks/use-proveedor"
import { useProyectos } from "@/hooks/use-proyecto"
import { useUsuarios } from "@/hooks/use-usuario"
import {
  ESTADOS_PENDIENTE,
  PRIORIDADES_PENDIENTE,
  type EstadoPendiente,
  type PendienteAdministrativo,
  type PrioridadPendiente,
} from "@/types/pendiente-administrativo"

const ESTADO_LABEL: Record<EstadoPendiente, string> = {
  PENDIENTE: "Pendiente", EN_PROCESO: "En proceso", RESUELTO: "Resuelto", CANCELADO: "Cancelado", POSTERGADO: "Postergado",
}
const PRIORIDAD_LABEL: Record<PrioridadPendiente, string> = { ALTA: "Alta", MEDIA: "Media", BAJA: "Baja" }

const selectClase = "h-8 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"

const esquema = z.object({
  titulo: z.string().min(1, "El título es obligatorio").max(200),
  descripcion: z.string().optional(),
  fechaEstimadaResolucion: z.string().optional(),
  prioridad: z.enum(PRIORIDADES_PENDIENTE as [PrioridadPendiente, ...PrioridadPendiente[]]),
  estado: z.enum(ESTADOS_PENDIENTE as [EstadoPendiente, ...EstadoPendiente[]]),
  responsableId: z.string().optional(),
  categoria: z.string().optional(),
  proyectoId: z.string().optional(),
  clienteId: z.string().optional(),
  proveedorId: z.string().optional(),
  observaciones: z.string().optional(),
})

type Valores = z.infer<typeof esquema>

const VACIO: Valores = {
  titulo: "", descripcion: "", fechaEstimadaResolucion: "", prioridad: "MEDIA", estado: "PENDIENTE",
  responsableId: "", categoria: "", proyectoId: "", clienteId: "", proveedorId: "", observaciones: "",
}

export function PendientesAdministrativosPage() {
  const [page, setPage] = useState(0)
  const [texto, setTexto] = useState("")
  const [filtroEstado, setFiltroEstado] = useState<EstadoPendiente | "">("")
  const [filtroPrioridad, setFiltroPrioridad] = useState<PrioridadPendiente | "">("")
  const [filtroResponsableId, setFiltroResponsableId] = useState("")
  const [filtroCategoria, setFiltroCategoria] = useState("")
  const [editando, setEditando] = useState<PendienteAdministrativo | null>(null)

  const [searchParams] = useSearchParams()
  const idFiltro = searchParams.get("id") ? Number(searchParams.get("id")) : undefined
  const registroUnico = usePendienteAdministrativo(idFiltro)

  const query = usePendientesAdministrativos({
    texto, page, size: 10,
    estado: filtroEstado || undefined,
    prioridad: filtroPrioridad || undefined,
    responsableId: filtroResponsableId ? Number(filtroResponsableId) : undefined,
    categoria: filtroCategoria || undefined,
  })
  const usuariosQuery = useUsuarios({ page: 0, size: 200 })
  const proyectosQuery = useProyectos({ page: 0, size: 200, activo: true })
  const clientesQuery = useClientes({ page: 0, size: 200, activo: true })
  const proveedoresQuery = useProveedores({ page: 0, size: 200, activo: true })
  const crear = useCrearPendienteAdministrativo()
  const editar = useEditarPendienteAdministrativo()
  const cambiarEstado = useCambiarEstadoPendienteAdministrativo()
  const eliminar = useEliminarPendienteAdministrativo()

  const form = useForm<Valores>({ resolver: zodResolver(esquema), defaultValues: VACIO })

  function iniciarEdicion(p: PendienteAdministrativo) {
    setEditando(p)
    form.reset({
      titulo: p.titulo,
      descripcion: p.descripcion ?? "",
      fechaEstimadaResolucion: p.fechaEstimadaResolucion ?? "",
      prioridad: p.prioridad,
      estado: p.estado,
      responsableId: p.responsableId?.toString() ?? "",
      categoria: p.categoria ?? "",
      proyectoId: p.proyectoId?.toString() ?? "",
      clienteId: p.clienteId?.toString() ?? "",
      proveedorId: p.proveedorId?.toString() ?? "",
      observaciones: p.observaciones ?? "",
    })
  }

  function cancelarEdicion() {
    setEditando(null)
    form.reset(VACIO)
  }

  function onSubmit(valores: Valores) {
    const base = {
      titulo: valores.titulo,
      descripcion: valores.descripcion || undefined,
      fechaEstimadaResolucion: valores.fechaEstimadaResolucion || undefined,
      prioridad: valores.prioridad,
      responsableId: valores.responsableId ? Number(valores.responsableId) : undefined,
      categoria: valores.categoria || undefined,
      proyectoId: valores.proyectoId ? Number(valores.proyectoId) : undefined,
      clienteId: valores.clienteId ? Number(valores.clienteId) : undefined,
      proveedorId: valores.proveedorId ? Number(valores.proveedorId) : undefined,
      observaciones: valores.observaciones || undefined,
    }
    if (editando) {
      editar.mutate({ id: editando.id, valores: { ...base, estado: valores.estado } }, { onSuccess: cancelarEdicion })
    } else {
      crear.mutate(base, { onSuccess: cancelarEdicion })
    }
  }

  const columnas = useMemo<ColumnDef<PendienteAdministrativo>[]>(
    () => [
      { header: "Título", accessorKey: "titulo" },
      { header: "Prioridad", accessorKey: "prioridad", cell: (info) => PRIORIDAD_LABEL[info.getValue() as PrioridadPendiente] },
      { header: "Estado", accessorKey: "estado", cell: (info) => ESTADO_LABEL[info.getValue() as EstadoPendiente] },
      { header: "Fecha estimada", accessorKey: "fechaEstimadaResolucion", cell: (info) => info.getValue() ?? "—" },
      { header: "Responsable", accessorKey: "responsableNombre", cell: (info) => info.getValue() ?? "—" },
      { header: "Categoría", accessorKey: "categoria", cell: (info) => info.getValue() ?? "—" },
      {
        header: "Acciones",
        id: "acciones",
        cell: ({ row }) => {
          const p = row.original
          return (
            <div className="flex flex-wrap gap-2">
              <Button variant="outline" size="sm" onClick={() => iniciarEdicion(p)}>Editar</Button>
              <Button variant="outline" size="sm" disabled={cambiarEstado.isPending} onClick={() => cambiarEstado.mutate({ id: p.id, activo: p.activo })}>
                {p.activo ? "Desactivar" : "Activar"}
              </Button>
              <Button variant="outline" size="sm" disabled={eliminar.isPending} onClick={() => eliminar.mutate(p.id)}>Eliminar</Button>
            </div>
          )
        },
      },
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [cambiarEstado.isPending, eliminar.isPending]
  )

  const filas = idFiltro !== undefined ? (registroUnico.data ? [registroUnico.data] : []) : (query.data?.content ?? [])

  const tabla = useReactTable({
    data: filas,
    columns: columnas,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
    pageCount: query.data?.totalPages ?? 0,
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-lg font-semibold text-foreground">Pendientes administrativos</h1>
        <p className="text-sm text-muted-foreground">Recordatorios, controles manuales, revisiones del contador y ajustes fuera del flujo contable formal (F8.5).</p>
      </div>

      <Card>
        <CardHeader><CardTitle>{editando ? `Editar ${editando.titulo}` : "Nuevo pendiente"}</CardTitle></CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="titulo" render={({ field }) => (
                  <FormItem><FormLabel>Título</FormLabel><FormControl><Input {...field} placeholder="Pedir factura del proveedor X" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="prioridad" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Prioridad</FormLabel>
                    <FormControl>
                      <select {...field} className={selectClase}>
                        {PRIORIDADES_PENDIENTE.map((v) => <option key={v} value={v}>{PRIORIDAD_LABEL[v]}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="fechaEstimadaResolucion" render={({ field }) => (
                  <FormItem><FormLabel>Fecha estimada de resolución</FormLabel><FormControl><Input {...field} type="date" /></FormControl><FormMessage /></FormItem>
                )} />
              </div>
              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="responsableId" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Responsable (opcional)</FormLabel>
                    <FormControl>
                      <select {...field} disabled={usuariosQuery.isLoading} className={selectClase}>
                        <option value="">—</option>
                        {usuariosQuery.data?.content?.map((u) => <option key={u.id} value={u.id.toString()}>{u.nombre}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="categoria" render={({ field }) => (
                  <FormItem><FormLabel>Categoría (opcional)</FormLabel><FormControl><Input {...field} placeholder="Bancos, Impuestos, Facturación…" /></FormControl><FormMessage /></FormItem>
                )} />
                {editando && (
                  <FormField control={form.control} name="estado" render={({ field }) => (
                    <FormItem>
                      <FormLabel>Estado</FormLabel>
                      <FormControl>
                        <select {...field} className={selectClase}>
                          {ESTADOS_PENDIENTE.map((e) => <option key={e} value={e}>{ESTADO_LABEL[e]}</option>)}
                        </select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )} />
                )}
              </div>
              <div className="grid gap-4 sm:grid-cols-3">
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
                <FormField control={form.control} name="clienteId" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Cliente (opcional)</FormLabel>
                    <FormControl>
                      <select {...field} disabled={clientesQuery.isLoading} className={selectClase}>
                        <option value="">—</option>
                        {clientesQuery.data?.content?.map((c) => <option key={c.id} value={c.id.toString()}>{c.nombre}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
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
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <FormField control={form.control} name="descripcion" render={({ field }) => (
                  <FormItem><FormLabel>Descripción</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="observaciones" render={({ field }) => (
                  <FormItem><FormLabel>Observaciones</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                )} />
              </div>
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
          {idFiltro !== undefined ? (
            <p className="text-sm text-muted-foreground">
              Mostrando un único resultado (desde la búsqueda global) — <Link to="/pendientes" className="hover:underline">Ver todos</Link>
            </p>
          ) : (
            <div className="flex flex-wrap gap-2">
              <Input placeholder="Buscar por título…" value={texto} onChange={(e) => { setTexto(e.target.value); setPage(0) }} className="max-w-xs" />
              <select value={filtroEstado} onChange={(e) => { setFiltroEstado(e.target.value as EstadoPendiente | ""); setPage(0) }} className={`${selectClase} w-40`}>
                <option value="">Todos los estados</option>
                {ESTADOS_PENDIENTE.map((e) => <option key={e} value={e}>{ESTADO_LABEL[e]}</option>)}
              </select>
              <select value={filtroPrioridad} onChange={(e) => { setFiltroPrioridad(e.target.value as PrioridadPendiente | ""); setPage(0) }} className={`${selectClase} w-36`}>
                <option value="">Todas las prioridades</option>
                {PRIORIDADES_PENDIENTE.map((v) => <option key={v} value={v}>{PRIORIDAD_LABEL[v]}</option>)}
              </select>
              <select value={filtroResponsableId} onChange={(e) => { setFiltroResponsableId(e.target.value); setPage(0) }} className={`${selectClase} w-40`}>
                <option value="">Todos los responsables</option>
                {usuariosQuery.data?.content?.map((u) => <option key={u.id} value={u.id.toString()}>{u.nombre}</option>)}
              </select>
              <Input placeholder="Categoría…" value={filtroCategoria} onChange={(e) => { setFiltroCategoria(e.target.value); setPage(0) }} className="max-w-40" />
            </div>
          )}
          {(idFiltro !== undefined ? registroUnico.isLoading : query.isLoading) ? (
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
                    {tabla.getRowModel().rows.length === 0 && (
                      <tr><td colSpan={7} className="py-4 text-center text-sm text-muted-foreground">Sin pendientes para los filtros seleccionados.</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
              {idFiltro === undefined && (
                <div className="flex items-center gap-2">
                  <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}>Anterior</Button>
                  <span className="text-sm text-muted-foreground">Página {page + 1} de {query.data?.totalPages || 1}</span>
                  <Button variant="outline" size="sm" disabled={page + 1 >= (query.data?.totalPages ?? 1)} onClick={() => setPage((p) => p + 1)}>Siguiente</Button>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

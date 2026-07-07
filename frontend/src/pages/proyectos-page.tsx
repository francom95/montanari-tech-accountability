import { zodResolver } from "@hookform/resolvers/zod"
import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from "@tanstack/react-table"
import { useMemo, useState } from "react"
import { useForm } from "react-hook-form"
import { Link } from "react-router-dom"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useClientes } from "@/hooks/use-cliente"
import { useMonedas } from "@/hooks/use-monedas"
import {
  useCambiarEstadoProyecto,
  useCrearProyecto,
  useEliminarProyecto,
  useProyectos,
} from "@/hooks/use-proyecto"
import type { Proyecto } from "@/types/proyecto"

const esquema = z.object({
  nombre: z.string().min(1, "El nombre es obligatorio").max(160),
  clienteId: z.string().min(1, "El cliente es obligatorio"),
  monedaId: z.string().min(1, "La moneda es obligatoria"),
  montoTotal: z.string().min(1, "El monto total es obligatorio"),
  pais: z.string().optional(),
  tipoProyecto: z.string().optional(),
})

type Valores = z.infer<typeof esquema>

export function ProyectosPage() {
  const [page, setPage] = useState(0)
  const [texto, setTexto] = useState("")

  const query = useProyectos({ texto, page, size: 10 })
  const clientes = useClientes({ page: 0, size: 100 })
  const monedas = useMonedas({ page: 0, size: 100 })
  const crear = useCrearProyecto()
  const cambiarEstado = useCambiarEstadoProyecto()
  const eliminar = useEliminarProyecto()

  const form = useForm<Valores>({
    resolver: zodResolver(esquema),
    defaultValues: { nombre: "", clienteId: "", monedaId: "", montoTotal: "", pais: "", tipoProyecto: "" },
  })

  function onSubmit(valores: Valores) {
    crear.mutate(
      {
        nombre: valores.nombre,
        clienteId: Number(valores.clienteId),
        monedaId: Number(valores.monedaId),
        montoTotal: Number(valores.montoTotal),
        pais: valores.pais || undefined,
        tipoProyecto: valores.tipoProyecto || undefined,
      },
      { onSuccess: () => form.reset({ nombre: "", clienteId: "", monedaId: "", montoTotal: "", pais: "", tipoProyecto: "" }) }
    )
  }

  const columnas = useMemo<ColumnDef<Proyecto>[]>(
    () => [
      { header: "Nombre", accessorKey: "nombre" },
      { header: "Cliente", accessorKey: "clienteNombre" },
      { header: "Estado", accessorKey: "estado" },
      { header: "Moneda", accessorKey: "monedaCodigo" },
      { header: "Monto total", accessorKey: "montoTotal" },
      { header: "Activo", accessorKey: "activo", cell: (info) => (info.getValue() ? "Activo" : "Inactivo") },
      {
        header: "Acciones",
        id: "acciones",
        cell: ({ row }) => {
          const p = row.original
          return (
            <div className="flex gap-2">
              <Link to={`/proyectos/${p.id}`}>
                <Button variant="outline" size="sm">Abrir</Button>
              </Link>
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
        <h1 className="text-lg font-semibold text-foreground">Proyectos</h1>
        <p className="text-sm text-muted-foreground">
          Alta rápida de proyectos (F2.5). Cuotas, etapas y el resto de la ficha se gestionan al abrir un proyecto.
        </p>
      </div>

      <Card>
        <CardHeader><CardTitle>Nuevo proyecto</CardTitle></CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="nombre" render={({ field }) => (
                  <FormItem><FormLabel>Nombre</FormLabel><FormControl><Input {...field} placeholder="Sitio web Acme" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="clienteId" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Cliente</FormLabel>
                    <FormControl>
                      <select {...field} disabled={clientes.isLoading} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        <option value="">Seleccionar</option>
                        {clientes.data?.content?.map((c) => (
                          <option key={c.id} value={c.id.toString()}>{c.nombre}</option>
                        ))}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="monedaId" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Moneda</FormLabel>
                    <FormControl>
                      <select {...field} disabled={monedas.isLoading} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        <option value="">Seleccionar</option>
                        {monedas.data?.content?.map((m) => (
                          <option key={m.id} value={m.id.toString()}>{m.codigo} - {m.nombre}</option>
                        ))}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
              </div>

              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="montoTotal" render={({ field }) => (
                  <FormItem><FormLabel>Monto total</FormLabel><FormControl><Input {...field} type="number" step="0.01" placeholder="10000" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="pais" render={({ field }) => (
                  <FormItem><FormLabel>País</FormLabel><FormControl><Input {...field} placeholder="Argentina" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="tipoProyecto" render={({ field }) => (
                  <FormItem><FormLabel>Tipo de proyecto</FormLabel><FormControl><Input {...field} placeholder="Desarrollo a medida" /></FormControl><FormMessage /></FormItem>
                )} />
              </div>

              <Button type="submit" disabled={crear.isPending}>Crear</Button>
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
                <span className="text-sm text-muted-foreground">{page + 1} de {tabla.getPageCount()}</span>
                <Button variant="outline" size="sm" disabled={page >= (tabla.getPageCount() || 1) - 1} onClick={() => setPage((p) => p + 1)}>Siguiente</Button>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

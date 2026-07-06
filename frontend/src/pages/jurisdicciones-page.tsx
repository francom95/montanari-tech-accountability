import { zodResolver } from "@hookform/resolvers/zod"
import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from "@tanstack/react-table"
import { useMemo, useState } from "react"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import {
  useCambiarEstadoJurisdiccion,
  useCrearJurisdiccion,
  useEditarJurisdiccion,
  useEliminarJurisdiccion,
  useJurisdiccions,
} from "@/hooks/use-jurisdiccion"
import type { Jurisdiccion } from "@/types/jurisdiccion"

const esquema = z.object({
  nombre: z.string().min(1, "El nombre es obligatorio").max(80),
  codigo: z.string().min(1, "El código es obligatorio").max(20),
  alicuotaIIBB: z.string().min(1, "La alícuota es obligatoria"),
})

type Valores = z.infer<typeof esquema>

export function JurisdiccionesPage() {
  const [page, setPage] = useState(0)
  const [texto, setTexto] = useState("")
  const [editando, setEditando] = useState<Jurisdiccion | null>(null)

  const query = useJurisdiccions({ texto, page, size: 10 })
  const crear = useCrearJurisdiccion()
  const editar = useEditarJurisdiccion()
  const cambiarEstado = useCambiarEstadoJurisdiccion()
  const eliminar = useEliminarJurisdiccion()

  const form = useForm<Valores>({ resolver: zodResolver(esquema), defaultValues: { nombre: "", codigo: "", alicuotaIIBB: "" } })

  function iniciarEdicion(e: Jurisdiccion) {
    setEditando(e)
    form.reset({ nombre: e.nombre, codigo: e.codigo, alicuotaIIBB: e.alicuotaIIBB })
  }

  function cancelarEdicion() {
    setEditando(null)
    form.reset({ nombre: "", codigo: "", alicuotaIIBB: "" })
  }

  function onSubmit(valores: Valores) {
    if (editando) {
      editar.mutate({ id: editando.id, valores: { nombre: valores.nombre, alicuotaIIBB: valores.alicuotaIIBB } }, { onSuccess: cancelarEdicion })
    } else {
      crear.mutate(valores, { onSuccess: () => form.reset({ nombre: "", codigo: "", alicuotaIIBB: "" }) })
    }
  }

  const columnas = useMemo<ColumnDef<Jurisdiccion>[]>(
    () => [
      { header: "Código", accessorKey: "codigo" },
      { header: "Nombre", accessorKey: "nombre" },
      { header: "Alícuota IIBB", accessorKey: "alicuotaIIBB" },
      { header: "Estado", accessorKey: "activo", cell: (info) => (info.getValue() ? "Activa" : "Inactiva") },
      {
        header: "Acciones",
        id: "acciones",
        cell: ({ row }) => {
          const e = row.original
          return (
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={() => iniciarEdicion(e)}>Editar</Button>
              <Button variant="outline" size="sm" disabled={cambiarEstado.isPending} onClick={() => cambiarEstado.mutate({ id: e.id, activo: e.activo })}>
                {e.activo ? "Desactivar" : "Activar"}
              </Button>
              <Button variant="outline" size="sm" disabled={eliminar.isPending} onClick={() => eliminar.mutate(e.id)}>Eliminar</Button>
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
        <h1 className="text-lg font-semibold text-foreground">Jurisdicciones</h1>
        <p className="text-sm text-muted-foreground">Molde PL-1/PL-2 replicado de Moneda (F2.1).</p>
      </div>

      <Card>
        <CardHeader><CardTitle>{editando ? `Editar ${editando.codigo}` : "Nueva jurisdicción"}</CardTitle></CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-4">
              <FormField control={form.control} name="codigo" render={({ field }) => (
                <FormItem><FormLabel>Código</FormLabel><FormControl><Input {...field} disabled={!!editando} placeholder="BA" /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="nombre" render={({ field }) => (
                <FormItem><FormLabel>Nombre</FormLabel><FormControl><Input {...field} placeholder="Buenos Aires" /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="alicuotaIIBB" render={({ field }) => (
                <FormItem><FormLabel>Alícuota IIBB (%)</FormLabel><FormControl><Input {...field} placeholder="3.50" /></FormControl><FormMessage /></FormItem>
              )} />
              <div className="flex items-end gap-2">
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

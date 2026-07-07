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
  useCambiarEstadoComisionista,
  useComisionistas,
  useCrearComisionista,
  useEditarComisionista,
  useEliminarComisionista,
} from "@/hooks/use-comisionista"
import type { Comisionista } from "@/types/comisionista"

const esquema = z.object({
  nombre: z.string().min(1, "El nombre es obligatorio").max(120),
  cuit: z.union([z.string().regex(/^\d{2}-\d{8}-\d{1}$/, "CUIT debe tener formato XX-XXXXXXXX-X"), z.literal("")]).optional(),
  contacto: z.string().max(100).optional(),
  email: z.union([z.string().email("Email inválido").max(100), z.literal("")]).optional(),
  telefono: z.string().max(20).optional(),
})

type Valores = z.infer<typeof esquema>

const VALORES_INICIALES: Valores = { nombre: "", cuit: "", contacto: "", email: "", telefono: "" }

export function ComisionistasPage() {
  const [page, setPage] = useState(0)
  const [texto, setTexto] = useState("")
  const [editando, setEditando] = useState<Comisionista | null>(null)

  const query = useComisionistas({ texto, page, size: 10 })
  const crear = useCrearComisionista()
  const editar = useEditarComisionista()
  const cambiarEstado = useCambiarEstadoComisionista()
  const eliminar = useEliminarComisionista()

  const form = useForm<Valores>({ resolver: zodResolver(esquema), defaultValues: VALORES_INICIALES })

  function iniciarEdicion(c: Comisionista) {
    setEditando(c)
    form.reset({
      nombre: c.nombre,
      cuit: c.cuit || "",
      contacto: c.contacto || "",
      email: c.email || "",
      telefono: c.telefono || "",
    })
  }

  function cancelarEdicion() {
    setEditando(null)
    form.reset(VALORES_INICIALES)
  }

  function onSubmit(valores: Valores) {
    const input = {
      nombre: valores.nombre,
      cuit: valores.cuit || undefined,
      contacto: valores.contacto || undefined,
      email: valores.email || undefined,
      telefono: valores.telefono || undefined,
    }
    if (editando) {
      editar.mutate({ id: editando.id, valores: input }, { onSuccess: cancelarEdicion })
    } else {
      crear.mutate(input, { onSuccess: () => form.reset(VALORES_INICIALES) })
    }
  }

  const columnas = useMemo<ColumnDef<Comisionista>[]>(
    () => [
      { header: "Nombre", accessorKey: "nombre" },
      { header: "CUIT", accessorKey: "cuit", cell: (info) => info.getValue() || "-" },
      { header: "Email", accessorKey: "email" },
      { header: "Estado", accessorKey: "activo", cell: (info) => (info.getValue() ? "Activo" : "Inactivo") },
      {
        header: "Acciones",
        id: "acciones",
        cell: ({ row }) => {
          const c = row.original
          return (
            <div className="flex gap-2">
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
        <h1 className="text-lg font-semibold text-foreground">Comisionistas</h1>
        <p className="text-sm text-muted-foreground">Molde PL-1/PL-2 (F2.7). El vínculo con proyectos y el % de comisión se cargan desde la ficha del proyecto.</p>
      </div>

      <Card>
        <CardHeader><CardTitle>{editando ? `Editar ${editando.nombre}` : "Nuevo comisionista"}</CardTitle></CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="nombre" render={({ field }) => (
                  <FormItem><FormLabel>Nombre</FormLabel><FormControl><Input {...field} placeholder="Cristian Pittaluga" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="cuit" render={({ field }) => (
                  <FormItem><FormLabel>CUIT (opcional)</FormLabel><FormControl><Input {...field} placeholder="20-12345678-9" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="contacto" render={({ field }) => (
                  <FormItem><FormLabel>Contacto</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                )} />
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <FormField control={form.control} name="email" render={({ field }) => (
                  <FormItem><FormLabel>Email</FormLabel><FormControl><Input {...field} type="email" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="telefono" render={({ field }) => (
                  <FormItem><FormLabel>Teléfono</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                )} />
              </div>
              <div className="flex items-center gap-2">
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

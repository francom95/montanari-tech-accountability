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
  useCambiarEstadoCliente,
  useCrearCliente,
  useEditarCliente,
  useEliminarCliente,
  useClientes,
  descargarClientesExcel,
  descargarClientesPdf,
} from "@/hooks/use-cliente"
import { useCuentasContables } from "@/hooks/use-cuenta-contable"
import { useJurisdiccions } from "@/hooks/use-jurisdiccion"
import type { Cliente } from "@/types/cliente"

const esquema = z.object({
  nombre: z.string().min(1, "El nombre es obligatorio").max(120),
  cuit: z.string().min(1, "El CUIT es obligatorio").regex(/^\d{2}-\d{8}-\d{1}$/, "CUIT debe tener formato XX-XXXXXXXX-X"),
  jurisdiccionId: z.string().min(1, "La jurisdicción es obligatoria"),
  contacto: z.string().max(100).optional(),
  email: z.union([z.string().email("Email inválido").max(100), z.literal("")]).optional(),
  telefono: z.string().max(20).optional(),
  cuentaCxcId: z.string().optional(),
})

type Valores = z.infer<typeof esquema>

export function ClientesPage() {
  const [page, setPage] = useState(0)
  const [texto, setTexto] = useState("")
  const [editando, setEditando] = useState<Cliente | null>(null)
  const [descargando, setDescargando] = useState<"excel" | "pdf" | null>(null)

  const query = useClientes({ texto, page, size: 10 })
  const jurisdicciones = useJurisdiccions({ page: 0, size: 100 })
  const cuentasContables = useCuentasContables({ activo: true, page: 0, size: 500 })
  const cuentasImputables = useMemo(() => (cuentasContables.data?.content ?? []).filter((c) => c.imputable), [cuentasContables.data])
  const crear = useCrearCliente()
  const editar = useEditarCliente()
  const cambiarEstado = useCambiarEstadoCliente()
  const eliminar = useEliminarCliente()

  const VACIO: Valores = { nombre: "", cuit: "", jurisdiccionId: "", contacto: "", email: "", telefono: "", cuentaCxcId: "" }

  const form = useForm<Valores>({
    resolver: zodResolver(esquema),
    defaultValues: VACIO,
  })

  function iniciarEdicion(e: Cliente) {
    setEditando(e)
    form.reset({
      nombre: e.nombre,
      cuit: e.cuit,
      jurisdiccionId: e.jurisdiccionId.toString(),
      contacto: e.contacto || "",
      email: e.email || "",
      telefono: e.telefono || "",
      cuentaCxcId: e.cuentaCxcId?.toString() || "",
    })
  }

  function cancelarEdicion() {
    setEditando(null)
    form.reset(VACIO)
  }

  async function exportar(formato: "excel" | "pdf") {
    setDescargando(formato)
    try {
      if (formato === "excel") await descargarClientesExcel({ texto: texto || undefined })
      else await descargarClientesPdf({ texto: texto || undefined })
    } finally {
      setDescargando(null)
    }
  }

  function onSubmit(valores: Valores) {
    if (editando) {
      editar.mutate({
        id: editando.id,
        valores: {
          nombre: valores.nombre,
          jurisdiccionId: Number(valores.jurisdiccionId),
          contacto: valores.contacto,
          email: valores.email,
          telefono: valores.telefono,
          cuentaCxcId: valores.cuentaCxcId ? Number(valores.cuentaCxcId) : undefined,
        }
      }, { onSuccess: cancelarEdicion })
    } else {
      crear.mutate({
        nombre: valores.nombre,
        cuit: valores.cuit,
        jurisdiccionId: Number(valores.jurisdiccionId),
        contacto: valores.contacto,
        email: valores.email,
        telefono: valores.telefono,
        cuentaCxcId: valores.cuentaCxcId ? Number(valores.cuentaCxcId) : undefined,
      }, { onSuccess: () => form.reset(VACIO) })
    }
  }

  const columnas = useMemo<ColumnDef<Cliente>[]>(
    () => [
      { header: "CUIT", accessorKey: "cuit" },
      { header: "Nombre", accessorKey: "nombre" },
      { header: "Jurisdicción", accessorKey: "jurisdiccionNombre" },
      { header: "Email", accessorKey: "email" },
      { header: "Estado", accessorKey: "activo", cell: (info) => (info.getValue() ? "Activo" : "Inactivo") },
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
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-lg font-semibold text-foreground">Clientes</h1>
          <p className="text-sm text-muted-foreground">Molde PL-1/PL-2 con FK a Jurisdicción (F2.2).</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" disabled={descargando !== null || !query.data} onClick={() => exportar("excel")}>
            {descargando === "excel" ? "Exportando…" : "Exportar Excel"}
          </Button>
          <Button variant="outline" size="sm" disabled={descargando !== null || !query.data} onClick={() => exportar("pdf")}>
            {descargando === "pdf" ? "Exportando…" : "Exportar PDF"}
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader><CardTitle>{editando ? `Editar ${editando.nombre}` : "Nuevo cliente"}</CardTitle></CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-3">
              <FormField control={form.control} name="nombre" render={({ field }) => (
                <FormItem><FormLabel>Nombre</FormLabel><FormControl><Input {...field} placeholder="Acme Inc" /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="cuit" render={({ field }) => (
                <FormItem><FormLabel>CUIT</FormLabel><FormControl><Input {...field} disabled={!!editando} placeholder="20-12345678-9" /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="jurisdiccionId" render={({ field }) => (
                <FormItem>
                  <FormLabel>Jurisdicción</FormLabel>
                  <FormControl>
                    <select {...field} disabled={jurisdicciones.isLoading} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                      <option value="">Seleccionar</option>
                      {jurisdicciones.data?.content?.map((j) => (
                        <option key={j.id} value={j.id.toString()}>{j.nombre}</option>
                      ))}
                    </select>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={form.control} name="contacto" render={({ field }) => (
                <FormItem><FormLabel>Contacto</FormLabel><FormControl><Input {...field} placeholder="Juan Pérez" /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="email" render={({ field }) => (
                <FormItem><FormLabel>Email</FormLabel><FormControl><Input {...field} type="email" placeholder="juan@example.com" /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="telefono" render={({ field }) => (
                <FormItem><FormLabel>Teléfono</FormLabel><FormControl><Input {...field} placeholder="1123456789" /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="cuentaCxcId" render={({ field }) => (
                <FormItem>
                  <FormLabel>Cuenta de créditos por ventas</FormLabel>
                  <FormControl>
                    <select {...field} disabled={cuentasContables.isLoading} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                      <option value="">Sin cuenta propia (usa el mapeo por defecto)</option>
                      {cuentasImputables.map((c) => <option key={c.id} value={c.id.toString()}>{c.codigo} — {c.nombre}</option>)}
                    </select>
                  </FormControl>
                  <FormMessage />
                </FormItem>
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

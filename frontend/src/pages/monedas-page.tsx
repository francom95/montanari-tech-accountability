import { zodResolver } from "@hookform/resolvers/zod"
import {
  flexRender,
  getCoreRowModel,
  useReactTable,
  type ColumnDef,
} from "@tanstack/react-table"
import { isAxiosError } from "axios"
import { useMemo, useState } from "react"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import {
  useCambiarEstadoMoneda,
  useCrearMoneda,
  useEditarMoneda,
  useEliminarMoneda,
  useMonedas,
} from "@/hooks/use-monedas"
import type { Moneda } from "@/types/moneda"

/**
 * Molde de referencia de PL-2 (F1.8): ruta + TanStack Table (paginado
 * manual, columnas con acciones) + formulario RHF/Zod que espeja las
 * validaciones del backend + hooks de React Query + manejo de 409. Para
 * una entidad nueva: copiar este archivo, cambiar el esquema Zod, las
 * columnas y los hooks importados.
 */
const esquemaMoneda = z.object({
  codigo: z
    .string()
    .regex(/^[A-Z]{3}$/, "Debe ser un código ISO 4217 de 3 letras mayúsculas (ej. ARS)"),
  nombre: z.string().min(1, "El nombre es obligatorio").max(80),
  simbolo: z.string().min(1, "El símbolo es obligatorio").max(5),
})

type MonedaValores = z.infer<typeof esquemaMoneda>

export function MonedasPage() {
  const [page, setPage] = useState(0)
  const [texto, setTexto] = useState("")
  const [editando, setEditando] = useState<Moneda | null>(null)
  const [error409, setError409] = useState<string | null>(null)

  const monedasQuery = useMonedas({ texto, page, size: 10 })
  const crearMutation = useCrearMoneda()
  const editarMutation = useEditarMoneda()
  const cambiarEstadoMutation = useCambiarEstadoMoneda()
  const eliminarMutation = useEliminarMoneda()

  const form = useForm<MonedaValores>({
    resolver: zodResolver(esquemaMoneda),
    defaultValues: { codigo: "", nombre: "", simbolo: "" },
  })

  function iniciarEdicion(moneda: Moneda) {
    setEditando(moneda)
    form.reset({ codigo: moneda.codigo, nombre: moneda.nombre, simbolo: moneda.simbolo })
  }

  function cancelarEdicion() {
    setEditando(null)
    form.reset({ codigo: "", nombre: "", simbolo: "" })
  }

  function onSubmit(valores: MonedaValores) {
    if (editando) {
      editarMutation.mutate(
        { id: editando.id, valores: { nombre: valores.nombre, simbolo: valores.simbolo } },
        { onSuccess: cancelarEdicion }
      )
    } else {
      crearMutation.mutate(valores, { onSuccess: () => form.reset({ codigo: "", nombre: "", simbolo: "" }) })
    }
  }

  function eliminar(id: number) {
    setError409(null)
    eliminarMutation.mutate(id, {
      onError: (error) => {
        if (isAxiosError(error) && error.response?.status === 409) {
          setError409(error.response.data?.detail ?? "No se puede eliminar: tiene datos asociados.")
        }
      },
    })
  }

  const columnas = useMemo<ColumnDef<Moneda>[]>(
    () => [
      { header: "Código", accessorKey: "codigo" },
      { header: "Nombre", accessorKey: "nombre" },
      { header: "Símbolo", accessorKey: "simbolo" },
      {
        header: "Estado",
        accessorKey: "activo",
        cell: (info) => (info.getValue() ? "Activa" : "Inactiva"),
      },
      {
        header: "Acciones",
        id: "acciones",
        cell: ({ row }) => {
          const moneda = row.original
          return (
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={() => iniciarEdicion(moneda)}>
                Editar
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={cambiarEstadoMutation.isPending}
                onClick={() => cambiarEstadoMutation.mutate({ id: moneda.id, activo: moneda.activo })}
              >
                {moneda.activo ? "Desactivar" : "Activar"}
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={eliminarMutation.isPending}
                onClick={() => eliminar(moneda.id)}
              >
                Eliminar
              </Button>
            </div>
          )
        },
      },
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [cambiarEstadoMutation.isPending, eliminarMutation.isPending]
  )

  const tabla = useReactTable({
    data: monedasQuery.data?.content ?? [],
    columns: columnas,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
    pageCount: monedasQuery.data?.totalPages ?? 0,
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-lg font-semibold text-foreground">Monedas</h1>
        <p className="text-sm text-muted-foreground">
          Molde de referencia PL-1/PL-2 (F1.8). ISO 4217 (ARS, USD, ...).
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>{editando ? `Editar ${editando.codigo}` : "Nueva moneda"}</CardTitle>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-4">
              <FormField
                control={form.control}
                name="codigo"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Código</FormLabel>
                    <FormControl>
                      <Input {...field} disabled={!!editando} placeholder="ARS" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="nombre"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Nombre</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="Peso Argentino" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="simbolo"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Símbolo</FormLabel>
                    <FormControl>
                      <Input {...field} placeholder="$" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="flex items-end gap-2">
                <Button type="submit" disabled={crearMutation.isPending || editarMutation.isPending}>
                  {editando ? "Guardar" : "Crear"}
                </Button>
                {editando && (
                  <Button type="button" variant="outline" onClick={cancelarEdicion}>
                    Cancelar
                  </Button>
                )}
              </div>

              {crearMutation.isError && (
                <p className="text-sm text-destructive sm:col-span-4">
                  No se pudo crear (¿el código ya existe?).
                </p>
              )}
            </form>
          </Form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Listado</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <Input
            placeholder="Buscar por código o nombre…"
            value={texto}
            onChange={(e) => {
              setTexto(e.target.value)
              setPage(0)
            }}
            className="max-w-xs"
          />

          {error409 && <p className="text-sm text-destructive">{error409}</p>}

          {monedasQuery.isLoading ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : (
            <>
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground">
                  {tabla.getHeaderGroups().map((headerGroup) => (
                    <tr key={headerGroup.id} className="border-b border-border">
                      {headerGroup.headers.map((header) => (
                        <th key={header.id} className="py-2 pr-4 font-medium">
                          {flexRender(header.column.columnDef.header, header.getContext())}
                        </th>
                      ))}
                    </tr>
                  ))}
                </thead>
                <tbody>
                  {tabla.getRowModel().rows.map((row) => (
                    <tr key={row.id} className="border-b border-border last:border-0">
                      {row.getVisibleCells().map((cell) => (
                        <td key={cell.id} className="py-2 pr-4">
                          {flexRender(cell.column.columnDef.cell, cell.getContext())}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>

              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  Anterior
                </Button>
                <span className="text-sm text-muted-foreground">
                  Página {page + 1} de {monedasQuery.data?.totalPages || 1}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page + 1 >= (monedasQuery.data?.totalPages ?? 1)}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Siguiente
                </Button>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

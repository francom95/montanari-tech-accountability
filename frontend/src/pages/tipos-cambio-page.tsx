import { zodResolver } from "@hookform/resolvers/zod"
import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from "@tanstack/react-table"
import { useMemo, useState } from "react"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useMonedas } from "@/hooks/use-monedas"
import {
  useCambiarEstadoTipoCambio,
  useCrearTipoCambio,
  useEditarTipoCambio,
  useEliminarTipoCambio,
  useTipoCambios,
} from "@/hooks/use-tipocambio"
import type { TipoCambio } from "@/types/tipocambio"

const CRITERIOS = ["BNA_VENTA", "BNA_COMPRA", "OFICIAL", "MANUAL", "OTRO"] as const

const esquemaCrear = z.object({
  fecha: z.string().min(1, "La fecha es obligatoria"),
  monedaId: z.string().min(1, "Elegí una moneda"),
  criterio: z.enum(CRITERIOS),
  valorCompra: z.string().min(1, "El valor de compra es obligatorio"),
  valorVenta: z.string().min(1, "El valor de venta es obligatorio"),
  fuente: z.string().optional(),
  observaciones: z.string().optional(),
})

type Valores = z.infer<typeof esquemaCrear>

export function TiposCambioPage() {
  const [page, setPage] = useState(0)
  const [texto, setTexto] = useState("")
  const [editando, setEditando] = useState<TipoCambio | null>(null)

  const query = useTipoCambios({ texto, page, size: 10 })
  const monedasQuery = useMonedas({ page: 0, size: 100, activo: true })
  const crear = useCrearTipoCambio()
  const editar = useEditarTipoCambio()
  const cambiarEstado = useCambiarEstadoTipoCambio()
  const eliminar = useEliminarTipoCambio()

  const form = useForm({
    resolver: zodResolver(esquemaCrear),
    defaultValues: { fecha: "", monedaId: "", criterio: "MANUAL", valorCompra: "", valorVenta: "", fuente: "", observaciones: "" },
  })

  function iniciarEdicion(e: TipoCambio) {
    setEditando(e)
    form.reset({
      fecha: e.fecha,
      monedaId: String(e.monedaId),
      criterio: e.criterio as Valores["criterio"],
      valorCompra: e.valorCompra,
      valorVenta: e.valorVenta,
      fuente: e.fuente ?? "",
      observaciones: e.observaciones ?? "",
    })
  }

  function cancelarEdicion() {
    setEditando(null)
    form.reset({ fecha: "", monedaId: "", criterio: "MANUAL", valorCompra: "", valorVenta: "", fuente: "", observaciones: "" })
  }

  function onSubmit(valores: Valores) {
    if (editando) {
      editar.mutate(
        { id: editando.id, valores: { valorCompra: valores.valorCompra, valorVenta: valores.valorVenta, fuente: valores.fuente, observaciones: valores.observaciones } },
        { onSuccess: cancelarEdicion }
      )
    } else {
      crear.mutate(
        { ...valores, monedaId: Number(valores.monedaId) },
        { onSuccess: cancelarEdicion }
      )
    }
  }

  const monedasPorId = useMemo(
    () => new Map((monedasQuery.data?.content ?? []).map((m) => [m.id, m.codigo])),
    [monedasQuery.data]
  )

  const columnas = useMemo<ColumnDef<TipoCambio>[]>(
    () => [
      { header: "Fecha", accessorKey: "fecha" },
      { header: "Moneda", accessorKey: "monedaId", cell: (info) => monedasPorId.get(info.getValue<number>()) ?? info.getValue<number>() },
      { header: "Criterio", accessorKey: "criterio" },
      { header: "Compra", accessorKey: "valorCompra" },
      { header: "Venta", accessorKey: "valorVenta" },
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
    [cambiarEstado.isPending, eliminar.isPending, monedasPorId]
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
        <h1 className="text-lg font-semibold text-foreground">Tipos de cambio</h1>
        <p className="text-sm text-muted-foreground">Molde PL-1/PL-2 replicado de Moneda (F2.1). FK a Moneda; fecha/moneda/criterio no se editan.</p>
      </div>

      <Card>
        <CardHeader><CardTitle>{editando ? `Editar tipo de cambio del ${editando.fecha}` : "Nuevo tipo de cambio"}</CardTitle></CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-4">
              <FormField control={form.control} name="fecha" render={({ field }) => (
                <FormItem><FormLabel>Fecha</FormLabel><FormControl><Input {...field} type="date" disabled={!!editando} /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="monedaId" render={({ field }) => (
                <FormItem>
                  <FormLabel>Moneda</FormLabel>
                  <FormControl>
                    <select {...field} disabled={!!editando} className="h-8 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50">
                      <option value="">Seleccionar…</option>
                      {(monedasQuery.data?.content ?? []).map((m) => <option key={m.id} value={String(m.id)}>{m.codigo}</option>)}
                    </select>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={form.control} name="criterio" render={({ field }) => (
                <FormItem>
                  <FormLabel>Criterio</FormLabel>
                  <FormControl>
                    <select {...field} disabled={!!editando} className="h-8 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50">
                      {CRITERIOS.map((c) => <option key={c} value={c}>{c}</option>)}
                    </select>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={form.control} name="valorCompra" render={({ field }) => (
                <FormItem><FormLabel>Valor compra</FormLabel><FormControl><Input {...field} placeholder="900.00" /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="valorVenta" render={({ field }) => (
                <FormItem><FormLabel>Valor venta</FormLabel><FormControl><Input {...field} placeholder="950.00" /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="fuente" render={({ field }) => (
                <FormItem><FormLabel>Fuente</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="observaciones" render={({ field }) => (
                <FormItem><FormLabel>Observaciones</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
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

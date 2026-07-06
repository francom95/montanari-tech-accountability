import { zodResolver } from "@hookform/resolvers/zod"
import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from "@tanstack/react-table"
import { useMemo, useState } from "react"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useCuentasBancarias } from "@/hooks/use-cuenta-bancaria"
import { useMonedas } from "@/hooks/use-monedas"
import {
  useCambiarEstadoTarjetaCredito,
  useCrearTarjetaCredito,
  useEditarTarjetaCredito,
  useEliminarTarjetaCredito,
  useTarjetasCredito,
} from "@/hooks/use-tarjeta-credito"
import type { TarjetaCredito } from "@/types/tarjeta-credito"

const esquema = z.object({
  entidad: z.string().min(1, "La entidad es obligatoria").max(80),
  monedaId: z.string().min(1, "Elegí una moneda"),
  diaCierre: z.string().min(1, "El día de cierre es obligatorio"),
  diaVencimiento: z.string().min(1, "El día de vencimiento es obligatorio"),
  cuentaBancariaDebitoId: z.string().min(1, "Elegí una cuenta de débito"),
  saldoInicial: z.string().min(1, "El saldo inicial es obligatorio"),
  fechaSaldoInicial: z.string().min(1, "La fecha es obligatoria"),
})

type Valores = z.infer<typeof esquema>

const DEFAULTS: Valores = {
  entidad: "",
  monedaId: "",
  diaCierre: "",
  diaVencimiento: "",
  cuentaBancariaDebitoId: "",
  saldoInicial: "",
  fechaSaldoInicial: "",
}

export function TarjetasCreditoPage() {
  const [page, setPage] = useState(0)
  const [texto, setTexto] = useState("")
  const [editando, setEditando] = useState<TarjetaCredito | null>(null)

  const query = useTarjetasCredito({ texto, page, size: 10 })
  const monedasQuery = useMonedas({ page: 0, size: 100, activo: true })
  const cuentasQuery = useCuentasBancarias({ page: 0, size: 100, activo: true })
  const crear = useCrearTarjetaCredito()
  const editar = useEditarTarjetaCredito()
  const cambiarEstado = useCambiarEstadoTarjetaCredito()
  const eliminar = useEliminarTarjetaCredito()

  const form = useForm({ resolver: zodResolver(esquema), defaultValues: DEFAULTS })

  function iniciarEdicion(t: TarjetaCredito) {
    setEditando(t)
    form.reset({
      entidad: t.entidad,
      monedaId: String(t.monedaId),
      diaCierre: String(t.diaCierre),
      diaVencimiento: String(t.diaVencimiento),
      cuentaBancariaDebitoId: String(t.cuentaBancariaDebitoId),
      saldoInicial: t.saldoInicial,
      fechaSaldoInicial: t.fechaSaldoInicial,
    })
  }

  function cancelarEdicion() {
    setEditando(null)
    form.reset(DEFAULTS)
  }

  function onSubmit(valores: Valores) {
    const normalizadas = {
      entidad: valores.entidad,
      monedaId: Number(valores.monedaId),
      diaCierre: Number(valores.diaCierre),
      diaVencimiento: Number(valores.diaVencimiento),
      cuentaBancariaDebitoId: Number(valores.cuentaBancariaDebitoId),
      saldoInicial: valores.saldoInicial,
      fechaSaldoInicial: valores.fechaSaldoInicial,
    }
    if (editando) {
      editar.mutate({ id: editando.id, valores: normalizadas }, { onSuccess: cancelarEdicion })
    } else {
      crear.mutate(normalizadas, { onSuccess: cancelarEdicion })
    }
  }

  const columnas = useMemo<ColumnDef<TarjetaCredito>[]>(
    () => [
      { header: "Entidad", accessorKey: "entidad" },
      { header: "Moneda", accessorKey: "monedaCodigo" },
      { header: "Cierre", accessorKey: "diaCierre" },
      { header: "Vencimiento", accessorKey: "diaVencimiento" },
      { header: "Cuenta débito", accessorKey: "cuentaBancariaDebitoAlias" },
      { header: "Saldo actual", accessorKey: "saldoActual" },
      { header: "Estado", accessorKey: "activo", cell: (info) => (info.getValue() ? "Activo" : "Inactivo") },
      {
        header: "Acciones",
        id: "acciones",
        cell: ({ row }) => {
          const t = row.original
          return (
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={() => iniciarEdicion(t)}>Editar</Button>
              <Button variant="outline" size="sm" disabled={cambiarEstado.isPending} onClick={() => cambiarEstado.mutate({ id: t.id, activo: t.activo })}>
                {t.activo ? "Desactivar" : "Activar"}
              </Button>
              <Button variant="outline" size="sm" disabled={eliminar.isPending} onClick={() => eliminar.mutate(t.id)}>Eliminar</Button>
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
        <h1 className="text-lg font-semibold text-foreground">Tarjetas de crédito</h1>
        <p className="text-sm text-muted-foreground">Molde PL-1/PL-2 con FKs a Moneda y Cuenta bancaria de débito (F2.4). El saldo inicial y su fecha recalculan el saldo actual.</p>
      </div>

      <Card>
        <CardHeader><CardTitle>{editando ? `Editar ${editando.entidad}` : "Nueva tarjeta"}</CardTitle></CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-4">
              <FormField control={form.control} name="entidad" render={({ field }) => (
                <FormItem><FormLabel>Entidad</FormLabel><FormControl><Input {...field} placeholder="Visa Banco Galicia" /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="monedaId" render={({ field }) => (
                <FormItem>
                  <FormLabel>Moneda</FormLabel>
                  <FormControl>
                    <select {...field} className="h-8 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50">
                      <option value="">Seleccionar…</option>
                      {(monedasQuery.data?.content ?? []).map((m) => <option key={m.id} value={String(m.id)}>{m.codigo}</option>)}
                    </select>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={form.control} name="diaCierre" render={({ field }) => (
                <FormItem><FormLabel>Día de cierre</FormLabel><FormControl><Input {...field} type="number" min={1} max={31} /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="diaVencimiento" render={({ field }) => (
                <FormItem><FormLabel>Día de vencimiento</FormLabel><FormControl><Input {...field} type="number" min={1} max={31} /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="cuentaBancariaDebitoId" render={({ field }) => (
                <FormItem>
                  <FormLabel>Cuenta de débito</FormLabel>
                  <FormControl>
                    <select {...field} className="h-8 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50">
                      <option value="">Seleccionar…</option>
                      {(cuentasQuery.data?.content ?? []).map((c) => <option key={c.id} value={String(c.id)}>{c.alias}</option>)}
                    </select>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={form.control} name="saldoInicial" render={({ field }) => (
                <FormItem><FormLabel>Saldo inicial</FormLabel><FormControl><Input {...field} placeholder="-1500.00" /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="fechaSaldoInicial" render={({ field }) => (
                <FormItem><FormLabel>Fecha del saldo</FormLabel><FormControl><Input {...field} type="date" /></FormControl><FormMessage /></FormItem>
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

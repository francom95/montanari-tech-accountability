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
  useCambiarEstadoCuentaBancaria,
  useCrearCuentaBancaria,
  useCuentasBancarias,
  useEditarCuentaBancaria,
  useEliminarCuentaBancaria,
} from "@/hooks/use-cuenta-bancaria"
import { useMonedas } from "@/hooks/use-monedas"
import type { CuentaBancaria } from "@/types/cuenta-bancaria"

const TIPOS = ["CUENTA_CORRIENTE", "CAJA_AHORRO", "MERCADO_PAGO", "CAJA_FISICA", "OTRA"] as const
const ESTADOS_CONCILIACION = ["PENDIENTE", "CONCILIADA"] as const

const esquema = z.object({
  entidad: z.string().min(1, "La entidad es obligatoria").max(80),
  alias: z.string().min(1, "El alias es obligatorio").max(80),
  monedaId: z.string().min(1, "Elegí una moneda"),
  tipo: z.enum(TIPOS),
  estadoConciliacion: z.enum(ESTADOS_CONCILIACION),
  saldoInicial: z.string().min(1, "El saldo inicial es obligatorio"),
  fechaSaldoInicial: z.string().min(1, "La fecha es obligatoria"),
})

type Valores = z.infer<typeof esquema>

const DEFAULTS: Valores = {
  entidad: "",
  alias: "",
  monedaId: "",
  tipo: "CUENTA_CORRIENTE",
  estadoConciliacion: "PENDIENTE",
  saldoInicial: "",
  fechaSaldoInicial: "",
}

export function CuentasBancariasPage() {
  const [page, setPage] = useState(0)
  const [texto, setTexto] = useState("")
  const [editando, setEditando] = useState<CuentaBancaria | null>(null)

  const query = useCuentasBancarias({ texto, page, size: 10 })
  const monedasQuery = useMonedas({ page: 0, size: 100, activo: true })
  const crear = useCrearCuentaBancaria()
  const editar = useEditarCuentaBancaria()
  const cambiarEstado = useCambiarEstadoCuentaBancaria()
  const eliminar = useEliminarCuentaBancaria()

  const form = useForm({ resolver: zodResolver(esquema), defaultValues: DEFAULTS })

  function iniciarEdicion(e: CuentaBancaria) {
    setEditando(e)
    form.reset({
      entidad: e.entidad,
      alias: e.alias,
      monedaId: String(e.monedaId),
      tipo: e.tipo,
      estadoConciliacion: e.estadoConciliacion,
      saldoInicial: e.saldoInicial,
      fechaSaldoInicial: e.fechaSaldoInicial,
    })
  }

  function cancelarEdicion() {
    setEditando(null)
    form.reset(DEFAULTS)
  }

  function onSubmit(valores: Valores) {
    const normalizadas = { ...valores, monedaId: Number(valores.monedaId) }
    if (editando) {
      editar.mutate({ id: editando.id, valores: normalizadas }, { onSuccess: cancelarEdicion })
    } else {
      crear.mutate(normalizadas, { onSuccess: cancelarEdicion })
    }
  }

  const columnas = useMemo<ColumnDef<CuentaBancaria>[]>(
    () => [
      { header: "Alias", accessorKey: "alias" },
      { header: "Entidad", accessorKey: "entidad" },
      { header: "Moneda", accessorKey: "monedaCodigo" },
      { header: "Tipo", accessorKey: "tipo" },
      { header: "Conciliación", accessorKey: "estadoConciliacion" },
      { header: "Saldo actual", accessorKey: "saldoActual" },
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
      <div>
        <h1 className="text-lg font-semibold text-foreground">Cuentas bancarias / cuentas de dinero</h1>
        <p className="text-sm text-muted-foreground">Molde PL-1/PL-2 con FK a Moneda (F2.4). El saldo inicial y su fecha recalculan el saldo actual.</p>
      </div>

      <Card>
        <CardHeader><CardTitle>{editando ? `Editar ${editando.alias}` : "Nueva cuenta"}</CardTitle></CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-4">
              <FormField control={form.control} name="entidad" render={({ field }) => (
                <FormItem><FormLabel>Entidad</FormLabel><FormControl><Input {...field} placeholder="Banco Galicia" /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="alias" render={({ field }) => (
                <FormItem><FormLabel>Alias</FormLabel><FormControl><Input {...field} placeholder="Banco Galicia CC" /></FormControl><FormMessage /></FormItem>
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
              <FormField control={form.control} name="tipo" render={({ field }) => (
                <FormItem>
                  <FormLabel>Tipo</FormLabel>
                  <FormControl>
                    <select {...field} className="h-8 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50">
                      {TIPOS.map((t) => <option key={t} value={t}>{t}</option>)}
                    </select>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={form.control} name="estadoConciliacion" render={({ field }) => (
                <FormItem>
                  <FormLabel>Conciliación</FormLabel>
                  <FormControl>
                    <select {...field} className="h-8 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50">
                      {ESTADOS_CONCILIACION.map((e) => <option key={e} value={e}>{e}</option>)}
                    </select>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={form.control} name="saldoInicial" render={({ field }) => (
                <FormItem><FormLabel>Saldo inicial</FormLabel><FormControl><Input {...field} placeholder="1000.00" /></FormControl><FormMessage /></FormItem>
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

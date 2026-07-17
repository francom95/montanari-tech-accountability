import { zodResolver } from "@hookform/resolvers/zod"
import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from "@tanstack/react-table"
import { useMemo, useState } from "react"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useCurrentUser } from "@/hooks/use-auth"
import { useCuentasContables } from "@/hooks/use-cuenta-contable"
import {
  useCambiarEstadoMapeoCuenta,
  useCrearMapeoCuenta,
  useEliminarMapeoCuenta,
  useMapeosCuenta,
} from "@/hooks/use-mapeo-cuenta"
import { CONCEPTOS_CONTABLES, type ConceptoContable, type MapeoCuenta } from "@/types/mapeo-cuenta"

const esquema = z.object({
  concepto: z.string().min(1, "Obligatorio"),
  discriminadorTipo: z.string().optional(),
  discriminadorValor: z.string().optional(),
  cuentaContableId: z.string().min(1, "Obligatoria"),
})

type Valores = z.infer<typeof esquema>
const VACIO: Valores = { concepto: "", discriminadorTipo: "", discriminadorValor: "", cuentaContableId: "" }

export function MapeoCuentaPage() {
  const [conceptoFiltro, setConceptoFiltro] = useState<ConceptoContable | "">("")
  const usuario = useCurrentUser()
  const esAdmin = usuario.data?.rol === "ADMINISTRADOR"

  const query = useMapeosCuenta({ concepto: conceptoFiltro || undefined, page: 0, size: 100 })
  const cuentas = useCuentasContables({ activo: true, page: 0, size: 500 })
  const cuentasImputables = useMemo(() => (cuentas.data?.content ?? []).filter((c) => c.imputable), [cuentas.data])
  const crear = useCrearMapeoCuenta()
  const cambiarEstado = useCambiarEstadoMapeoCuenta()
  const eliminar = useEliminarMapeoCuenta()

  const form = useForm<Valores>({ resolver: zodResolver(esquema), defaultValues: VACIO })

  function onSubmit(valores: Valores) {
    crear.mutate({
      concepto: valores.concepto as ConceptoContable,
      discriminadorTipo: valores.discriminadorTipo || undefined,
      discriminadorValor: valores.discriminadorValor || undefined,
      cuentaContableId: Number(valores.cuentaContableId),
    }, { onSuccess: () => form.reset(VACIO) })
  }

  const columnas = useMemo<ColumnDef<MapeoCuenta>[]>(
    () => [
      { header: "Concepto", accessorKey: "concepto" },
      { header: "Discriminador", id: "discriminador", cell: ({ row }) => row.original.discriminadorTipo ? `${row.original.discriminadorTipo}=${row.original.discriminadorValor}` : "Por defecto" },
      { header: "Cuenta", id: "cuenta", cell: ({ row }) => `${row.original.cuentaContableCodigo} — ${row.original.cuentaContableNombre}` },
      { header: "Estado", accessorKey: "activo", cell: (info) => (info.getValue() ? "Activo" : "Inactivo") },
      {
        header: "Acciones",
        id: "acciones",
        cell: ({ row }) => {
          if (!esAdmin) return <span className="text-xs text-muted-foreground">Solo administrador</span>
          const m = row.original
          return (
            <div className="flex gap-2">
              <Button variant="outline" size="sm" disabled={cambiarEstado.isPending} onClick={() => cambiarEstado.mutate({ id: m.id, activo: m.activo })}>
                {m.activo ? "Desactivar" : "Activar"}
              </Button>
              <Button variant="outline" size="sm" disabled={eliminar.isPending} onClick={() => eliminar.mutate(m.id)}>Eliminar</Button>
            </div>
          )
        },
      },
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [esAdmin, cambiarEstado.isPending, eliminar.isPending]
  )

  const tabla = useReactTable({
    data: query.data?.content ?? [],
    columns: columnas,
    getCoreRowModel: getCoreRowModel(),
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-lg font-semibold text-foreground">Mapeo de cuentas</h1>
        <p className="text-sm text-muted-foreground">
          Configura a qué cuenta contable resuelve cada concepto que usan los asientos automáticos (F4.1). Sin fila por defecto, el generador rechaza confirmar con "MAPEO_CUENTA_FALTANTE".
        </p>
      </div>

      {esAdmin && (
        <Card>
          <CardHeader><CardTitle>Nuevo mapeo</CardTitle></CardHeader>
          <CardContent>
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-4">
                <FormField control={form.control} name="concepto" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Concepto</FormLabel>
                    <FormControl>
                      <select {...field} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        <option value="">Seleccionar</option>
                        {CONCEPTOS_CONTABLES.map((c) => <option key={c} value={c}>{c}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="discriminadorTipo" render={({ field }) => (
                  <FormItem><FormLabel>Discriminador (tipo)</FormLabel><FormControl><Input {...field} placeholder="TIPO_INGRESO" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="discriminadorValor" render={({ field }) => (
                  <FormItem><FormLabel>Discriminador (valor)</FormLabel><FormControl><Input {...field} placeholder="VENTA" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="cuentaContableId" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Cuenta contable</FormLabel>
                    <FormControl>
                      <select {...field} disabled={cuentas.isLoading} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        <option value="">Seleccionar</option>
                        {cuentasImputables.map((c) => <option key={c.id} value={c.id.toString()}>{c.codigo} — {c.nombre}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <div className="sm:col-span-4">
                  <Button type="submit" disabled={crear.isPending}>Crear mapeo</Button>
                </div>
              </form>
            </Form>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader><CardTitle>Mapeos configurados</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <select
            value={conceptoFiltro}
            onChange={(e) => setConceptoFiltro(e.target.value as ConceptoContable | "")}
            className="h-8 max-w-xs rounded-lg border border-input bg-background px-2 py-1 text-sm"
          >
            <option value="">Todos los conceptos</option>
            {CONCEPTOS_CONTABLES.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
          {query.isLoading ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : (
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
          )}
        </CardContent>
      </Card>
    </div>
  )
}

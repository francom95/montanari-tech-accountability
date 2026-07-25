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
import { useCompromisos } from "@/hooks/use-compromiso"
import { useCuentasBancarias } from "@/hooks/use-cuenta-bancaria"
import {
  useCambiarEstadoInversion,
  useCrearInversion,
  useEditarInversion,
  useEliminarInversion,
  useInversiones,
} from "@/hooks/use-inversion"
import { useVencimientos } from "@/hooks/use-vencimiento"
import { ESTADOS_INVERSION, TIPOS_VINCULO_INVERSION, type EstadoInversion, type Inversion, type TipoVinculoInversion } from "@/types/inversion"

const ESTADO_LABEL: Record<EstadoInversion, string> = { ACTIVA: "Activa", RESCATADA_TOTAL: "Rescatada total", CANCELADA: "Cancelada" }
const VINCULO_LABEL: Record<TipoVinculoInversion, string> = { COMPROMISO: "Compromiso", VENCIMIENTO: "Vencimiento" }

const selectClase = "h-8 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
const pesos = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" })

const esquema = z.object({
  instrumento: z.string().min(1, "El instrumento es obligatorio").max(120),
  cuentaOrigenId: z.string().min(1, "Elegí una cuenta de origen"),
  objetivoDelDinero: z.string().optional(),
  vinculoTipo: z.string().optional(),
  vinculoRefId: z.string().optional(),
  estado: z.enum(ESTADOS_INVERSION as [EstadoInversion, ...EstadoInversion[]]),
})

type Valores = z.infer<typeof esquema>

const VACIO: Valores = { instrumento: "", cuentaOrigenId: "", objetivoDelDinero: "", vinculoTipo: "", vinculoRefId: "", estado: "ACTIVA" }

export function InversionesPage() {
  const [page, setPage] = useState(0)
  const [texto, setTexto] = useState("")
  const [editando, setEditando] = useState<Inversion | null>(null)

  const query = useInversiones({ texto, page, size: 10 })
  const cuentasQuery = useCuentasBancarias({ page: 0, size: 100, activo: true })
  const compromisosQuery = useCompromisos({ page: 0, size: 200 })
  const vencimientosQuery = useVencimientos({ page: 0, size: 200 })
  const crear = useCrearInversion()
  const editar = useEditarInversion()
  const cambiarEstado = useCambiarEstadoInversion()
  const eliminar = useEliminarInversion()

  const form = useForm<Valores>({ resolver: zodResolver(esquema), defaultValues: VACIO })
  const vinculoTipo = form.watch("vinculoTipo") as TipoVinculoInversion | ""

  function iniciarEdicion(inv: Inversion) {
    setEditando(inv)
    form.reset({
      instrumento: inv.instrumento,
      cuentaOrigenId: String(inv.cuentaOrigenId),
      objetivoDelDinero: inv.objetivoDelDinero ?? "",
      vinculoTipo: inv.vinculoTipo ?? "",
      vinculoRefId: inv.vinculoRefId ? String(inv.vinculoRefId) : "",
      estado: inv.estado,
    })
  }

  function cancelarEdicion() {
    setEditando(null)
    form.reset(VACIO)
  }

  function onSubmit(valores: Valores) {
    const base = {
      instrumento: valores.instrumento,
      cuentaOrigenId: Number(valores.cuentaOrigenId),
      objetivoDelDinero: valores.objetivoDelDinero || undefined,
      vinculoTipo: (valores.vinculoTipo || undefined) as TipoVinculoInversion | undefined,
      vinculoRefId: valores.vinculoRefId ? Number(valores.vinculoRefId) : undefined,
    }
    if (editando) {
      editar.mutate({ id: editando.id, valores: { ...base, estado: valores.estado } }, { onSuccess: cancelarEdicion })
    } else {
      crear.mutate(base, { onSuccess: cancelarEdicion })
    }
  }

  const columnas = useMemo<ColumnDef<Inversion>[]>(
    () => [
      { header: "Instrumento", accessorKey: "instrumento" },
      { header: "Cuenta origen", accessorKey: "cuentaOrigenAlias" },
      { header: "Objetivo", accessorKey: "objetivoDelDinero", cell: (info) => info.getValue() ?? "—" },
      { header: "Valuación actual", accessorKey: "valuacionActual", cell: (info) => pesos.format(info.getValue() as number) },
      { header: "Rendimiento", accessorKey: "rendimiento", cell: (info) => pesos.format(info.getValue() as number) },
      { header: "Estado", accessorKey: "estado", cell: (info) => ESTADO_LABEL[info.getValue() as EstadoInversion] },
      {
        header: "Acciones",
        id: "acciones",
        cell: ({ row }) => {
          const inv = row.original
          return (
            <div className="flex flex-wrap gap-2">
              <Link to={`/presupuesto/inversiones/${inv.id}`}>
                <Button variant="outline" size="sm">Ver detalle</Button>
              </Link>
              <Button variant="outline" size="sm" onClick={() => iniciarEdicion(inv)}>Editar</Button>
              <Button variant="outline" size="sm" disabled={cambiarEstado.isPending} onClick={() => cambiarEstado.mutate({ id: inv.id, activo: inv.activo })}>
                {inv.activo ? "Desactivar" : "Activar"}
              </Button>
              <Button variant="outline" size="sm" disabled={eliminar.isPending} onClick={() => eliminar.mutate(inv.id)}>Eliminar</Button>
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
        <h1 className="text-lg font-semibold text-foreground">Inversiones</h1>
        <p className="text-sm text-muted-foreground">Fondo Fima y similares (F8.4) — vínculo opcional a un Compromiso (F8.2) o Vencimiento (F8.1) para proyectar el rescate en el flujo de caja (F8.3).</p>
      </div>

      <Card>
        <CardHeader><CardTitle>{editando ? `Editar ${editando.instrumento}` : "Nueva inversión"}</CardTitle></CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="instrumento" render={({ field }) => (
                  <FormItem><FormLabel>Instrumento</FormLabel><FormControl><Input {...field} placeholder="Fondo Fima" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="cuentaOrigenId" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Cuenta de origen</FormLabel>
                    <FormControl>
                      <select {...field} disabled={cuentasQuery.isLoading} className={selectClase}>
                        <option value="">Seleccionar…</option>
                        {(cuentasQuery.data?.content ?? []).map((c) => <option key={c.id} value={String(c.id)}>{c.alias}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="objetivoDelDinero" render={({ field }) => (
                  <FormItem><FormLabel>Objetivo (opcional)</FormLabel><FormControl><Input {...field} placeholder="IVA marzo" /></FormControl><FormMessage /></FormItem>
                )} />
              </div>
              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="vinculoTipo" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Vínculo (opcional)</FormLabel>
                    <FormControl>
                      <select {...field} onChange={(e) => { field.onChange(e); form.setValue("vinculoRefId", "") }} className={selectClase}>
                        <option value="">Sin vínculo</option>
                        {TIPOS_VINCULO_INVERSION.map((t) => <option key={t} value={t}>{VINCULO_LABEL[t]}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                {vinculoTipo === "COMPROMISO" && (
                  <FormField control={form.control} name="vinculoRefId" render={({ field }) => (
                    <FormItem>
                      <FormLabel>Compromiso</FormLabel>
                      <FormControl>
                        <select {...field} className={selectClase}>
                          <option value="">Seleccionar…</option>
                          {(compromisosQuery.data?.content ?? []).map((c) => <option key={c.id} value={String(c.id)}>{c.concepto} — {c.fechaPrevista}</option>)}
                        </select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )} />
                )}
                {vinculoTipo === "VENCIMIENTO" && (
                  <FormField control={form.control} name="vinculoRefId" render={({ field }) => (
                    <FormItem>
                      <FormLabel>Vencimiento</FormLabel>
                      <FormControl>
                        <select {...field} className={selectClase}>
                          <option value="">Seleccionar…</option>
                          {(vencimientosQuery.data?.content ?? []).map((v) => <option key={v.id} value={String(v.id)}>{v.descripcion} — {v.fecha}</option>)}
                        </select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )} />
                )}
                {editando && (
                  <FormField control={form.control} name="estado" render={({ field }) => (
                    <FormItem>
                      <FormLabel>Estado</FormLabel>
                      <FormControl>
                        <select {...field} className={selectClase}>
                          {ESTADOS_INVERSION.map((e) => <option key={e} value={e}>{ESTADO_LABEL[e]}</option>)}
                        </select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )} />
                )}
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
          <Input placeholder="Buscar…" value={texto} onChange={(e) => { setTexto(e.target.value); setPage(0) }} className="max-w-xs" />
          {query.isLoading ? (
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
                  </tbody>
                </table>
              </div>
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

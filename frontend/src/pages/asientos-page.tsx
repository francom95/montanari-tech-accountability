import { zodResolver } from "@hookform/resolvers/zod"
import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from "@tanstack/react-table"
import { useMemo, useState } from "react"
import { useFieldArray, useForm, useWatch, type Control } from "react-hook-form"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import {
  useAsientos,
  useConfirmarAsiento,
  useCrearAsiento,
  useEditarAsiento,
  useEliminarAsiento,
} from "@/hooks/use-asiento"
import { useClientes } from "@/hooks/use-cliente"
import { useCuentasBancarias } from "@/hooks/use-cuenta-bancaria"
import { useCuentasContables } from "@/hooks/use-cuenta-contable"
import { useEtapas } from "@/hooks/use-etapa"
import { useMonedas } from "@/hooks/use-monedas"
import { useProveedores } from "@/hooks/use-proveedor"
import { useProyectos } from "@/hooks/use-proyecto"
import type { Asiento, EstadoAsiento } from "@/types/asiento"

const ESTADOS: EstadoAsiento[] = ["BORRADOR", "CONFIRMADO", "ANULADO"]
const ESTADO_LABEL: Record<EstadoAsiento, string> = { BORRADOR: "Borrador", CONFIRMADO: "Confirmado", ANULADO: "Anulado" }

const esquemaLinea = z.object({
  cuentaContableId: z.string().min(1, "Obligatoria"),
  debe: z.string(),
  haber: z.string(),
  monedaId: z.string().min(1, "Obligatoria"),
  tipoCambio: z.string().optional(),
  importeOriginal: z.string().optional(),
  leyenda: z.string().optional(),
  proyectoId: z.string().optional(),
  etapaId: z.string().optional(),
  clienteId: z.string().optional(),
  proveedorId: z.string().optional(),
  cuentaBancariaId: z.string().optional(),
})

const esquema = z.object({
  fecha: z.string().min(1, "La fecha es obligatoria"),
  descripcion: z.string().min(1, "La descripción es obligatoria").max(500),
  observaciones: z.string().optional(),
  lineas: z.array(esquemaLinea),
})

type Valores = z.infer<typeof esquema>
type LineaValores = Valores["lineas"][number]

const LINEA_VACIA: LineaValores = {
  cuentaContableId: "", debe: "", haber: "", monedaId: "", tipoCambio: "", importeOriginal: "",
  leyenda: "", proyectoId: "", etapaId: "", clienteId: "", proveedorId: "", cuentaBancariaId: "",
}

function asientoAValores(a: Asiento): Valores {
  return {
    fecha: a.fecha,
    descripcion: a.descripcion,
    observaciones: a.observaciones ?? "",
    lineas: a.lineas.map((l) => ({
      cuentaContableId: l.cuentaContableId.toString(),
      debe: l.debe ? l.debe.toString() : "",
      haber: l.haber ? l.haber.toString() : "",
      monedaId: l.monedaId.toString(),
      tipoCambio: l.tipoCambio?.toString() ?? "",
      importeOriginal: l.importeOriginal?.toString() ?? "",
      leyenda: l.leyenda ?? "",
      proyectoId: l.proyectoId?.toString() ?? "",
      etapaId: l.etapaId?.toString() ?? "",
      clienteId: l.clienteId?.toString() ?? "",
      proveedorId: l.proveedorId?.toString() ?? "",
      cuentaBancariaId: l.cuentaBancariaId?.toString() ?? "",
    })),
  }
}

const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"
const inputClase = "h-8"

export function AsientosPage() {
  const [page, setPage] = useState(0)
  const [texto, setTexto] = useState("")
  const [estadoFiltro, setEstadoFiltro] = useState<EstadoAsiento | "">("")
  const [editando, setEditando] = useState<Asiento | null>(null)
  const [mostrarForm, setMostrarForm] = useState(false)

  const query = useAsientos({ texto, estado: estadoFiltro || undefined, page, size: 10 })
  const crear = useCrearAsiento()
  const editar = useEditarAsiento()
  const confirmar = useConfirmarAsiento()
  const eliminar = useEliminarAsiento()

  const monedas = useMonedas({ page: 0, size: 20 })
  const monedaArsId = monedas.data?.content?.find((m) => m.codigo === "ARS")?.id

  const form = useForm<Valores>({
    resolver: zodResolver(esquema),
    defaultValues: { fecha: "", descripcion: "", observaciones: "", lineas: [] },
  })
  const lineas = useFieldArray({ control: form.control, name: "lineas" })

  function nuevoAsiento() {
    setEditando(null)
    form.reset({ fecha: "", descripcion: "", observaciones: "", lineas: [] })
    setMostrarForm(true)
  }

  function iniciarEdicion(a: Asiento) {
    setEditando(a)
    form.reset(asientoAValores(a))
    setMostrarForm(true)
  }

  function cancelar() {
    setMostrarForm(false)
    setEditando(null)
  }

  function agregarLinea() {
    lineas.append({ ...LINEA_VACIA, monedaId: monedaArsId ? monedaArsId.toString() : "" })
  }

  function onSubmit(valores: Valores) {
    const payload = {
      fecha: valores.fecha,
      descripcion: valores.descripcion,
      observaciones: valores.observaciones || undefined,
      lineas: valores.lineas.map((l) => ({
        cuentaContableId: Number(l.cuentaContableId),
        debe: l.debe ? Number(l.debe) : 0,
        haber: l.haber ? Number(l.haber) : 0,
        monedaId: Number(l.monedaId),
        tipoCambio: l.tipoCambio ? Number(l.tipoCambio) : undefined,
        importeOriginal: l.importeOriginal ? Number(l.importeOriginal) : undefined,
        leyenda: l.leyenda || undefined,
        proyectoId: l.proyectoId ? Number(l.proyectoId) : undefined,
        etapaId: l.etapaId ? Number(l.etapaId) : undefined,
        clienteId: l.clienteId ? Number(l.clienteId) : undefined,
        proveedorId: l.proveedorId ? Number(l.proveedorId) : undefined,
        cuentaBancariaId: l.cuentaBancariaId ? Number(l.cuentaBancariaId) : undefined,
      })),
    }
    if (editando) {
      editar.mutate({ id: editando.id, valores: payload }, { onSuccess: cancelar })
    } else {
      crear.mutate(payload, { onSuccess: cancelar })
    }
  }

  const lineasObservadas = useWatch({ control: form.control, name: "lineas" })
  const totales = useMemo(() => {
    const debe = (lineasObservadas ?? []).reduce((acc, l) => acc + (Number(l.debe) || 0), 0)
    const haber = (lineasObservadas ?? []).reduce((acc, l) => acc + (Number(l.haber) || 0), 0)
    return { debe, haber, diferencia: debe - haber }
  }, [lineasObservadas])

  const columnas = useMemo<ColumnDef<Asiento>[]>(
    () => [
      { header: "N°", accessorKey: "numero", cell: (info) => info.getValue() ?? "—" },
      { header: "Fecha", accessorKey: "fecha" },
      { header: "Descripción", accessorKey: "descripcion" },
      { header: "Estado", accessorKey: "estado", cell: (info) => ESTADO_LABEL[info.getValue() as EstadoAsiento] },
      { header: "Debe", accessorKey: "totalDebe", cell: (info) => (info.getValue() as number).toFixed(2) },
      { header: "Haber", accessorKey: "totalHaber", cell: (info) => (info.getValue() as number).toFixed(2) },
      {
        header: "Acciones",
        id: "acciones",
        cell: ({ row }) => {
          const a = row.original
          if (a.estado !== "BORRADOR") {
            return <span className="text-xs text-muted-foreground">Sin acciones (F3.5)</span>
          }
          return (
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={() => iniciarEdicion(a)}>Editar</Button>
              <Button variant="outline" size="sm" disabled={confirmar.isPending} onClick={() => confirmar.mutate(a.id)}>Confirmar</Button>
              <Button variant="outline" size="sm" disabled={eliminar.isPending} onClick={() => eliminar.mutate(a.id)}>Eliminar</Button>
            </div>
          )
        },
      },
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [confirmar.isPending, eliminar.isPending]
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
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-semibold text-foreground">Asientos contables</h1>
          <p className="text-sm text-muted-foreground">
            Carga manual de asientos (F3.4). Búsqueda avanzada, duplicación, edición de confirmados y anulación se agregan en F3.5.
          </p>
        </div>
        {!mostrarForm && <Button onClick={nuevoAsiento}>Nuevo asiento</Button>}
      </div>

      {mostrarForm && (
        <Card>
          <CardHeader><CardTitle>{editando ? `Editar borrador N° ${editando.numero ?? "s/n"}` : "Nuevo asiento (borrador)"}</CardTitle></CardHeader>
          <CardContent>
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <div className="grid gap-4 sm:grid-cols-3">
                  <FormField control={form.control} name="fecha" render={({ field }) => (
                    <FormItem><FormLabel>Fecha</FormLabel><FormControl><Input {...field} type="date" className={inputClase} /></FormControl><FormMessage /></FormItem>
                  )} />
                  <FormField control={form.control} name="descripcion" render={({ field }) => (
                    <FormItem className="sm:col-span-2"><FormLabel>Descripción</FormLabel><FormControl><Input {...field} className={inputClase} /></FormControl><FormMessage /></FormItem>
                  )} />
                </div>
                <FormField control={form.control} name="observaciones" render={({ field }) => (
                  <FormItem><FormLabel>Observaciones</FormLabel><FormControl><Input {...field} className={inputClase} /></FormControl><FormMessage /></FormItem>
                )} />

                <div className="overflow-x-auto">
                  <table className="w-full min-w-[1400px] text-left text-sm">
                    <thead className="text-muted-foreground">
                      <tr className="border-b border-border">
                        <th className="py-2 pr-2 font-medium">Cuenta</th>
                        <th className="py-2 pr-2 font-medium">Debe</th>
                        <th className="py-2 pr-2 font-medium">Haber</th>
                        <th className="py-2 pr-2 font-medium">Moneda</th>
                        <th className="py-2 pr-2 font-medium">TC</th>
                        <th className="py-2 pr-2 font-medium">Importe original</th>
                        <th className="py-2 pr-2 font-medium">Leyenda</th>
                        <th className="py-2 pr-2 font-medium">Proyecto</th>
                        <th className="py-2 pr-2 font-medium">Etapa</th>
                        <th className="py-2 pr-2 font-medium">Cliente</th>
                        <th className="py-2 pr-2 font-medium">Proveedor</th>
                        <th className="py-2 pr-2 font-medium">Destino de fondos</th>
                        <th className="py-2 pr-2 font-medium"></th>
                      </tr>
                    </thead>
                    <tbody>
                      {lineas.fields.map((campo, indice) => (
                        <LineaAsientoRow key={campo.id} control={form.control} indice={indice} onQuitar={() => lineas.remove(indice)} />
                      ))}
                    </tbody>
                  </table>
                </div>

                <div className="flex items-center gap-4">
                  <Button type="button" variant="outline" size="sm" onClick={agregarLinea}>Agregar línea</Button>
                  <div className="flex gap-4 text-sm">
                    <span>Debe: <strong>{totales.debe.toFixed(2)}</strong></span>
                    <span>Haber: <strong>{totales.haber.toFixed(2)}</strong></span>
                    <span className={totales.diferencia !== 0 ? "font-semibold text-destructive" : "text-muted-foreground"}>
                      Diferencia: {totales.diferencia.toFixed(2)}
                    </span>
                  </div>
                </div>

                <div className="flex gap-2">
                  <Button type="submit" disabled={crear.isPending || editar.isPending}>{editando ? "Guardar borrador" : "Crear borrador"}</Button>
                  <Button type="button" variant="outline" onClick={cancelar}>Cancelar</Button>
                </div>
              </form>
            </Form>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader><CardTitle>Listado</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="flex gap-2">
            <Input placeholder="Buscar por descripción…" value={texto} onChange={(e) => { setTexto(e.target.value); setPage(0) }} className="max-w-xs" />
            <select
              value={estadoFiltro}
              onChange={(e) => { setEstadoFiltro(e.target.value as EstadoAsiento | ""); setPage(0) }}
              className={`${selectClase} max-w-40`}
            >
              <option value="">Todos los estados</option>
              {ESTADOS.map((e) => <option key={e} value={e}>{ESTADO_LABEL[e]}</option>)}
            </select>
          </div>
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

function LineaAsientoRow({ control, indice, onQuitar }: { control: Control<Valores>; indice: number; onQuitar: () => void }) {
  const cuentas = useCuentasContables({ activo: true, page: 0, size: 500 })
  const cuentasImputables = useMemo(
    () => (cuentas.data?.content ?? []).filter((c) => c.imputable),
    [cuentas.data]
  )
  const monedas = useMonedas({ page: 0, size: 20 })
  const proyectos = useProyectos({ activo: true, page: 0, size: 200 })
  const clientes = useClientes({ activo: true, page: 0, size: 200 })
  const proveedores = useProveedores({ activo: true, page: 0, size: 200 })
  const cuentasBancarias = useCuentasBancarias({ activo: true, page: 0, size: 100 })

  const proyectoId = useWatch({ control, name: `lineas.${indice}.proyectoId` })
  const proyectoIdNumero = proyectoId ? Number(proyectoId) : undefined
  const etapas = useEtapas(proyectoIdNumero, { activo: true, page: 0, size: 200 })

  return (
    <tr className="border-b border-border last:border-0 align-top">
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.cuentaContableId`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={cuentas.isLoading} className={`${selectClase} min-w-56`}>
                <option value="">Seleccionar…</option>
                {cuentasImputables.map((c) => <option key={c.id} value={c.id.toString()}>{c.codigo} — {c.nombre}</option>)}
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.debe`} render={({ field }) => (
          <FormItem><FormControl><Input {...field} type="number" step="0.01" className={`${inputClase} w-28`} /></FormControl><FormMessage /></FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.haber`} render={({ field }) => (
          <FormItem><FormControl><Input {...field} type="number" step="0.01" className={`${inputClase} w-28`} /></FormControl><FormMessage /></FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.monedaId`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={monedas.isLoading} className={`${selectClase} w-20`}>
                <option value="">—</option>
                {monedas.data?.content?.map((m) => <option key={m.id} value={m.id.toString()}>{m.codigo}</option>)}
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.tipoCambio`} render={({ field }) => (
          <FormItem><FormControl><Input {...field} type="number" step="0.000001" placeholder="auto" className={`${inputClase} w-24`} /></FormControl><FormMessage /></FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.importeOriginal`} render={({ field }) => (
          <FormItem><FormControl><Input {...field} type="number" step="0.01" className={`${inputClase} w-28`} /></FormControl><FormMessage /></FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.leyenda`} render={({ field }) => (
          <FormItem><FormControl><Input {...field} className={`${inputClase} w-40`} /></FormControl><FormMessage /></FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.proyectoId`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={proyectos.isLoading} className={`${selectClase} min-w-40`}>
                <option value="">Sin proyecto</option>
                {proyectos.data?.content?.map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.etapaId`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={!proyectoIdNumero || etapas.isLoading} className={`${selectClase} min-w-36`}>
                <option value="">Sin etapa</option>
                {etapas.data?.content?.map((e) => <option key={e.id} value={e.id.toString()}>{e.nombre}</option>)}
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.clienteId`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={clientes.isLoading} className={`${selectClase} min-w-36`}>
                <option value="">Sin cliente</option>
                {clientes.data?.content?.map((c) => <option key={c.id} value={c.id.toString()}>{c.nombre}</option>)}
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.proveedorId`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={proveedores.isLoading} className={`${selectClase} min-w-36`}>
                <option value="">Sin proveedor</option>
                {proveedores.data?.content?.map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <FormField control={control} name={`lineas.${indice}.cuentaBancariaId`} render={({ field }) => (
          <FormItem>
            <FormControl>
              <select {...field} disabled={cuentasBancarias.isLoading} className={`${selectClase} min-w-40`}>
                <option value="">Sin destino de fondos</option>
                {cuentasBancarias.data?.content?.map((c) => <option key={c.id} value={c.id.toString()}>{c.alias}</option>)}
              </select>
            </FormControl>
            <FormMessage />
          </FormItem>
        )} />
      </td>
      <td className="py-2 pr-2">
        <Button type="button" variant="outline" size="sm" onClick={onQuitar}>Quitar</Button>
      </td>
    </tr>
  )
}

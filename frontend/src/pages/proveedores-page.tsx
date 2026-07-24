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
  useCambiarEstadoProveedor,
  useCrearProveedor,
  useEditarProveedor,
  useEliminarProveedor,
  useProveedores,
  descargarProveedoresExcel,
  descargarProveedoresPdf,
} from "@/hooks/use-proveedor"
import { useCuentasContables } from "@/hooks/use-cuenta-contable"
import { useJurisdiccions } from "@/hooks/use-jurisdiccion"
import { useMonedas } from "@/hooks/use-monedas"
import { useTipoCostos } from "@/hooks/use-tipocosto"
import { CONDICIONES_IVA, type Proveedor } from "@/types/proveedor"
import { Checkbox } from "@/components/ui/checkbox"

const CONDICION_IVA_LABEL: Record<(typeof CONDICIONES_IVA)[number], string> = {
  RESPONSABLE_INSCRIPTO: "Responsable Inscripto",
  MONOTRIBUTISTA: "Monotributista",
  EXENTO: "Exento",
  CONSUMIDOR_FINAL: "Consumidor Final",
}

const esquema = z.object({
  nombre: z.string().min(1, "El nombre es obligatorio").max(120),
  cuit: z.string().min(1, "El CUIT es obligatorio").regex(/^\d{2}-\d{8}-\d{1}$/, "CUIT debe tener formato XX-XXXXXXXX-X"),
  jurisdiccionId: z.string().min(1, "La jurisdicción es obligatoria"),
  monedaHabitualId: z.string().optional(),
  tiposCostoIds: z.array(z.string()).optional(),
  contacto: z.string().max(100).optional(),
  email: z.union([z.string().email("Email inválido").max(100), z.literal("")]).optional(),
  telefono: z.string().max(20).optional(),
  condicionIva: z.string().min(1, "La condición de IVA es obligatoria"),
  cuentaCxpId: z.string().optional(),
})

type Valores = z.infer<typeof esquema>

export function ProveedoresPage() {
  const [page, setPage] = useState(0)
  const [texto, setTexto] = useState("")
  const [editando, setEditando] = useState<Proveedor | null>(null)
  const [descargando, setDescargando] = useState<"excel" | "pdf" | null>(null)

  const query = useProveedores({ texto, page, size: 10 })
  const jurisdicciones = useJurisdiccions({ page: 0, size: 100 })
  const monedas = useMonedas({ page: 0, size: 100 })
  const tiposCosto = useTipoCostos({ page: 0, size: 100 })
  const cuentasContables = useCuentasContables({ activo: true, page: 0, size: 500 })
  const cuentasImputables = useMemo(() => (cuentasContables.data?.content ?? []).filter((c) => c.imputable), [cuentasContables.data])
  const crear = useCrearProveedor()
  const editar = useEditarProveedor()
  const cambiarEstado = useCambiarEstadoProveedor()
  const eliminar = useEliminarProveedor()

  const VACIO: Valores = {
    nombre: "",
    cuit: "",
    jurisdiccionId: "",
    monedaHabitualId: "",
    tiposCostoIds: [],
    contacto: "",
    email: "",
    telefono: "",
    condicionIva: "RESPONSABLE_INSCRIPTO",
    cuentaCxpId: "",
  }

  const form = useForm<Valores>({
    resolver: zodResolver(esquema),
    defaultValues: VACIO,
  })

  function iniciarEdicion(e: Proveedor) {
    setEditando(e)
    form.reset({
      nombre: e.nombre,
      cuit: e.cuit,
      jurisdiccionId: e.jurisdiccionId.toString(),
      monedaHabitualId: e.monedaHabitualId?.toString() || "",
      tiposCostoIds: e.tiposCosto?.map(tc => tc.id.toString()) || [],
      contacto: e.contacto || "",
      email: e.email || "",
      telefono: e.telefono || "",
      condicionIva: e.condicionIva,
      cuentaCxpId: e.cuentaCxpId?.toString() || "",
    })
  }

  function cancelarEdicion() {
    setEditando(null)
    form.reset(VACIO)
  }

  async function exportar(formato: "excel" | "pdf") {
    setDescargando(formato)
    try {
      if (formato === "excel") await descargarProveedoresExcel({ texto: texto || undefined })
      else await descargarProveedoresPdf({ texto: texto || undefined })
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
          monedaHabitualId: valores.monedaHabitualId ? Number(valores.monedaHabitualId) : undefined,
          tiposCostoIds: valores.tiposCostoIds?.map(Number),
          contacto: valores.contacto,
          email: valores.email,
          telefono: valores.telefono,
          condicionIva: valores.condicionIva as Proveedor["condicionIva"],
          cuentaCxpId: valores.cuentaCxpId ? Number(valores.cuentaCxpId) : undefined,
        }
      }, { onSuccess: cancelarEdicion })
    } else {
      crear.mutate({
        nombre: valores.nombre,
        cuit: valores.cuit,
        jurisdiccionId: Number(valores.jurisdiccionId),
        monedaHabitualId: valores.monedaHabitualId ? Number(valores.monedaHabitualId) : undefined,
        tiposCostoIds: valores.tiposCostoIds?.map(Number),
        contacto: valores.contacto,
        email: valores.email,
        telefono: valores.telefono,
        condicionIva: valores.condicionIva as Proveedor["condicionIva"],
        cuentaCxpId: valores.cuentaCxpId ? Number(valores.cuentaCxpId) : undefined,
      }, { onSuccess: () => form.reset(VACIO) })
    }
  }

  const columnas = useMemo<ColumnDef<Proveedor>[]>(
    () => [
      { header: "CUIT", accessorKey: "cuit" },
      { header: "Nombre", accessorKey: "nombre" },
      { header: "Jurisdicción", accessorKey: "jurisdiccionNombre" },
      { header: "Moneda", accessorKey: "monedaHabitualCodigo" },
      { header: "Condición IVA", accessorKey: "condicionIva", cell: (info) => CONDICION_IVA_LABEL[info.getValue() as keyof typeof CONDICION_IVA_LABEL] },
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
          <h1 className="text-lg font-semibold text-foreground">Proveedores</h1>
          <p className="text-sm text-muted-foreground">Molde PL-1/PL-2 con FKs a Jurisdicción y Moneda, M2M a TipoCosto (F2.3).</p>
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
        <CardHeader><CardTitle>{editando ? `Editar ${editando.nombre}` : "Nuevo proveedor"}</CardTitle></CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="nombre" render={({ field }) => (
                  <FormItem><FormLabel>Nombre</FormLabel><FormControl><Input {...field} placeholder="Acme Supplies" /></FormControl><FormMessage /></FormItem>
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
              </div>

              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="monedaHabitualId" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Moneda habitual</FormLabel>
                    <FormControl>
                      <select {...field} disabled={monedas.isLoading} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        <option value="">Sin especificar</option>
                        {monedas.data?.content?.map((m) => (
                          <option key={m.id} value={m.id.toString()}>{m.codigo} - {m.nombre}</option>
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
              </div>

              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="telefono" render={({ field }) => (
                  <FormItem><FormLabel>Teléfono</FormLabel><FormControl><Input {...field} placeholder="1123456789" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="condicionIva" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Condición de IVA</FormLabel>
                    <FormControl>
                      <select {...field} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        {CONDICIONES_IVA.map((c) => (
                          <option key={c} value={c}>{CONDICION_IVA_LABEL[c]}</option>
                        ))}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="cuentaCxpId" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Cuenta de deudas comerciales</FormLabel>
                    <FormControl>
                      <select {...field} disabled={cuentasContables.isLoading} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        <option value="">Sin cuenta propia (usa el mapeo por defecto)</option>
                        {cuentasImputables.map((c) => (
                          <option key={c.id} value={c.id.toString()}>{c.codigo} — {c.nombre}</option>
                        ))}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
              </div>

              <FormItem>
                <FormLabel>Tipos de costo</FormLabel>
                <div className="grid gap-2 sm:grid-cols-3">
                  {tiposCosto.data?.content?.map((tc) => (
                    <div key={tc.id} className="flex items-center space-x-2">
                      <Checkbox
                        id={`tc-${tc.id}`}
                        checked={form.watch("tiposCostoIds")?.includes(tc.id.toString()) || false}
                        onCheckedChange={(checked) => {
                          const current = form.watch("tiposCostoIds") || []
                          if (checked) {
                            form.setValue("tiposCostoIds", [...current, tc.id.toString()])
                          } else {
                            form.setValue("tiposCostoIds", current.filter(id => id !== tc.id.toString()))
                          }
                        }}
                      />
                      <label htmlFor={`tc-${tc.id}`} className="text-sm">{tc.nombre}</label>
                    </div>
                  ))}
                </div>
              </FormItem>

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

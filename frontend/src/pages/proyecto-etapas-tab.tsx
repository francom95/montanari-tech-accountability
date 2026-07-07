import { zodResolver } from "@hookform/resolvers/zod"
import { useMemo, useRef, useState } from "react"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import {
  useCambiarEstadoEtapa,
  useConfirmarImportacionEtapas,
  useCrearEtapa,
  useEditarEtapa,
  useEliminarEtapa,
  useEtapas,
  usePrevisualizarImportacionEtapas,
} from "@/hooks/use-etapa"
import { useProveedores } from "@/hooks/use-proveedor"
import type { Etapa, EtapaCrearInput, EstadoEtapa, EtapaImportFila } from "@/types/etapa"

const ESTADOS_ETAPA = ["PENDIENTE", "EN_CURSO", "FINALIZADA", "CANCELADA"] as const

const esquema = z.object({
  nombre: z.string().min(1, "El nombre es obligatorio").max(160),
  descripcion: z.string().optional(),
  estado: z.string().optional(),
  fechaInicio: z.string().optional(),
  fechaEstimadaFin: z.string().optional(),
  porcentajeAvance: z.string().optional(),
  montoPresupuestado: z.string().optional(),
  costosEstimados: z.string().optional(),
  pagosPrevistos: z.string().optional(),
  cobrosPrevistos: z.string().optional(),
  observaciones: z.string().optional(),
  proveedoresIds: z.array(z.string()).optional(),
})

type Valores = z.infer<typeof esquema>

const VALORES_INICIALES: Valores = {
  nombre: "", descripcion: "", estado: "PENDIENTE", fechaInicio: "", fechaEstimadaFin: "",
  porcentajeAvance: "", montoPresupuestado: "", costosEstimados: "", pagosPrevistos: "", cobrosPrevistos: "",
  observaciones: "", proveedoresIds: [],
}

export function EtapasTab({ proyectoId }: { proyectoId: number }) {
  const [editando, setEditando] = useState<Etapa | null>(null)
  const etapas = useEtapas(proyectoId, { size: 50 })
  const proveedores = useProveedores({ page: 0, size: 100 })
  const crear = useCrearEtapa(proyectoId)
  const editar = useEditarEtapa(proyectoId)
  const cambiarEstado = useCambiarEstadoEtapa(proyectoId)
  const eliminar = useEliminarEtapa(proyectoId)

  const form = useForm<Valores>({ resolver: zodResolver(esquema), defaultValues: VALORES_INICIALES })

  function iniciarEdicion(e: Etapa) {
    setEditando(e)
    form.reset({
      nombre: e.nombre,
      descripcion: e.descripcion || "",
      estado: e.estado,
      fechaInicio: e.fechaInicio || "",
      fechaEstimadaFin: e.fechaEstimadaFin || "",
      porcentajeAvance: e.porcentajeAvance?.toString() || "",
      montoPresupuestado: e.montoPresupuestado?.toString() || "",
      costosEstimados: e.costosEstimados?.toString() || "",
      pagosPrevistos: e.pagosPrevistos?.toString() || "",
      cobrosPrevistos: e.cobrosPrevistos?.toString() || "",
      observaciones: e.observaciones || "",
      proveedoresIds: e.proveedores.map((p) => p.id.toString()),
    })
  }

  function cancelarEdicion() {
    setEditando(null)
    form.reset(VALORES_INICIALES)
  }

  function aInput(v: Valores): EtapaCrearInput {
    return {
      nombre: v.nombre,
      descripcion: v.descripcion || undefined,
      estado: (v.estado || undefined) as EstadoEtapa | undefined,
      fechaInicio: v.fechaInicio || undefined,
      fechaEstimadaFin: v.fechaEstimadaFin || undefined,
      porcentajeAvance: v.porcentajeAvance ? Number(v.porcentajeAvance) : undefined,
      montoPresupuestado: v.montoPresupuestado ? Number(v.montoPresupuestado) : undefined,
      costosEstimados: v.costosEstimados ? Number(v.costosEstimados) : undefined,
      pagosPrevistos: v.pagosPrevistos ? Number(v.pagosPrevistos) : undefined,
      cobrosPrevistos: v.cobrosPrevistos ? Number(v.cobrosPrevistos) : undefined,
      observaciones: v.observaciones || undefined,
      proveedoresIds: v.proveedoresIds?.map(Number),
    }
  }

  function onSubmit(valores: Valores) {
    if (editando) {
      editar.mutate({ id: editando.id, valores: aInput(valores) }, { onSuccess: cancelarEdicion })
    } else {
      crear.mutate(aInput(valores), { onSuccess: () => form.reset(VALORES_INICIALES) })
    }
  }

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader><CardTitle>{editando ? `Editar ${editando.nombre}` : "Nueva etapa"}</CardTitle></CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="nombre" render={({ field }) => (
                  <FormItem><FormLabel>Nombre</FormLabel><FormControl><Input {...field} placeholder="Diseño UX" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="estado" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Estado</FormLabel>
                    <FormControl>
                      <select {...field} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        {ESTADOS_ETAPA.map((e) => <option key={e} value={e}>{e}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="porcentajeAvance" render={({ field }) => (
                  <FormItem><FormLabel>% de avance</FormLabel><FormControl><Input {...field} type="number" min={0} max={100} placeholder="0" /></FormControl><FormMessage /></FormItem>
                )} />
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <FormField control={form.control} name="fechaInicio" render={({ field }) => (
                  <FormItem><FormLabel>Fecha de inicio</FormLabel><FormControl><Input {...field} type="date" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="fechaEstimadaFin" render={({ field }) => (
                  <FormItem><FormLabel>Fecha estimada de fin</FormLabel><FormControl><Input {...field} type="date" /></FormControl><FormMessage /></FormItem>
                )} />
              </div>

              <div className="grid gap-4 sm:grid-cols-4">
                <FormField control={form.control} name="montoPresupuestado" render={({ field }) => (
                  <FormItem><FormLabel>Monto presupuestado</FormLabel><FormControl><Input {...field} type="number" step="0.01" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="costosEstimados" render={({ field }) => (
                  <FormItem><FormLabel>Costos estimados</FormLabel><FormControl><Input {...field} type="number" step="0.01" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="pagosPrevistos" render={({ field }) => (
                  <FormItem><FormLabel>Pagos previstos</FormLabel><FormControl><Input {...field} type="number" step="0.01" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="cobrosPrevistos" render={({ field }) => (
                  <FormItem><FormLabel>Cobros previstos</FormLabel><FormControl><Input {...field} type="number" step="0.01" /></FormControl><FormMessage /></FormItem>
                )} />
              </div>

              <FormField control={form.control} name="descripcion" render={({ field }) => (
                <FormItem><FormLabel>Descripción</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="observaciones" render={({ field }) => (
                <FormItem><FormLabel>Observaciones</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
              )} />

              <FormItem>
                <FormLabel>Proveedores asociados</FormLabel>
                <div className="grid gap-2 sm:grid-cols-3">
                  {proveedores.data?.content?.map((pr) => (
                    <div key={pr.id} className="flex items-center space-x-2">
                      <Checkbox
                        id={`prov-${pr.id}`}
                        checked={form.watch("proveedoresIds")?.includes(pr.id.toString()) || false}
                        onCheckedChange={(checked) => {
                          const actual = form.watch("proveedoresIds") || []
                          form.setValue("proveedoresIds", checked ? [...actual, pr.id.toString()] : actual.filter((id) => id !== pr.id.toString()))
                        }}
                      />
                      <label htmlFor={`prov-${pr.id}`} className="text-sm">{pr.nombre}</label>
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
        <CardHeader><CardTitle>Etapas del proyecto</CardTitle></CardHeader>
        <CardContent>
          {etapas.isLoading ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground">
                <tr className="border-b border-border">
                  <th className="py-2 pr-4 font-medium">Nombre</th>
                  <th className="py-2 pr-4 font-medium">Estado</th>
                  <th className="py-2 pr-4 font-medium">% avance</th>
                  <th className="py-2 pr-4 font-medium">Proveedores</th>
                  <th className="py-2 pr-4 font-medium">Activa</th>
                  <th className="py-2 pr-4 font-medium">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {etapas.data?.content?.map((e) => (
                  <tr key={e.id} className="border-b border-border last:border-0">
                    <td className="py-2 pr-4">{e.nombre}</td>
                    <td className="py-2 pr-4">{e.estado}</td>
                    <td className="py-2 pr-4">{e.porcentajeAvance ?? "-"}</td>
                    <td className="py-2 pr-4">{e.proveedores.map((p) => p.nombre).join(", ") || "-"}</td>
                    <td className="py-2 pr-4">{e.activo ? "Sí" : "No"}</td>
                    <td className="py-2 pr-4">
                      <div className="flex gap-2">
                        <Button variant="outline" size="sm" onClick={() => iniciarEdicion(e)}>Editar</Button>
                        <Button variant="outline" size="sm" disabled={cambiarEstado.isPending} onClick={() => cambiarEstado.mutate({ id: e.id, activo: e.activo })}>
                          {e.activo ? "Desactivar" : "Activar"}
                        </Button>
                        <Button variant="outline" size="sm" disabled={eliminar.isPending} onClick={() => eliminar.mutate(e.id)}>Eliminar</Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>

      <ImportarEtapas proyectoId={proyectoId} />
    </div>
  )
}

function ImportarEtapas({ proyectoId }: { proyectoId: number }) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [filas, setFilas] = useState<EtapaImportFila[] | null>(null)
  const previsualizar = usePrevisualizarImportacionEtapas(proyectoId)
  const confirmar = useConfirmarImportacionEtapas(proyectoId)

  const filasValidas = useMemo(() => filas?.filter((f) => f.errores.length === 0).length ?? 0, [filas])

  function onSeleccionarArchivo(e: React.ChangeEvent<HTMLInputElement>) {
    const archivo = e.target.files?.[0]
    if (!archivo) return
    setFilas(null)
    previsualizar.mutate(archivo, { onSuccess: setFilas })
  }

  function quitarFila(fila: number) {
    setFilas((actual) => actual?.filter((f) => f.fila !== fila) ?? null)
  }

  function confirmarImportacion() {
    if (!filas) return
    confirmar.mutate(filas.filter((f) => f.errores.length === 0), {
      onSuccess: () => {
        setFilas(null)
        if (inputRef.current) inputRef.current.value = ""
      },
    })
  }

  return (
    <Card>
      <CardHeader><CardTitle>Importar etapas desde Excel/CSV</CardTitle></CardHeader>
      <CardContent className="space-y-4">
        <p className="text-sm text-muted-foreground">
          Columnas esperadas (con encabezado): nombre, descripción, estado, fecha inicio (dd/MM/yyyy),
          fecha estimada fin (dd/MM/yyyy), % avance, monto presupuestado, costos estimados, pagos previstos,
          cobros previstos, observaciones, proveedores (nombres separados por ";").
        </p>
        <input ref={inputRef} type="file" accept=".xlsx,.xls,.csv" onChange={onSeleccionarArchivo} className="text-sm" />
        {previsualizar.isPending && <p className="text-sm text-muted-foreground">Procesando archivo…</p>}

        {filas && (
          <div className="space-y-4">
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground">
                <tr className="border-b border-border">
                  <th className="py-2 pr-4 font-medium">Fila</th>
                  <th className="py-2 pr-4 font-medium">Nombre</th>
                  <th className="py-2 pr-4 font-medium">Estado</th>
                  <th className="py-2 pr-4 font-medium">Fecha inicio</th>
                  <th className="py-2 pr-4 font-medium">Proveedores</th>
                  <th className="py-2 pr-4 font-medium">Errores</th>
                  <th className="py-2 pr-4 font-medium"></th>
                </tr>
              </thead>
              <tbody>
                {filas.map((f) => (
                  <tr key={f.fila} className={`border-b border-border last:border-0 ${f.errores.length > 0 ? "bg-destructive/10" : ""}`}>
                    <td className="py-2 pr-4">{f.fila}</td>
                    <td className="py-2 pr-4">{f.nombre || "-"}</td>
                    <td className="py-2 pr-4">{f.estado || "-"}</td>
                    <td className="py-2 pr-4">{f.fechaInicio || "-"}</td>
                    <td className="py-2 pr-4">{f.proveedoresNombres.join(", ") || "-"}</td>
                    <td className="py-2 pr-4 text-destructive">{f.errores.join("; ")}</td>
                    <td className="py-2 pr-4"><Button type="button" variant="outline" size="sm" onClick={() => quitarFila(f.fila)}>Quitar</Button></td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="flex items-center gap-2">
              <Button type="button" disabled={filasValidas === 0 || confirmar.isPending} onClick={confirmarImportacion}>
                Confirmar importación ({filasValidas} válidas)
              </Button>
              <Button type="button" variant="outline" onClick={() => setFilas(null)}>Cancelar</Button>
            </div>
            {confirmar.data && (
              <p className="text-sm text-muted-foreground">
                Creadas: {confirmar.data.creadas.length} · Rechazadas: {confirmar.data.rechazadas.length}
              </p>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

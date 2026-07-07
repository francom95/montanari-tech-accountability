import { zodResolver } from "@hookform/resolvers/zod"
import { useState } from "react"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useComisionistas } from "@/hooks/use-comisionista"
import {
  useCambiarEstadoComisionProyecto,
  useComisionesDeProyecto,
  useCrearComisionProyecto,
  useEditarComisionProyecto,
  useEliminarComisionProyecto,
} from "@/hooks/use-comision-proyecto"
import { useMonedas } from "@/hooks/use-monedas"
import type { BaseCalculo, ComisionProyecto, ComisionProyectoCrearInput, EstadoPagoComision } from "@/types/comision-proyecto"

const BASES_CALCULO: BaseCalculo[] = ["MONTO_TOTAL", "MONTO_SIN_IMPUESTOS", "MONTO_COBRADO"]
const ESTADOS_PAGO: EstadoPagoComision[] = ["PENDIENTE", "PAGADO"]

const esquema = z.object({
  comisionistaId: z.string().min(1, "El comisionista es obligatorio"),
  porcentajeComision: z.string().min(1, "El % es obligatorio"),
  baseCalculo: z.string().min(1),
  monedaId: z.string().min(1, "La moneda es obligatoria"),
  importeFinal: z.string().optional(),
  estadoPago: z.string().optional(),
  fechaEstimadaPago: z.string().optional(),
  observaciones: z.string().optional(),
})

type Valores = z.infer<typeof esquema>

const VALORES_INICIALES: Valores = {
  comisionistaId: "", porcentajeComision: "10", baseCalculo: "MONTO_TOTAL", monedaId: "",
  importeFinal: "", estadoPago: "PENDIENTE", fechaEstimadaPago: "", observaciones: "",
}

export function ComisionesTab({ proyectoId }: { proyectoId: number }) {
  const [editando, setEditando] = useState<ComisionProyecto | null>(null)
  const comisiones = useComisionesDeProyecto(proyectoId, { size: 50 })
  const comisionistas = useComisionistas({ page: 0, size: 100 })
  const monedas = useMonedas({ page: 0, size: 100 })
  const crear = useCrearComisionProyecto(proyectoId)
  const editar = useEditarComisionProyecto(proyectoId)
  const cambiarEstado = useCambiarEstadoComisionProyecto(proyectoId)
  const eliminar = useEliminarComisionProyecto(proyectoId)

  const form = useForm<Valores>({ resolver: zodResolver(esquema), defaultValues: VALORES_INICIALES })

  function iniciarEdicion(c: ComisionProyecto) {
    setEditando(c)
    form.reset({
      comisionistaId: c.comisionistaId.toString(),
      porcentajeComision: c.porcentajeComision.toString(),
      baseCalculo: c.baseCalculo,
      monedaId: c.monedaId.toString(),
      importeFinal: c.importeFinal?.toString() || "",
      estadoPago: c.estadoPago,
      fechaEstimadaPago: c.fechaEstimadaPago || "",
      observaciones: c.observaciones || "",
    })
  }

  function cancelarEdicion() {
    setEditando(null)
    form.reset(VALORES_INICIALES)
  }

  function onSubmit(valores: Valores) {
    const base: ComisionProyectoCrearInput = {
      comisionistaId: Number(valores.comisionistaId),
      porcentajeComision: Number(valores.porcentajeComision),
      baseCalculo: valores.baseCalculo as BaseCalculo,
      monedaId: Number(valores.monedaId),
      fechaEstimadaPago: valores.fechaEstimadaPago || undefined,
      observaciones: valores.observaciones || undefined,
    }
    if (editando) {
      editar.mutate({
        id: editando.id,
        valores: {
          ...base,
          importeFinal: valores.importeFinal ? Number(valores.importeFinal) : undefined,
          estadoPago: (valores.estadoPago || undefined) as EstadoPagoComision | undefined,
        },
      }, { onSuccess: cancelarEdicion })
    } else {
      crear.mutate(base, { onSuccess: () => form.reset(VALORES_INICIALES) })
    }
  }

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader><CardTitle>{editando ? "Editar comisión" : "Nueva comisión"}</CardTitle></CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="comisionistaId" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Comisionista</FormLabel>
                    <FormControl>
                      <select {...field} disabled={comisionistas.isLoading} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        <option value="">Seleccionar</option>
                        {comisionistas.data?.content?.map((c) => (
                          <option key={c.id} value={c.id.toString()}>{c.nombre}</option>
                        ))}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="porcentajeComision" render={({ field }) => (
                  <FormItem><FormLabel>% de comisión</FormLabel><FormControl><Input {...field} type="number" step="0.01" min={0} max={100} /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="baseCalculo" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Base de cálculo</FormLabel>
                    <FormControl>
                      <select {...field} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        {BASES_CALCULO.map((b) => <option key={b} value={b}>{b}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
              </div>

              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="monedaId" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Moneda</FormLabel>
                    <FormControl>
                      <select {...field} disabled={monedas.isLoading} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        <option value="">Seleccionar</option>
                        {monedas.data?.content?.map((m) => (
                          <option key={m.id} value={m.id.toString()}>{m.codigo}</option>
                        ))}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="fechaEstimadaPago" render={({ field }) => (
                  <FormItem><FormLabel>Fecha estimada de pago</FormLabel><FormControl><Input {...field} type="date" /></FormControl><FormMessage /></FormItem>
                )} />
                {editando && (
                  <FormField control={form.control} name="estadoPago" render={({ field }) => (
                    <FormItem>
                      <FormLabel>Estado de pago</FormLabel>
                      <FormControl>
                        <select {...field} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                          {ESTADOS_PAGO.map((e) => <option key={e} value={e}>{e}</option>)}
                        </select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )} />
                )}
              </div>

              {editando && (
                <FormField control={form.control} name="importeFinal" render={({ field }) => (
                  <FormItem><FormLabel>Importe final</FormLabel><FormControl><Input {...field} type="number" step="0.01" /></FormControl><FormMessage /></FormItem>
                )} />
              )}

              <FormField control={form.control} name="observaciones" render={({ field }) => (
                <FormItem><FormLabel>Observaciones</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
              )} />

              <div className="flex items-center gap-2">
                <Button type="submit" disabled={crear.isPending || editar.isPending}>{editando ? "Guardar" : "Crear"}</Button>
                {editando && <Button type="button" variant="outline" onClick={cancelarEdicion}>Cancelar</Button>}
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>Comisiones del proyecto</CardTitle></CardHeader>
        <CardContent>
          {comisiones.isLoading ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground">
                <tr className="border-b border-border">
                  <th className="py-2 pr-4 font-medium">Comisionista</th>
                  <th className="py-2 pr-4 font-medium">%</th>
                  <th className="py-2 pr-4 font-medium">Base</th>
                  <th className="py-2 pr-4 font-medium">Estimado</th>
                  <th className="py-2 pr-4 font-medium">Final</th>
                  <th className="py-2 pr-4 font-medium">Estado</th>
                  <th className="py-2 pr-4 font-medium">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {comisiones.data?.content?.map((c) => (
                  <tr key={c.id} className="border-b border-border last:border-0">
                    <td className="py-2 pr-4">{c.comisionistaNombre}</td>
                    <td className="py-2 pr-4">{c.porcentajeComision}%</td>
                    <td className="py-2 pr-4">{c.baseCalculo}</td>
                    <td className="py-2 pr-4">{c.monedaCodigo} {c.importeEstimado}</td>
                    <td className="py-2 pr-4">{c.importeFinal ?? "-"}</td>
                    <td className="py-2 pr-4">{c.estadoPago}</td>
                    <td className="py-2 pr-4">
                      <div className="flex gap-2">
                        <Button variant="outline" size="sm" onClick={() => iniciarEdicion(c)}>Editar</Button>
                        <Button variant="outline" size="sm" disabled={cambiarEstado.isPending} onClick={() => cambiarEstado.mutate({ id: c.id, activo: c.activo })}>
                          {c.activo ? "Desactivar" : "Activar"}
                        </Button>
                        <Button variant="outline" size="sm" disabled={eliminar.isPending} onClick={() => eliminar.mutate(c.id)}>Eliminar</Button>
                      </div>
                    </td>
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

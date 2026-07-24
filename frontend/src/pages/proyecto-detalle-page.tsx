import { zodResolver } from "@hookform/resolvers/zod"
import { useEffect, useRef, useState } from "react"
import { useFieldArray, useForm } from "react-hook-form"
import { Link, useParams } from "react-router-dom"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useClientes } from "@/hooks/use-cliente"
import { useMonedas } from "@/hooks/use-monedas"
import { useEditarProyecto, useProyecto } from "@/hooks/use-proyecto"
import { useUsuarios } from "@/hooks/use-usuario"
import type { Proyecto } from "@/types/proyecto"
import { ComisionesTab } from "@/pages/proyecto-comisiones-tab"
import { EtapasTab } from "@/pages/proyecto-etapas-tab"
import { PresupuestoTab } from "@/pages/proyecto-presupuesto-tab"
import { RentabilidadTab } from "@/pages/proyecto-rentabilidad-tab"

const ESTADOS_PROYECTO = ["PROSPECTO", "EN_CURSO", "PAUSADO", "FINALIZADO", "CANCELADO"] as const
const TIPOS_PROYECTO = ["ARGENTINA", "EXTERIOR"] as const
const TIPO_PROYECTO_LABEL: Record<string, string> = { ARGENTINA: "Argentina", EXTERIOR: "Exterior" }
const ESTADOS_COMERCIALES = ["PROSPECTO", "EN_NEGOCIACION", "GANADO", "PERDIDO"] as const
const ESTADOS_FACTURACION = ["NO_FACTURADO", "PARCIALMENTE_FACTURADO", "FACTURADO_TOTAL"] as const
const ESTADOS_COBRANZA = ["PENDIENTE", "PARCIAL", "COBRADO_TOTAL"] as const

const esquemaCuota = z.object({ fechaEstimadaCobro: z.string().min(1, "Obligatoria"), importe: z.string().min(1, "Obligatorio") })

const esquema = z.object({
  nombre: z.string().min(1, "El nombre es obligatorio").max(160),
  clienteId: z.string().min(1, "El cliente es obligatorio"),
  responsableId: z.string().optional(),
  pais: z.string().optional(),
  tipoProyecto: z.string().optional(),
  estado: z.string(),
  monedaId: z.string().min(1, "La moneda es obligatoria"),
  montoTotal: z.string().min(1, "El monto total es obligatorio"),
  cantidadPagosPactados: z.string().optional(),
  comentarios: z.string().optional(),
  estadoComercial: z.string(),
  estadoFacturacion: z.string(),
  estadoCobranza: z.string(),
  fechaEstimadaFinalizacion: z.string().optional(),
  fechaRealFinalizacion: z.string().optional(),
  cuotas: z.array(esquemaCuota),
})

type Valores = z.infer<typeof esquema>

function proyectoAValores(p: Proyecto): Valores {
  return {
    nombre: p.nombre,
    clienteId: p.clienteId.toString(),
    responsableId: p.responsableId?.toString() || "",
    pais: p.pais || "",
    tipoProyecto: p.tipoProyecto || "",
    estado: p.estado,
    monedaId: p.monedaId.toString(),
    montoTotal: p.montoTotal.toString(),
    cantidadPagosPactados: p.cantidadPagosPactados?.toString() || "",
    comentarios: p.comentarios || "",
    estadoComercial: p.estadoComercial,
    estadoFacturacion: p.estadoFacturacion,
    estadoCobranza: p.estadoCobranza,
    fechaEstimadaFinalizacion: p.fechaEstimadaFinalizacion || "",
    fechaRealFinalizacion: p.fechaRealFinalizacion || "",
    cuotas: p.cuotas.map((c) => ({ fechaEstimadaCobro: c.fechaEstimadaCobro, importe: c.importe.toString() })),
  }
}

type Pestaña = "datos" | "cuotas" | "etapas" | "comisiones" | "presupuesto" | "reporte"

export function ProyectoDetallePage() {
  const { id } = useParams()
  const proyectoId = Number(id)
  const [pestaña, setPestaña] = useState<Pestaña>("datos")
  const proyecto = useProyecto(proyectoId)

  return (
    <div className="space-y-6">
      <div>
        <Link to="/proyectos" className="text-sm text-muted-foreground hover:underline">← Volver a proyectos</Link>
        <h1 className="text-lg font-semibold text-foreground">{proyecto.data?.nombre ?? "Proyecto"}</h1>
        <p className="text-sm text-muted-foreground">Ficha de proyecto: datos, cuotas, etapas y comisiones (F2.5/F2.7).</p>
      </div>

      <div className="flex gap-2 border-b border-border">
        {([
          ["datos", "Datos"],
          ["cuotas", "Cuotas"],
          ["etapas", "Etapas"],
          ["comisiones", "Comisiones"],
          ["presupuesto", "Presupuesto"],
          ["reporte", "Reporte"],
        ] as [Pestaña, string][]).map(([clave, etiqueta]) => (
          <button
            key={clave}
            type="button"
            onClick={() => setPestaña(clave)}
            className={`px-3 py-2 text-sm font-medium ${pestaña === clave ? "border-b-2 border-primary text-foreground" : "text-muted-foreground"}`}
          >
            {etiqueta}
          </button>
        ))}
      </div>

      {proyecto.isLoading || !proyecto.data ? (
        <p className="text-sm text-muted-foreground">Cargando…</p>
      ) : pestaña === "datos" || pestaña === "cuotas" ? (
        <FichaProyectoForm proyecto={proyecto.data} pestaña={pestaña} />
      ) : pestaña === "etapas" ? (
        <EtapasTab proyectoId={proyectoId} />
      ) : pestaña === "comisiones" ? (
        <ComisionesTab proyectoId={proyectoId} />
      ) : pestaña === "presupuesto" ? (
        <PresupuestoTab proyectoId={proyectoId} tipoProyecto={proyecto.data.tipoProyecto} />
      ) : (
        <RentabilidadTab proyectoId={proyectoId} />
      )}
    </div>
  )
}

function FichaProyectoForm({ proyecto, pestaña }: { proyecto: Proyecto; pestaña: "datos" | "cuotas" }) {
  const clientes = useClientes({ page: 0, size: 100 })
  const usuarios = useUsuarios({ page: 0, size: 100 })
  const monedas = useMonedas({ page: 0, size: 100 })
  const editar = useEditarProyecto()

  const form = useForm<Valores>({ resolver: zodResolver(esquema), defaultValues: proyectoAValores(proyecto) })
  const cuotas = useFieldArray({ control: form.control, name: "cuotas" })

  const inicializadoParaId = useRef<number | null>(null)
  useEffect(() => {
    if (inicializadoParaId.current !== proyecto.id) {
      form.reset(proyectoAValores(proyecto))
      inicializadoParaId.current = proyecto.id
    }
  }, [proyecto, form])

  function onSubmit(valores: Valores) {
    editar.mutate({
      id: proyecto.id,
      valores: {
        nombre: valores.nombre,
        clienteId: Number(valores.clienteId),
        responsableId: valores.responsableId ? Number(valores.responsableId) : undefined,
        pais: valores.pais || undefined,
        tipoProyecto: valores.tipoProyecto || undefined,
        estado: valores.estado as Proyecto["estado"],
        monedaId: Number(valores.monedaId),
        montoTotal: Number(valores.montoTotal),
        cantidadPagosPactados: valores.cantidadPagosPactados ? Number(valores.cantidadPagosPactados) : undefined,
        comentarios: valores.comentarios || undefined,
        estadoComercial: valores.estadoComercial as Proyecto["estadoComercial"],
        estadoFacturacion: valores.estadoFacturacion as Proyecto["estadoFacturacion"],
        estadoCobranza: valores.estadoCobranza as Proyecto["estadoCobranza"],
        fechaEstimadaFinalizacion: valores.fechaEstimadaFinalizacion || undefined,
        fechaRealFinalizacion: valores.fechaRealFinalizacion || undefined,
        cuotas: valores.cuotas.map((c) => ({ fechaEstimadaCobro: c.fechaEstimadaCobro, importe: Number(c.importe) })),
      },
    })
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        {pestaña === "datos" && (
          <Card>
            <CardHeader><CardTitle>Datos del proyecto</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="nombre" render={({ field }) => (
                  <FormItem><FormLabel>Nombre</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="clienteId" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Cliente</FormLabel>
                    <FormControl>
                      <select {...field} disabled={clientes.isLoading} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        {clientes.data?.content?.map((c) => <option key={c.id} value={c.id.toString()}>{c.nombre}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="responsableId" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Responsable</FormLabel>
                    <FormControl>
                      <select {...field} disabled={usuarios.isLoading} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        <option value="">Sin asignar</option>
                        {usuarios.data?.content?.map((u) => <option key={u.id} value={u.id.toString()}>{u.nombre}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
              </div>

              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="pais" render={({ field }) => (
                  <FormItem><FormLabel>País</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="tipoProyecto" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Tipo de proyecto</FormLabel>
                    <FormControl>
                      <select {...field} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        {TIPOS_PROYECTO.map((t) => <option key={t} value={t}>{TIPO_PROYECTO_LABEL[t]}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="estado" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Estado</FormLabel>
                    <FormControl>
                      <select {...field} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        {ESTADOS_PROYECTO.map((e) => <option key={e} value={e}>{e}</option>)}
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
                        {monedas.data?.content?.map((m) => <option key={m.id} value={m.id.toString()}>{m.codigo}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="montoTotal" render={({ field }) => (
                  <FormItem><FormLabel>Monto total</FormLabel><FormControl><Input {...field} type="number" step="0.01" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="cantidadPagosPactados" render={({ field }) => (
                  <FormItem><FormLabel>Cantidad de pagos pactados</FormLabel><FormControl><Input {...field} type="number" /></FormControl><FormMessage /></FormItem>
                )} />
              </div>

              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="estadoComercial" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Estado comercial</FormLabel>
                    <FormControl>
                      <select {...field} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        {ESTADOS_COMERCIALES.map((e) => <option key={e} value={e}>{e}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="estadoFacturacion" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Estado de facturación</FormLabel>
                    <FormControl>
                      <select {...field} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        {ESTADOS_FACTURACION.map((e) => <option key={e} value={e}>{e}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="estadoCobranza" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Estado de cobranza</FormLabel>
                    <FormControl>
                      <select {...field} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        {ESTADOS_COBRANZA.map((e) => <option key={e} value={e}>{e}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <FormField control={form.control} name="fechaEstimadaFinalizacion" render={({ field }) => (
                  <FormItem><FormLabel>Fecha estimada de finalización</FormLabel><FormControl><Input {...field} type="date" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="fechaRealFinalizacion" render={({ field }) => (
                  <FormItem><FormLabel>Fecha real de finalización</FormLabel><FormControl><Input {...field} type="date" /></FormControl><FormMessage /></FormItem>
                )} />
              </div>

              <FormField control={form.control} name="comentarios" render={({ field }) => (
                <FormItem><FormLabel>Comentarios</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
              )} />
            </CardContent>
          </Card>
        )}

        {pestaña === "cuotas" && (
          <Card>
            <CardHeader><CardTitle>Cuotas pactadas de cobro</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr className="border-b border-border">
                    <th className="py-2 pr-4 font-medium">#</th>
                    <th className="py-2 pr-4 font-medium">Fecha estimada de cobro</th>
                    <th className="py-2 pr-4 font-medium">Importe</th>
                    <th className="py-2 pr-4 font-medium"></th>
                  </tr>
                </thead>
                <tbody>
                  {cuotas.fields.map((campo, indice) => (
                    <tr key={campo.id} className="border-b border-border last:border-0">
                      <td className="py-2 pr-4">{indice + 1}</td>
                      <td className="py-2 pr-4">
                        <FormField control={form.control} name={`cuotas.${indice}.fechaEstimadaCobro`} render={({ field }) => (
                          <FormItem><FormControl><Input {...field} type="date" /></FormControl><FormMessage /></FormItem>
                        )} />
                      </td>
                      <td className="py-2 pr-4">
                        <FormField control={form.control} name={`cuotas.${indice}.importe`} render={({ field }) => (
                          <FormItem><FormControl><Input {...field} type="number" step="0.01" /></FormControl><FormMessage /></FormItem>
                        )} />
                      </td>
                      <td className="py-2 pr-4">
                        <Button type="button" variant="outline" size="sm" onClick={() => cuotas.remove(indice)}>Quitar</Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <Button type="button" variant="outline" size="sm" onClick={() => cuotas.append({ fechaEstimadaCobro: "", importe: "" })}>
                Agregar cuota
              </Button>
            </CardContent>
          </Card>
        )}

        <Button type="submit" disabled={editar.isPending}>Guardar cambios</Button>
      </form>
    </Form>
  )
}

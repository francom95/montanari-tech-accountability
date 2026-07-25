import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { useParams } from "react-router-dom"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useInversion } from "@/hooks/use-inversion"
import { useCrearMovimientoInversion, useMovimientosInversion } from "@/hooks/use-movimiento-inversion"
import { TIPOS_MOVIMIENTO_INVERSION, type TipoMovimientoInversion } from "@/types/movimiento-inversion"

const selectClase = "h-8 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
const pesos = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" })
const TIPO_LABEL: Record<TipoMovimientoInversion, string> = { SUSCRIPCION: "Suscripción", RESCATE: "Rescate" }

const esquema = z.object({
  tipo: z.enum(TIPOS_MOVIMIENTO_INVERSION as [TipoMovimientoInversion, ...TipoMovimientoInversion[]]),
  fecha: z.string().min(1, "La fecha es obligatoria"),
  montoAplicado: z.string().min(1, "El monto es obligatorio"),
  cuotapartes: z.string().min(1, "Las cuotapartes son obligatorias"),
  valorCuotaparte: z.string().min(1, "El valor de cuotaparte es obligatorio"),
  fechaLiquidacion: z.string().optional(),
  observaciones: z.string().optional(),
})

type Valores = z.infer<typeof esquema>

const VACIO: Valores = { tipo: "SUSCRIPCION", fecha: "", montoAplicado: "", cuotapartes: "", valorCuotaparte: "", fechaLiquidacion: "", observaciones: "" }

export function InversionDetallePage() {
  const { id } = useParams<{ id: string }>()
  const inversionId = id ? Number(id) : undefined

  const inversionQuery = useInversion(inversionId)
  const movimientosQuery = useMovimientosInversion(inversionId)
  const crear = useCrearMovimientoInversion()

  const form = useForm<Valores>({ resolver: zodResolver(esquema), defaultValues: VACIO })

  function onSubmit(valores: Valores) {
    if (!inversionId) return
    crear.mutate(
      {
        inversionId,
        tipo: valores.tipo,
        fecha: valores.fecha,
        montoAplicado: Number(valores.montoAplicado),
        cuotapartes: Number(valores.cuotapartes),
        valorCuotaparte: Number(valores.valorCuotaparte),
        fechaLiquidacion: valores.fechaLiquidacion || undefined,
        observaciones: valores.observaciones || undefined,
      },
      { onSuccess: () => form.reset(VACIO) }
    )
  }

  if (inversionQuery.isLoading || !inversionQuery.data) {
    return <p className="text-sm text-muted-foreground">Cargando…</p>
  }

  const inv = inversionQuery.data

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-lg font-semibold text-foreground">{inv.instrumento}</h1>
        <p className="text-sm text-muted-foreground">{inv.cuentaOrigenAlias}{inv.objetivoDelDinero ? ` — ${inv.objetivoDelDinero}` : ""}</p>
      </div>

      <Card>
        <CardHeader><CardTitle>Resumen</CardTitle></CardHeader>
        <CardContent>
          <div className="grid gap-4 sm:grid-cols-4 text-sm">
            <div><p className="text-muted-foreground">Cuotapartes</p><p className="font-medium">{inv.cuotapartesAcumuladas}</p></div>
            <div><p className="text-muted-foreground">Monto neto aplicado</p><p className="font-medium">{pesos.format(inv.montoNetoAplicado)}</p></div>
            <div><p className="text-muted-foreground">Valuación actual</p><p className="font-medium">{pesos.format(inv.valuacionActual)}</p></div>
            <div><p className="text-muted-foreground">Rendimiento</p><p className="font-medium">{pesos.format(inv.rendimiento)}</p></div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>Nuevo movimiento</CardTitle></CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-3">
              <FormField control={form.control} name="tipo" render={({ field }) => (
                <FormItem>
                  <FormLabel>Tipo</FormLabel>
                  <FormControl>
                    <select {...field} className={selectClase}>
                      {TIPOS_MOVIMIENTO_INVERSION.map((t) => <option key={t} value={t}>{TIPO_LABEL[t]}</option>)}
                    </select>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )} />
              <FormField control={form.control} name="fecha" render={({ field }) => (
                <FormItem><FormLabel>Fecha</FormLabel><FormControl><Input {...field} type="date" /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="montoAplicado" render={({ field }) => (
                <FormItem><FormLabel>Monto aplicado</FormLabel><FormControl><Input {...field} type="number" step="0.01" /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="cuotapartes" render={({ field }) => (
                <FormItem><FormLabel>Cuotapartes</FormLabel><FormControl><Input {...field} type="number" step="0.000001" /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="valorCuotaparte" render={({ field }) => (
                <FormItem><FormLabel>Valor de cuotaparte</FormLabel><FormControl><Input {...field} type="number" step="0.000001" /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="fechaLiquidacion" render={({ field }) => (
                <FormItem><FormLabel>Fecha de liquidación (opcional)</FormLabel><FormControl><Input {...field} type="date" /></FormControl><FormMessage /></FormItem>
              )} />
              <FormField control={form.control} name="observaciones" render={({ field }) => (
                <FormItem className="sm:col-span-3"><FormLabel>Observaciones</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
              )} />
              <div className="sm:col-span-3">
                <Button type="submit" disabled={crear.isPending}>Registrar movimiento</Button>
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>Movimientos</CardTitle></CardHeader>
        <CardContent>
          {movimientosQuery.isLoading ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[700px] text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr className="border-b border-border">
                    <th className="py-2 pr-4 font-medium">Fecha</th>
                    <th className="py-2 pr-4 font-medium">Tipo</th>
                    <th className="py-2 pr-4 font-medium">Monto aplicado</th>
                    <th className="py-2 pr-4 font-medium">Cuotapartes</th>
                    <th className="py-2 pr-4 font-medium">Valor cuotaparte</th>
                    <th className="py-2 pr-4 font-medium">Mov. bancario</th>
                  </tr>
                </thead>
                <tbody>
                  {(movimientosQuery.data?.content ?? []).map((m) => (
                    <tr key={m.id} className="border-b border-border last:border-0">
                      <td className="py-2 pr-4">{m.fecha}</td>
                      <td className="py-2 pr-4">{TIPO_LABEL[m.tipo]}</td>
                      <td className="py-2 pr-4">{pesos.format(m.montoAplicado)}</td>
                      <td className="py-2 pr-4">{m.cuotapartes}</td>
                      <td className="py-2 pr-4">{m.valorCuotaparte}</td>
                      <td className="py-2 pr-4">{m.movimientoBancarioId ?? "—"}</td>
                    </tr>
                  ))}
                  {(movimientosQuery.data?.content ?? []).length === 0 && (
                    <tr><td colSpan={6} className="py-4 text-center text-sm text-muted-foreground">Sin movimientos todavía.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

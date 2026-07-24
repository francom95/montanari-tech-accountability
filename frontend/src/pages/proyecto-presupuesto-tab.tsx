import { zodResolver } from "@hookform/resolvers/zod"
import { useEffect } from "react"
import { useFieldArray, useForm } from "react-hook-form"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { usePresupuestoProyecto, useGuardarPresupuestoProyecto } from "@/hooks/use-presupuesto-proyecto"
import type { PresupuestoProyecto } from "@/types/presupuesto-proyecto"

const esquemaLinea = z.object({ nombre: z.string().min(1, "Obligatorio"), importeUsd: z.string().min(1, "Obligatorio") })

const esquema = z.object({
  margenDeseadoUsd: z.string().min(1, "El margen deseado es obligatorio"),
  comisionesBancariasIntermediasComexUsd: z.string().optional(),
  observaciones: z.string().optional(),
  lineasCosto: z.array(esquemaLinea),
})

type Valores = z.infer<typeof esquema>

const VALORES_INICIALES: Valores = {
  margenDeseadoUsd: "",
  comisionesBancariasIntermediasComexUsd: "",
  observaciones: "",
  lineasCosto: [],
}

function presupuestoAValores(p: PresupuestoProyecto): Valores {
  return {
    margenDeseadoUsd: p.margenDeseadoUsd.toString(),
    comisionesBancariasIntermediasComexUsd: p.comisionesBancariasIntermediasComexUsd?.toString() || "",
    observaciones: p.observaciones || "",
    lineasCosto: p.lineasCosto.map((l) => ({ nombre: l.nombre, importeUsd: l.importeUsd.toString() })),
  }
}

function n(v: number | null | undefined) {
  return v === null || v === undefined ? "-" : v.toLocaleString("es-AR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function PresupuestoTab({ proyectoId, tipoProyecto }: { proyectoId: number; tipoProyecto: string | null }) {
  const presupuesto = usePresupuestoProyecto(proyectoId)
  const guardar = useGuardarPresupuestoProyecto(proyectoId)

  const form = useForm<Valores>({ resolver: zodResolver(esquema), defaultValues: VALORES_INICIALES })
  const lineas = useFieldArray({ control: form.control, name: "lineasCosto" })

  useEffect(() => {
    if (presupuesto.data) {
      form.reset(presupuestoAValores(presupuesto.data))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [presupuesto.data])

  function onSubmit(valores: Valores) {
    guardar.mutate({
      margenDeseadoUsd: Number(valores.margenDeseadoUsd),
      comisionesBancariasIntermediasComexUsd: valores.comisionesBancariasIntermediasComexUsd
        ? Number(valores.comisionesBancariasIntermediasComexUsd)
        : undefined,
      observaciones: valores.observaciones || undefined,
      lineasCosto: valores.lineasCosto.map((l) => ({ nombre: l.nombre, importeUsd: Number(l.importeUsd) })),
    })
  }

  if (presupuesto.isLoading) {
    return <p className="text-sm text-muted-foreground">Cargando…</p>
  }

  const calc = presupuesto.data?.calculado

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>Costos de producción</CardTitle>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <div className="space-y-2">
                {lineas.fields.map((campo, indice) => (
                  <div key={campo.id} className="grid grid-cols-[1fr_160px_auto] items-end gap-2">
                    <FormField control={form.control} name={`lineasCosto.${indice}.nombre`} render={({ field }) => (
                      <FormItem><FormLabel>Concepto</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                    )} />
                    <FormField control={form.control} name={`lineasCosto.${indice}.importeUsd`} render={({ field }) => (
                      <FormItem><FormLabel>Importe (USD)</FormLabel><FormControl><Input {...field} type="number" step="0.01" /></FormControl><FormMessage /></FormItem>
                    )} />
                    <Button type="button" variant="outline" size="sm" onClick={() => lineas.remove(indice)}>Quitar</Button>
                  </div>
                ))}
              </div>
              <Button type="button" variant="outline" size="sm" onClick={() => lineas.append({ nombre: "", importeUsd: "" })}>
                + Agregar línea de costo
              </Button>

              <div className="grid gap-4 sm:grid-cols-2">
                <FormField control={form.control} name="margenDeseadoUsd" render={({ field }) => (
                  <FormItem><FormLabel>Margen deseado (USD)</FormLabel><FormControl><Input {...field} type="number" step="0.01" /></FormControl><FormMessage /></FormItem>
                )} />
                {tipoProyecto === "EXTERIOR" && (
                  <FormField control={form.control} name="comisionesBancariasIntermediasComexUsd" render={({ field }) => (
                    <FormItem><FormLabel>Comisiones bancarias intermedias COMEX (USD)</FormLabel><FormControl><Input {...field} type="number" step="0.01" /></FormControl><FormMessage /></FormItem>
                  )} />
                )}
              </div>

              <FormField control={form.control} name="observaciones" render={({ field }) => (
                <FormItem><FormLabel>Observaciones</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
              )} />

              <Button type="submit" disabled={guardar.isPending}>Guardar presupuesto</Button>
            </form>
          </Form>
        </CardContent>
      </Card>

      {calc && (
        <Card>
          <CardHeader>
            <CardTitle>Cálculo ({calc.tipoProyecto === "ARGENTINA" ? "Argentina" : "Exterior"})</CardTitle>
          </CardHeader>
          <CardContent>
            <table className="w-full text-left text-sm">
              <tbody>
                <Fila etiqueta="Total costo de producción" valor={calc.totalCostoProduccion} />
                <Fila etiqueta="Margen deseado" valor={calc.margenDeseadoUsd} />
                <Fila etiqueta="Colchón Impuesto a las Ganancias" valor={calc.colchonImpuestoGanancias} />
                <Fila etiqueta="Total costo + ganancia" valor={calc.totalCostoMasGanancia} destacado />

                {calc.tipoProyecto === "ARGENTINA" ? (
                  <>
                    <Fila etiqueta="Comisión por venta" valor={calc.comisionVenta} />
                    <Fila etiqueta="Precio sin IVA" valor={calc.precioSinIva} />
                    <Fila etiqueta="IIBB Convenio Multilateral" valor={calc.iibbConvenioMultilateral} />
                    <Fila etiqueta="Impuesto a los débitos y créditos" valor={calc.impuestoDebitosCreditos} />
                    <Fila etiqueta="IVA débito fiscal" valor={calc.ivaDebitoFiscal} />
                    <Fila etiqueta="Precio con IVA" valor={calc.precioConIva} />
                  </>
                ) : (
                  <>
                    <Fila etiqueta="Comisiones bancarias intermedias COMEX" valor={calc.comisionesBancariasIntermediasComex} />
                    <Fila etiqueta="Comisión bancaria COMEX" valor={calc.comisionBancariaComex} />
                    <Fila etiqueta="Percepción IVA COMEX" valor={calc.percepcionIvaComex} />
                    <Fila etiqueta="IIBB / SIRCREB COMEX" valor={calc.iibbSircrebComex} />
                    <Fila etiqueta="IVA crédito fiscal COMEX" valor={calc.ivaCreditoFiscalComex} />
                    <Fila etiqueta="Total impuestos y comisiones bancarias COMEX" valor={calc.totalImpuestosYComisionesBancariasComex} />
                  </>
                )}

                <Fila etiqueta="Precio final al cliente (USD)" valor={calc.precioFinalCliente} destacado />
              </tbody>
            </table>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

function Fila({ etiqueta, valor, destacado }: { etiqueta: string; valor: number | null; destacado?: boolean }) {
  return (
    <tr className={`border-b border-border last:border-0 ${destacado ? "font-semibold" : ""}`}>
      <td className="py-2 pr-4">{etiqueta}</td>
      <td className="py-2 pr-4 text-right">{n(valor)}</td>
    </tr>
  )
}

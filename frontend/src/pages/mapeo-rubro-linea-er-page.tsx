import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { useCurrentUser } from "@/hooks/use-auth"
import {
  useCrearMapeoRubroLineaEr,
  useEliminarMapeoRubroLineaEr,
  useMapeosRubroLineaEr,
} from "@/hooks/use-mapeo-rubro-linea-er"
import { useRubros } from "@/hooks/use-rubro"
import { ETIQUETA_LINEA, LINEAS_ESTADO_RESULTADOS } from "@/types/estado-resultados"
import type { LineaEstadoResultados } from "@/types/estado-resultados"
import type { NaturalezaEr } from "@/types/mapeo-rubro-linea-er"

const NATURALEZAS: NaturalezaEr[] = ["RP", "RN"]
const NATURALEZA_LABEL: Record<NaturalezaEr, string> = { RP: "Resultado Positivo (RP)", RN: "Resultado Negativo (RN)" }
const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm"

const esquema = z.object({
  rubroId: z.string().min(1, "Obligatorio"),
  naturaleza: z.string().min(1, "Obligatorio"),
  linea: z.string().min(1, "Obligatorio"),
})
type Valores = z.infer<typeof esquema>
const VACIO: Valores = { rubroId: "", naturaleza: "RP", linea: "INGRESOS_POR_VENTAS" }

export function MapeoRubroLineaErPage() {
  const usuario = useCurrentUser()
  const esAdmin = usuario.data?.rol === "ADMINISTRADOR"

  const mapeos = useMapeosRubroLineaEr()
  const rubros = useRubros({ activo: true, page: 0, size: 100 })
  const crear = useCrearMapeoRubroLineaEr()
  const eliminar = useEliminarMapeoRubroLineaEr()

  const form = useForm<Valores>({ resolver: zodResolver(esquema), defaultValues: VACIO })

  function onSubmit(valores: Valores) {
    crear.mutate({
      rubroId: Number(valores.rubroId),
      naturaleza: valores.naturaleza as NaturalezaEr,
      linea: valores.linea as LineaEstadoResultados,
    }, { onSuccess: () => form.reset(VACIO) })
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-lg font-semibold text-foreground">Mapeo rubro → línea del estado de resultados</h1>
        <p className="text-sm text-muted-foreground">
          A qué línea del ER (F7.3) cae cada rubro, separado por naturaleza (RP/RN) — un rubro puede tener cuentas de
          ambos signos mezcladas (p. ej. "Otros Ingresos y Egresos"). Un rubro sin mapeo para una naturaleza queda
          señalado como "sin mapear" en el reporte, nunca se descarta en silencio.
        </p>
      </div>

      {esAdmin && (
        <Card>
          <CardHeader><CardTitle>Nuevo mapeo</CardTitle></CardHeader>
          <CardContent>
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-4">
                <FormField control={form.control} name="rubroId" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Rubro</FormLabel>
                    <FormControl>
                      <select {...field} disabled={rubros.isLoading} className={selectClase}>
                        <option value="">Seleccionar</option>
                        {rubros.data?.content?.map((r) => <option key={r.id} value={r.id.toString()}>{r.nombre}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="naturaleza" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Naturaleza</FormLabel>
                    <FormControl>
                      <select {...field} className={selectClase}>
                        {NATURALEZAS.map((n) => <option key={n} value={n}>{NATURALEZA_LABEL[n]}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="linea" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Línea del ER</FormLabel>
                    <FormControl>
                      <select {...field} className={selectClase}>
                        {LINEAS_ESTADO_RESULTADOS.map((l) => <option key={l} value={l}>{ETIQUETA_LINEA[l]}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <div className="flex items-end">
                  <Button type="submit" disabled={crear.isPending}>Crear mapeo</Button>
                </div>
              </form>
            </Form>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader><CardTitle>Mapeos configurados</CardTitle></CardHeader>
        <CardContent>
          {mapeos.isLoading ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground">
                <tr className="border-b border-border">
                  <th className="py-2 pr-4 font-medium">Rubro</th>
                  <th className="py-2 pr-4 font-medium">Naturaleza</th>
                  <th className="py-2 pr-4 font-medium">Línea del ER</th>
                  <th className="py-2 pr-4 font-medium" />
                </tr>
              </thead>
              <tbody>
                {(mapeos.data ?? []).map((m) => (
                  <tr key={m.id} className="border-b border-border last:border-0">
                    <td className="py-2 pr-4">{m.rubroNombre}</td>
                    <td className="py-2 pr-4">{NATURALEZA_LABEL[m.naturaleza]}</td>
                    <td className="py-2 pr-4">{ETIQUETA_LINEA[m.linea]}</td>
                    <td className="py-2 pr-4">
                      {esAdmin && (
                        <Button variant="outline" size="sm" disabled={eliminar.isPending} onClick={() => eliminar.mutate(m.id)}>
                          Eliminar
                        </Button>
                      )}
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

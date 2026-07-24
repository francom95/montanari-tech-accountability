import { useEffect, useState } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { useActualizarConfiguracionTipoCambio, useConfiguracionTipoCambio } from "@/hooks/use-configuracion-cobranza"

const CRITERIOS = ["", "BNA_VENTA", "BNA_COMPRA", "OFICIAL", "MANUAL", "OTRO"] as const

export function ConfiguracionTipoCambioCard() {
  const config = useConfiguracionTipoCambio()
  const actualizar = useActualizarConfiguracionTipoCambio()
  const [criterio, setCriterio] = useState("")

  useEffect(() => {
    if (config.data) setCriterio(config.data.criterioPorDefecto ?? "")
  }, [config.data])

  return (
    <Card>
      <CardHeader><CardTitle>Criterio de tipo de cambio por defecto (F7.4)</CardTitle></CardHeader>
      <CardContent className="flex items-end gap-2">
        <div className="flex-1">
          <label className="mb-1 block text-sm text-muted-foreground">
            Al resolver automáticamente el TC de un asiento, priorizar este criterio (vacío = como hoy, toma la primera cotización cargada para esa fecha)
          </label>
          <select
            value={criterio}
            onChange={(e) => setCriterio(e.target.value)}
            className="h-8 w-full max-w-xs rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            {CRITERIOS.map((c) => <option key={c} value={c}>{c || "Sin preferencia"}</option>)}
          </select>
        </div>
        <Button
          type="button"
          disabled={actualizar.isPending}
          onClick={() => actualizar.mutate({ criterioPorDefecto: criterio || null })}
        >
          Guardar
        </Button>
      </CardContent>
    </Card>
  )
}

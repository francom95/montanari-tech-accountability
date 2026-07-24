import { useEffect, useState } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { useActualizarConfiguracionCobranza, useConfiguracionCobranza } from "@/hooks/use-configuracion-cobranza"

export function ConfiguracionCobranzaCard() {
  const config = useConfiguracionCobranza()
  const actualizar = useActualizarConfiguracionCobranza()
  const [diasGracia, setDiasGracia] = useState("3")
  const [tasa, setTasa] = useState("0")

  useEffect(() => {
    if (config.data) {
      setDiasGracia(String(config.data.diasGraciaMora))
      setTasa(String(config.data.tasaMoraDiariaPorcentaje))
    }
  }, [config.data])

  return (
    <Card>
      <CardHeader><CardTitle>Mora en cobros (F7.4)</CardTitle></CardHeader>
      <CardContent className="flex flex-wrap items-end gap-4">
        <div>
          <label className="mb-1 block text-sm text-muted-foreground">Días de gracia antes de aplicar recargo</label>
          <Input type="number" step="1" min={0} value={diasGracia} onChange={(e) => setDiasGracia(e.target.value)} className="h-8 w-32" />
        </div>
        <div>
          <label className="mb-1 block text-sm text-muted-foreground">Tasa diaria de mora (%)</label>
          <Input type="number" step="0.0001" min={0} value={tasa} onChange={(e) => setTasa(e.target.value)} className="h-8 w-32" />
        </div>
        <Button
          type="button"
          disabled={actualizar.isPending}
          onClick={() => actualizar.mutate({ diasGraciaMora: Number(diasGracia), tasaMoraDiariaPorcentaje: Number(tasa) })}
        >
          Guardar
        </Button>
      </CardContent>
    </Card>
  )
}

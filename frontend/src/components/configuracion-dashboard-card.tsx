import { useEffect, useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { useActualizarConfiguracionDashboard, useConfiguracionDashboard } from "@/hooks/use-dashboard"

export function ConfiguracionDashboardCard() {
  const config = useConfiguracionDashboard()
  const actualizar = useActualizarConfiguracionDashboard()
  const [diaVencimientoIva, setDiaVencimientoIva] = useState("20")
  const [diaVencimientoIibb, setDiaVencimientoIibb] = useState("15")
  const [ventanaObligacionesDias, setVentanaObligacionesDias] = useState("15")

  useEffect(() => {
    if (config.data) {
      setDiaVencimientoIva(String(config.data.diaVencimientoIva))
      setDiaVencimientoIibb(String(config.data.diaVencimientoIibb))
      setVentanaObligacionesDias(String(config.data.ventanaObligacionesDias))
    }
  }, [config.data])

  return (
    <Card>
      <CardHeader>
        <CardTitle>Configuración del dashboard</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-wrap items-end gap-4">
        <div>
          <label className="mb-1 block text-sm text-muted-foreground">Día de vencimiento IVA (mes siguiente)</label>
          <Input type="number" step="1" min={1} max={28} value={diaVencimientoIva}
            onChange={(e) => setDiaVencimientoIva(e.target.value)} className="h-8 w-32" />
        </div>
        <div>
          <label className="mb-1 block text-sm text-muted-foreground">Día de vencimiento IIBB (mes siguiente)</label>
          <Input type="number" step="1" min={1} max={28} value={diaVencimientoIibb}
            onChange={(e) => setDiaVencimientoIibb(e.target.value)} className="h-8 w-32" />
        </div>
        <div>
          <label className="mb-1 block text-sm text-muted-foreground">Ventana de obligaciones próximas (días)</label>
          <Input type="number" step="1" min={1} value={ventanaObligacionesDias}
            onChange={(e) => setVentanaObligacionesDias(e.target.value)} className="h-8 w-32" />
        </div>
        <Button type="button" disabled={actualizar.isPending}
          onClick={() => actualizar.mutate({
            diaVencimientoIva: Number(diaVencimientoIva),
            diaVencimientoIibb: Number(diaVencimientoIibb),
            ventanaObligacionesDias: Number(ventanaObligacionesDias),
          })}>
          Guardar
        </Button>
      </CardContent>
    </Card>
  )
}

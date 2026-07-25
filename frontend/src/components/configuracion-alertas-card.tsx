import { useEffect, useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { useActualizarConfiguracionAlertas, useConfiguracionAlertas } from "@/hooks/use-configuracion-alertas"
import { useSincronizarAlertas } from "@/hooks/use-alerta"

export function ConfiguracionAlertasCard() {
  const config = useConfiguracionAlertas()
  const actualizar = useActualizarConfiguracionAlertas()
  const sincronizar = useSincronizarAlertas()
  const [diasAnticipacion, setDiasAnticipacion] = useState("7")
  const [diasAtrasoCxc, setDiasAtrasoCxc] = useState("0")

  useEffect(() => {
    if (config.data) {
      setDiasAnticipacion(String(config.data.diasAnticipacion))
      setDiasAtrasoCxc(String(config.data.diasAtrasoCxc))
    }
  }, [config.data])

  return (
    <Card>
      <CardHeader>
        <CardTitle>Configuración de alertas</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-wrap items-end gap-4">
        <div>
          <label className="mb-1 block text-sm text-muted-foreground">Días de anticipación</label>
          <Input type="number" step="1" min={1} value={diasAnticipacion}
            onChange={(e) => setDiasAnticipacion(e.target.value)} className="h-8 w-32" />
        </div>
        <div>
          <label className="mb-1 block text-sm text-muted-foreground">Días de atraso mínimo (CxC)</label>
          <Input type="number" step="1" min={0} value={diasAtrasoCxc}
            onChange={(e) => setDiasAtrasoCxc(e.target.value)} className="h-8 w-32" />
        </div>
        <Button type="button" disabled={actualizar.isPending}
          onClick={() => actualizar.mutate({
            diasAnticipacion: Number(diasAnticipacion),
            diasAtrasoCxc: Number(diasAtrasoCxc),
          })}>
          Guardar
        </Button>
        <Button type="button" variant="outline" disabled={sincronizar.isPending} onClick={() => sincronizar.mutate()}>
          Sincronizar ahora
        </Button>
      </CardContent>
    </Card>
  )
}

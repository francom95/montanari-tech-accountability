import { isAxiosError } from "axios"
import { useState } from "react"

/**
 * F9.3: envuelve el flujo de reintento cuando una mutación gateada devuelve
 * 422 PERIODO_CERRADO — el caller pasa una función `reintentar(motivo)` que
 * repite la misma mutación con `confirmarPeriodoCerrado: true` y el motivo
 * cargado. Reusado en los flujos gateados de asientos/facturas/cobros/pagos
 * (F9.3 Fase B: crear, editar, confirmar, anular, aplicarAnticipo).
 */
export function usePeriodoCerradoOverride() {
  const [pendiente, setPendiente] = useState<{ mensaje: string; reintentar: (motivo: string) => void } | null>(null)
  const [motivo, setMotivo] = useState("")

  function capturar(error: unknown, reintentar: (motivo: string) => void): boolean {
    const data = isAxiosError(error) ? (error.response?.data as { codigo?: string; detail?: string } | undefined) : undefined
    if (data?.codigo !== "PERIODO_CERRADO") {
      return false
    }
    setPendiente({ mensaje: data.detail ?? "El período contable de esta fecha está cerrado.", reintentar })
    return true
  }

  function confirmar() {
    if (!pendiente || !motivo.trim()) return
    pendiente.reintentar(motivo)
    setPendiente(null)
    setMotivo("")
  }

  function cancelar() {
    setPendiente(null)
    setMotivo("")
  }

  return { pendiente, motivo, setMotivo, capturar, confirmar, cancelar }
}

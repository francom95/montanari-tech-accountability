export type TipoMovimientoInversion = "SUSCRIPCION" | "RESCATE"

export const TIPOS_MOVIMIENTO_INVERSION: TipoMovimientoInversion[] = ["SUSCRIPCION", "RESCATE"]

export type MovimientoInversion = {
  id: number
  inversionId: number
  tipo: TipoMovimientoInversion
  fecha: string
  montoAplicado: number
  cuotapartes: number
  valorCuotaparte: number
  fechaLiquidacion: string | null
  movimientoBancarioId: number | null
  observaciones: string | null
}

export type MovimientoInversionCrearInput = {
  inversionId: number
  tipo: TipoMovimientoInversion
  fecha: string
  montoAplicado: number
  cuotapartes: number
  valorCuotaparte: number
  fechaLiquidacion?: string
  observaciones?: string
}

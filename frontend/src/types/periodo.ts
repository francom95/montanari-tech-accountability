export type EstadoPeriodo = "ABIERTO" | "EN_REVISION" | "CERRADO"

export const ESTADOS_PERIODO: EstadoPeriodo[] = ["ABIERTO", "EN_REVISION", "CERRADO"]

export type Periodo = {
  id: number
  anio: number
  mes: number
  estado: EstadoPeriodo
  motivoCierre: string | null
  motivoReapertura: string | null
}

export type GenerarAutomaticosPeriodosResponse = {
  generados: number
}

export type LiquidacionResumenItem = {
  tipo: string
  id: number
  estado: string
  saldoAPagar: number
}

export type ConciliacionResumenItem = {
  cuentaBancariaId: number
  cuentaBancariaAlias: string
  monedaCodigo: string
  fechaDesde: string
  fechaHasta: string
  saldoBanco: number
  saldoSistema: number
  diferencia: number
}

export type PeriodoResumen = {
  periodoId: number
  anio: number
  mes: number
  estado: EstadoPeriodo
  liquidaciones: LiquidacionResumenItem[]
  conciliaciones: ConciliacionResumenItem[]
}

export type Granularidad = "DIARIO" | "SEMANAL" | "MENSUAL"

export type PuntoFlujoCaja = {
  fecha: string
  saldoInicial: number
  ingresos: number
  egresos: number
  saldoFinal: number
  esReal: boolean
  saldoNegativo: boolean
}

export type FlujoCajaResponse = {
  consolidado: PuntoFlujoCaja[]
  porCuenta: Record<string, PuntoFlujoCaja[]>
  advertencias: string[]
}

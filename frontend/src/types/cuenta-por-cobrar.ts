export type EstadoVencimiento = "VENCIDO" | "POR_VENCER" | "SIN_VENCIMIENTO"

export type CuentaPorCobrarFila = {
  facturaVentaId: number
  clienteId: number
  clienteNombre: string
  proyectoId: number | null
  proyectoNombre: string | null
  numero: string
  fecha: string
  fechaVencimiento: string | null
  monedaId: number
  monedaCodigo: string
  total: number
  totalArs: number
  saldo: number
  saldoArs: number
  estadoVencimiento: EstadoVencimiento
}

export type TotalPorMoneda = {
  monedaId: number
  monedaCodigo: string
  totalSaldo: number
  totalSaldoArs: number
}

export type CuentaPorCobrar = {
  filas: CuentaPorCobrarFila[]
  totalesPorMoneda: TotalPorMoneda[]
}

export type CuentaPorCobrarFiltros = {
  clienteId?: number
  proyectoId?: number
  monedaId?: number
  fechaDesde?: string
  fechaHasta?: string
  estadoVencimiento?: EstadoVencimiento
}

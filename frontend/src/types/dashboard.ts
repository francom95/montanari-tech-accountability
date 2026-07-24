export type IndicadorMonto = { valorArs: number; ruta: string }

export type VencimientoImpuesto = { fechaVencimiento: string; saldoAPagarArs: number; ruta: string }

export type DashboardResponse = {
  anio: number
  mes: number
  resultadoMensual: IndicadorMonto
  ventasDelPeriodo: IndicadorMonto
  cobrosDelPeriodo: IndicadorMonto
  cuentasPorCobrar: IndicadorMonto
  cuentasPorPagar: IndicadorMonto
  obligacionesProximas: IndicadorMonto
  saldoCaja: IndicadorMonto
  saldoBanco: IndicadorMonto
  margenEstimado: IndicadorMonto
  egresosProyectados: IndicadorMonto
  proximoVencimientoIva: VencimientoImpuesto
  proximoVencimientoIibb: VencimientoImpuesto
  alertas: string[]
}

export type ConfiguracionDashboard = {
  diaVencimientoIva: number
  diaVencimientoIibb: number
  ventanaObligacionesDias: number
}

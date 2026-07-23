export type SaldoEtiqueta = "DEUDOR" | "ACREEDOR" | "SALDADA"

export type BalanceSumasYSaldosNodo = {
  cuentaId: number
  codigo: string
  nombre: string
  imputable: boolean
  debe: number
  haber: number
  saldo: number
  saldoEtiqueta: SaldoEtiqueta
  saldoEsperado: "DEUDOR" | "ACREEDOR"
  contrarioAlEsperado: boolean
  hijos: BalanceSumasYSaldosNodo[]
}

export type BalanceSumasYSaldos = {
  raices: BalanceSumasYSaldosNodo[]
  totalDebe: number
  totalHaber: number
  balancea: boolean
  diferencia: number
}

export type BalanceSumasYSaldosFiltros = {
  fechaDesde?: string
  fechaHasta?: string
  incluirSinMovimiento?: boolean
  nivelMaximo?: number
}

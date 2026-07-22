export type MatchSugerido = {
  asientoId: number
  asientoNumero: number
  fecha: string
  origenTipo: string | null
  origenId: number | null
  descripcion: string
}

export type CuentaSugerida = {
  cuentaContableId: number
  cuentaContableCodigo: string
  cuentaContableNombre: string
  concepto: string
}

export type ConciliacionMovimiento = {
  movimientoBancarioId: number
  fecha: string
  descripcion: string
  importe: number
  monedaCodigo: string
  estado: "PENDIENTE" | "CONCILIADO" | "DESCARTADO"
  matchSugerido: MatchSugerido | null
  cuentaSugerida: CuentaSugerida | null
  asientoIdAsociado: number | null
  asientoNumeroAsociado: number | null
}

export type ConciliacionResumen = {
  cuentaBancariaId: number
  cuentaBancariaAlias: string
  monedaCodigo: string
  fechaDesde: string
  fechaHasta: string
  saldoBanco: number
  saldoSistema: number
  diferencia: number
  movimientos: ConciliacionMovimiento[]
}

export type TipoCuentaBancaria = "CUENTA_CORRIENTE" | "CAJA_AHORRO" | "MERCADO_PAGO" | "CAJA_FISICA" | "OTRA"
export type EstadoConciliacion = "CONCILIADA" | "PENDIENTE"

export type CuentaBancaria = {
  id: number
  entidad: string
  alias: string
  monedaId: number
  monedaCodigo: string
  tipo: TipoCuentaBancaria
  estadoConciliacion: EstadoConciliacion
  saldoInicial: string
  fechaSaldoInicial: string
  saldoActual: string
  cuentaContableId: number
  cuentaContableCodigo: string
  activo: boolean
}

export type CuentaBancariaCrearInput = {
  entidad: string
  alias: string
  monedaId: number
  tipo: TipoCuentaBancaria
  estadoConciliacion?: EstadoConciliacion
  saldoInicial: string
  fechaSaldoInicial: string
  cuentaContableId: number
}

export type CuentaBancariaEditarInput = CuentaBancariaCrearInput

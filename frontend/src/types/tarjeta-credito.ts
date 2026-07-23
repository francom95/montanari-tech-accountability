export type TarjetaCredito = {
  id: number
  entidad: string
  monedaId: number
  monedaCodigo: string
  diaCierre: number
  diaVencimiento: number
  cuentaBancariaDebitoId: number
  cuentaBancariaDebitoAlias: string
  /** Cuenta contable pasiva espejo de la deuda con la tarjeta (F5.4). Puede faltar en tarjetas creadas antes de F5.4 hasta que se editen. */
  cuentaContableId: number | null
  cuentaContableCodigo: string | null
  saldoInicial: string
  fechaSaldoInicial: string
  saldoActual: string
  activo: boolean
}

export type TarjetaCreditoCrearInput = {
  entidad: string
  monedaId: number
  diaCierre: number
  diaVencimiento: number
  cuentaBancariaDebitoId: number
  cuentaContableId: number
  saldoInicial: string
  fechaSaldoInicial: string
}

export type TarjetaCreditoEditarInput = TarjetaCreditoCrearInput

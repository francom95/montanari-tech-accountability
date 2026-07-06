export type TarjetaCredito = {
  id: number
  entidad: string
  monedaId: number
  monedaCodigo: string
  diaCierre: number
  diaVencimiento: number
  cuentaBancariaDebitoId: number
  cuentaBancariaDebitoAlias: string
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
  saldoInicial: string
  fechaSaldoInicial: string
}

export type TarjetaCreditoEditarInput = TarjetaCreditoCrearInput

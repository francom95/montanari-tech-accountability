export type EstadoPago = "BORRADOR" | "CONFIRMADO" | "ANULADO"

export type PagoImputacion = {
  id: number
  orden: number
  facturaCompraId: number
  facturaCompraNumero: string
  montoImputadoOriginal: number
  montoArsCancelado: number | null
}

export type AplicacionAnticipoPago = {
  id: number
  facturaCompraId: number
  facturaCompraNumero: string
  fecha: string
  montoOriginal: number
  montoArsCancelado: number
  asientoId: number
  asientoNumero: number
}

export type Pago = {
  id: number
  proveedorId: number
  proveedorNombre: string
  fecha: string
  monedaId: number
  monedaCodigo: string
  tipoCambio: number
  cuentaBancariaId: number
  cuentaBancariaAlias: string
  total: number
  totalArs: number
  montoAnticipo: number
  montoAnticipoDisponible: number
  estado: EstadoPago
  asientoId: number | null
  asientoNumero: number | null
  observaciones: string | null
  lineas: PagoImputacion[]
  aplicacionesAnticipo: AplicacionAnticipoPago[]
}

export type PagoImputacionInput = {
  facturaCompraId: number
  montoImputadoOriginal: number
}

export type PagoCrearInput = {
  proveedorId: number
  fecha: string
  monedaId: number
  tipoCambio: number
  cuentaBancariaId: number
  total: number
  observaciones?: string
  lineas?: PagoImputacionInput[]
}

export type PagoEditarInput = PagoCrearInput

export type SaldoFacturaCompra = {
  total: number
  imputado: number
  saldo: number
  totalArs: number
  imputadoArs: number
  saldoArs: number
}

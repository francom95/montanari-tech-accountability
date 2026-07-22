export type EstadoCobro = "BORRADOR" | "CONFIRMADO" | "ANULADO"

export const TIPOS_RETENCION_COBRO = ["RETENCION_GANANCIAS", "RETENCION_IVA"] as const
export type TipoRetencionCobro = (typeof TIPOS_RETENCION_COBRO)[number]

export type CobroImputacion = {
  id: number
  orden: number
  facturaVentaId: number
  facturaVentaNumero: string
  montoImputadoOriginal: number
  montoArsCancelado: number | null
}

export type CobroTributo = {
  id: number
  tipo: TipoRetencionCobro
  importe: number
}

export type AplicacionAnticipoCobro = {
  id: number
  facturaVentaId: number
  facturaVentaNumero: string
  fecha: string
  montoOriginal: number
  montoArsCancelado: number
  asientoId: number
  asientoNumero: number
}

export type Cobro = {
  id: number
  clienteId: number
  clienteNombre: string
  fecha: string
  monedaId: number
  monedaCodigo: string
  tipoCambio: number
  cuentaBancariaId: number
  cuentaBancariaAlias: string
  total: number
  totalArs: number
  importeRetenciones: number
  montoAnticipo: number
  montoAnticipoDisponible: number
  estado: EstadoCobro
  asientoId: number | null
  asientoNumero: number | null
  observaciones: string | null
  lineas: CobroImputacion[]
  tributos: CobroTributo[]
  aplicacionesAnticipo: AplicacionAnticipoCobro[]
}

export type CobroImputacionInput = {
  facturaVentaId: number
  montoImputadoOriginal: number
}

export type CobroTributoInput = {
  tipo: TipoRetencionCobro
  importe: number
}

export type CobroCrearInput = {
  clienteId: number
  fecha: string
  monedaId: number
  tipoCambio: number
  cuentaBancariaId: number
  total: number
  observaciones?: string
  lineas?: CobroImputacionInput[]
  tributos?: CobroTributoInput[]
}

export type CobroEditarInput = CobroCrearInput

export type SaldoFactura = {
  total: number
  imputado: number
  saldo: number
  totalArs: number
  imputadoArs: number
  saldoArs: number
}

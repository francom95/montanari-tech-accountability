export type EstadoMovimientoBancario = "PENDIENTE" | "CONCILIADO" | "DESCARTADO"
export type OrigenImportacionMovimiento = "MANUAL" | "GALICIA" | "MERCADO_PAGO" | "TARJETA_CREDITO"

export type MovimientoBancario = {
  id: number
  cuentaBancariaId: number
  cuentaBancariaAlias: string
  fecha: string
  descripcion: string
  importe: number
  monedaId: number
  monedaCodigo: string
  tipoCambio: number
  importeArs: number
  referencia: string | null
  origenImportacion: OrigenImportacionMovimiento
  cuentaContableSugeridaId: number | null
  cuentaContableSugeridaCodigo: string | null
  estado: EstadoMovimientoBancario
  asientoId: number | null
  asientoNumero: number | null
  motivoDescarte: string | null
  observaciones: string | null
}

export type MovimientoBancarioCrearInput = {
  cuentaBancariaId: number
  fecha: string
  descripcion: string
  importe: number
  monedaId: number
  tipoCambio: number
  referencia?: string
  cuentaContableSugeridaId?: number
  observaciones?: string
}

export type MovimientoBancarioCorregirInput = MovimientoBancarioCrearInput

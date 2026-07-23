export type ConsumoTarjeta = {
  id: number
  tarjetaCreditoId: number
  fecha: string
  descripcion: string
  referencia: string | null
  importe: number
  monedaId: number
  monedaCodigo: string
  tipoCambio: number
  importeArs: number
  cuentaContableId: number | null
  cuentaContableCodigo: string | null
  cuentaContableNombre: string | null
  proveedorId: number | null
  proveedorNombre: string | null
  proyectoId: number | null
  proyectoNombre: string | null
  conceptoId: number | null
  conceptoNombre: string | null
}

export type ClasificarConsumoInput = {
  cuentaContableId: number
  proveedorId?: number
  proyectoId?: number
  conceptoId?: number
}

export type ReglaClasificacionConsumo = {
  id: number
  patron: string
  cuentaContableId: number
  cuentaContableCodigo: string
  cuentaContableNombre: string
  proveedorId: number | null
  proveedorNombre: string | null
  proyectoId: number | null
  proyectoNombre: string | null
  conceptoId: number | null
  conceptoNombre: string | null
  activo: boolean
}

export type ReglaClasificacionConsumoInput = {
  patron: string
  cuentaContableId: number
  proveedorId?: number
  proyectoId?: number
  conceptoId?: number
}

export type ConsumoImportacionPreview = {
  fecha: string
  descripcion: string
  importe: number
  monedaCodigo: string
  referencia: string | null
  duplicado: boolean
  hash: string
}

export type ConsumoImportacionConfirmarInput = {
  fecha: string
  descripcion: string
  importe: number
  monedaCodigo: string
  referencia?: string
  hash: string
}

export type ConsumoImportacionResultado = {
  descripcion: string
  resultado: "IMPORTADO" | "DUPLICADO" | "ERROR"
  motivoError: string | null
  consumoTarjetaId: number | null
}

export type PagoTarjeta = {
  id: number
  tarjetaCreditoId: number
  tarjetaCreditoEntidad: string
  fecha: string
  importe: number
  monedaId: number
  monedaCodigo: string
  tipoCambio: number
  importeArs: number
  estado: "BORRADOR" | "CONFIRMADO" | "ANULADO"
  asientoId: number | null
  asientoNumero: number | null
  observaciones: string | null
}

export type PagoTarjetaCrearInput = {
  tarjetaCreditoId: number
  fecha: string
  importe: number
  monedaId: number
  tipoCambio: number
  observaciones?: string
}

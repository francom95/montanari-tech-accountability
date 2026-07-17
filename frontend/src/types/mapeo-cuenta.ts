export const CONCEPTOS_CONTABLES = [
  "IVA_DEBITO_FISCAL", "IVA_CREDITO_FISCAL", "INGRESO_VENTA", "CREDITO_POR_VENTA",
  "DEUDA_COMERCIAL", "COSTO_GASTO", "PERCEPCION_IVA_SUFRIDA", "PERCEPCION_IIBB_SUFRIDA",
  "RETENCION_GANANCIAS_SUFRIDA", "RETENCION_IVA_SUFRIDA", "DIF_CAMBIO_GANADA",
  "DIF_CAMBIO_PERDIDA", "ANTICIPO_CLIENTE", "ANTICIPO_PROVEEDOR",
] as const

export type ConceptoContable = (typeof CONCEPTOS_CONTABLES)[number]

export type MapeoCuenta = {
  id: number
  concepto: ConceptoContable
  discriminadorTipo: string | null
  discriminadorValor: string | null
  cuentaContableId: number
  cuentaContableCodigo: string
  cuentaContableNombre: string
  activo: boolean
}

export type MapeoCuentaCrearInput = {
  concepto: ConceptoContable
  discriminadorTipo?: string
  discriminadorValor?: string
  cuentaContableId: number
}

export type MapeoCuentaEditarInput = {
  cuentaContableId: number
}

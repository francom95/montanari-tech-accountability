export type TipoCompromiso =
  | "CUOTA_PLAN_DE_PAGOS" | "VENCIMIENTO_IMPOSITIVO" | "IVA_DIFERIDO" | "IIBB" | "PAGO_A_PROVEEDOR"
  | "SUELDOS" | "CARGAS_SOCIALES" | "CONTADOR" | "COMISION_BANCARIA" | "COMISION_POR_VENTA"
  | "SUSCRIPCION" | "TARJETA" | "OTRO_EGRESO"

export type EstadoCompromiso = "PENDIENTE" | "RESUELTO" | "CANCELADO"

export const TIPOS_COMPROMISO: TipoCompromiso[] = [
  "CUOTA_PLAN_DE_PAGOS", "VENCIMIENTO_IMPOSITIVO", "IVA_DIFERIDO", "IIBB", "PAGO_A_PROVEEDOR",
  "SUELDOS", "CARGAS_SOCIALES", "CONTADOR", "COMISION_BANCARIA", "COMISION_POR_VENTA",
  "SUSCRIPCION", "TARJETA", "OTRO_EGRESO",
]

export const ESTADOS_COMPROMISO: EstadoCompromiso[] = ["PENDIENTE", "RESUELTO", "CANCELADO"]

export type Compromiso = {
  id: number
  concepto: string
  tipo: TipoCompromiso
  fechaPrevista: string
  importe: number
  monedaId: number
  monedaCodigo: string
  proveedorId: number | null
  proveedorNombre: string | null
  proyectoId: number | null
  proyectoNombre: string | null
  estado: EstadoCompromiso
  observaciones: string | null
  vencimientoGeneradoId: number | null
  activo: boolean
}

export type CompromisoCrearInput = {
  concepto: string
  tipo: TipoCompromiso
  fechaPrevista: string
  importe: number
  monedaId: number
  proveedorId?: number
  proyectoId?: number
  observaciones?: string
  generarVencimiento: boolean
}

export type CompromisoEditarInput = {
  concepto: string
  tipo: TipoCompromiso
  fechaPrevista: string
  importe: number
  monedaId: number
  proveedorId?: number
  proyectoId?: number
  estado: EstadoCompromiso
  observaciones?: string
}

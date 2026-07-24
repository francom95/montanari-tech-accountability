export type TipoVencimiento =
  | "IVA" | "IIBB" | "GANANCIAS" | "BIENES_PERSONALES" | "CARGAS_SOCIALES" | "SUELDOS"
  | "CONTADOR" | "TARJETA" | "SUSCRIPCION" | "PRESTAMO" | "PLAN_DE_PAGO" | "PAGO_AUTOMATICO" | "OTRO"

export type TipoRecurrencia = "UNICA" | "MENSUAL" | "ANUAL" | "PERSONALIZADA"

/** VENCIDO nunca se persiste: lo calcula el backend en lectura (PENDIENTE + fecha pasada). */
export type EstadoVencimiento = "PENDIENTE" | "VENCIDO" | "PAGADO" | "REPROGRAMADO" | "CANCELADO"

/** Subconjunto que sí acepta el filtro del backend (columna real, sin VENCIDO). */
export type EstadoVencimientoFiltro = "PENDIENTE" | "PAGADO" | "REPROGRAMADO" | "CANCELADO"

export const TIPOS_VENCIMIENTO: TipoVencimiento[] = [
  "IVA", "IIBB", "GANANCIAS", "BIENES_PERSONALES", "CARGAS_SOCIALES", "SUELDOS",
  "CONTADOR", "TARJETA", "SUSCRIPCION", "PRESTAMO", "PLAN_DE_PAGO", "PAGO_AUTOMATICO", "OTRO",
]

export const RECURRENCIAS: TipoRecurrencia[] = ["UNICA", "MENSUAL", "ANUAL", "PERSONALIZADA"]

export const ESTADOS_VENCIMIENTO: EstadoVencimiento[] = ["PENDIENTE", "VENCIDO", "PAGADO", "REPROGRAMADO", "CANCELADO"]

export const ESTADOS_VENCIMIENTO_FILTRO: EstadoVencimientoFiltro[] = ["PENDIENTE", "PAGADO", "REPROGRAMADO", "CANCELADO"]

export type Vencimiento = {
  id: number
  descripcion: string
  tipo: TipoVencimiento
  fecha: string
  importeEstimado: number | null
  monedaId: number
  monedaCodigo: string
  recurrencia: TipoRecurrencia
  intervaloDiasPersonalizado: number | null
  estado: EstadoVencimiento
  cuentaContableId: number | null
  cuentaContableCodigo: string | null
  proveedorId: number | null
  proveedorNombre: string | null
  tarjetaCreditoId: number | null
  tarjetaCreditoEntidad: string | null
  proyectoId: number | null
  proyectoNombre: string | null
  conceptoRecurrenteId: number | null
  conceptoRecurrenteNombre: string | null
  asientoVinculadoId: number | null
  asientoVinculadoNumero: number | null
  origenGeneracion: string
  observaciones: string | null
  motivoCancelacion: string | null
}

export type VencimientoCrearInput = {
  descripcion: string
  tipo: TipoVencimiento
  fecha: string
  importeEstimado?: number
  monedaId: number
  recurrencia: TipoRecurrencia
  intervaloDiasPersonalizado?: number
  cuentaContableId?: number
  proveedorId?: number
  tarjetaCreditoId?: number
  proyectoId?: number
  conceptoRecurrenteId?: number
  observaciones?: string
}

export type VencimientoEditarInput = VencimientoCrearInput

export type VencimientoBusquedaFiltros = {
  tipo?: TipoVencimiento
  estado?: EstadoVencimientoFiltro
  fechaDesde?: string
  fechaHasta?: string
  proyectoId?: number
  proveedorId?: number
  tarjetaId?: number
}

export type GenerarAutomaticosResponse = {
  generados: { origen: string; cantidad: number }[]
  total: number
}

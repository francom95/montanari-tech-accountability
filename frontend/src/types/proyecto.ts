export type EstadoProyecto = "PROSPECTO" | "EN_CURSO" | "PAUSADO" | "FINALIZADO" | "CANCELADO"
export type EstadoComercial = "PROSPECTO" | "EN_NEGOCIACION" | "GANADO" | "PERDIDO"
export type EstadoFacturacion = "NO_FACTURADO" | "PARCIALMENTE_FACTURADO" | "FACTURADO_TOTAL"
export type EstadoCobranza = "PENDIENTE" | "PARCIAL" | "COBRADO_TOTAL"

export type Cuota = {
  id: number
  numero: number
  fechaEstimadaCobro: string
  importe: number
}

export type CuotaInput = {
  fechaEstimadaCobro: string
  importe: number
}

export type Proyecto = {
  id: number
  nombre: string
  clienteId: number
  clienteNombre: string
  responsableId: number | null
  responsableNombre: string | null
  pais: string | null
  tipoProyecto: string | null
  estado: EstadoProyecto
  monedaId: number
  monedaCodigo: string
  montoTotal: number
  cantidadPagosPactados: number | null
  comentarios: string | null
  estadoComercial: EstadoComercial
  estadoFacturacion: EstadoFacturacion
  estadoCobranza: EstadoCobranza
  fechaEstimadaFinalizacion: string | null
  fechaRealFinalizacion: string | null
  cuotas: Cuota[]
  activo: boolean
}

export type ProyectoCrearInput = {
  nombre: string
  clienteId: number
  responsableId?: number
  pais?: string
  tipoProyecto?: string
  estado?: EstadoProyecto
  monedaId: number
  montoTotal: number
  cantidadPagosPactados?: number
  comentarios?: string
  estadoComercial?: EstadoComercial
  estadoFacturacion?: EstadoFacturacion
  estadoCobranza?: EstadoCobranza
  fechaEstimadaFinalizacion?: string
  fechaRealFinalizacion?: string
  cuotas?: CuotaInput[]
}

export type ProyectoEditarInput = ProyectoCrearInput

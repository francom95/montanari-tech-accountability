export type PrioridadPendiente = "ALTA" | "MEDIA" | "BAJA"
export type EstadoPendiente = "PENDIENTE" | "EN_PROCESO" | "RESUELTO" | "CANCELADO" | "POSTERGADO"

export const PRIORIDADES_PENDIENTE: PrioridadPendiente[] = ["ALTA", "MEDIA", "BAJA"]
export const ESTADOS_PENDIENTE: EstadoPendiente[] = ["PENDIENTE", "EN_PROCESO", "RESUELTO", "CANCELADO", "POSTERGADO"]

export type PendienteAdministrativo = {
  id: number
  titulo: string
  descripcion: string | null
  fechaEstimadaResolucion: string | null
  prioridad: PrioridadPendiente
  estado: EstadoPendiente
  responsableId: number | null
  responsableNombre: string | null
  categoria: string | null
  proyectoId: number | null
  proyectoNombre: string | null
  clienteId: number | null
  clienteNombre: string | null
  proveedorId: number | null
  proveedorNombre: string | null
  observaciones: string | null
  activo: boolean
}

export type PendienteAdministrativoCrearInput = {
  titulo: string
  descripcion?: string
  fechaEstimadaResolucion?: string
  prioridad: PrioridadPendiente
  responsableId?: number
  categoria?: string
  proyectoId?: number
  clienteId?: number
  proveedorId?: number
  observaciones?: string
}

export type PendienteAdministrativoEditarInput = PendienteAdministrativoCrearInput & {
  estado: EstadoPendiente
}

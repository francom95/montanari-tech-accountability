export type EstadoEtapa = "PENDIENTE" | "EN_CURSO" | "FINALIZADA" | "CANCELADA"

export type ProveedorDto = {
  id: number
  nombre: string
}

export type Etapa = {
  id: number
  proyectoId: number
  nombre: string
  descripcion: string | null
  estado: EstadoEtapa
  fechaInicio: string | null
  fechaEstimadaFin: string | null
  porcentajeAvance: number | null
  montoPresupuestado: number | null
  costosEstimados: number | null
  proveedores: ProveedorDto[]
  pagosPrevistos: number | null
  cobrosPrevistos: number | null
  observaciones: string | null
  activo: boolean
}

export type EtapaCrearInput = {
  nombre: string
  descripcion?: string
  estado?: EstadoEtapa
  fechaInicio?: string
  fechaEstimadaFin?: string
  porcentajeAvance?: number
  montoPresupuestado?: number
  costosEstimados?: number
  proveedoresIds?: number[]
  pagosPrevistos?: number
  cobrosPrevistos?: number
  observaciones?: string
}

export type EtapaEditarInput = EtapaCrearInput

export type EtapaImportFila = {
  fila: number
  nombre: string | null
  descripcion: string | null
  estado: string | null
  fechaInicio: string | null
  fechaEstimadaFin: string | null
  porcentajeAvance: number | null
  montoPresupuestado: number | null
  costosEstimados: number | null
  pagosPrevistos: number | null
  cobrosPrevistos: number | null
  observaciones: string | null
  proveedoresNombres: string[]
  proveedoresIds: number[]
  errores: string[]
}

export type EtapaImportResultado = {
  creadas: Etapa[]
  rechazadas: EtapaImportFila[]
}

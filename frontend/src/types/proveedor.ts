export type TipoCostoDto = {
  id: number
  nombre: string
}

export type Proveedor = {
  id: number
  nombre: string
  cuit: string
  jurisdiccionId: number
  jurisdiccionNombre: string
  monedaHabitualId: number | null
  monedaHabitualCodigo: string | null
  tiposCosto: TipoCostoDto[]
  contacto: string | null
  email: string | null
  telefono: string | null
  activo: boolean
}

export type ProveedorCrearInput = {
  nombre: string
  cuit: string
  jurisdiccionId: number
  monedaHabitualId?: number
  tiposCostoIds?: number[]
  contacto?: string
  email?: string
  telefono?: string
}

export type ProveedorEditarInput = {
  nombre: string
  jurisdiccionId: number
  monedaHabitualId?: number
  tiposCostoIds?: number[]
  contacto?: string
  email?: string
  telefono?: string
}

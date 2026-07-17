export type Cliente = {
  id: number
  nombre: string
  cuit: string
  jurisdiccionId: number
  jurisdiccionNombre: string
  contacto: string | null
  email: string | null
  telefono: string | null
  cuentaCxcId: number | null
  cuentaCxcCodigo: string | null
  activo: boolean
}

export type ClienteCrearInput = {
  nombre: string
  cuit: string
  jurisdiccionId: number
  contacto?: string
  email?: string
  telefono?: string
  cuentaCxcId?: number
}

export type ClienteEditarInput = {
  nombre: string
  jurisdiccionId: number
  contacto?: string
  email?: string
  telefono?: string
  cuentaCxcId?: number
}

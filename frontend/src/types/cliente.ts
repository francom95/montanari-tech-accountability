export type Cliente = {
  id: number
  nombre: string
  cuit: string
  jurisdiccionId: number
  jurisdiccionNombre: string
  contacto: string | null
  email: string | null
  telefono: string | null
  activo: boolean
}

export type ClienteCrearInput = {
  nombre: string
  cuit: string
  jurisdiccionId: number
  contacto?: string
  email?: string
  telefono?: string
}

export type ClienteEditarInput = {
  nombre: string
  jurisdiccionId: number
  contacto?: string
  email?: string
  telefono?: string
}

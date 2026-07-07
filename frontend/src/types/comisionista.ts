export type Comisionista = {
  id: number
  nombre: string
  cuit: string | null
  contacto: string | null
  email: string | null
  telefono: string | null
  activo: boolean
}

export type ComisionistaCrearInput = {
  nombre: string
  cuit?: string
  contacto?: string
  email?: string
  telefono?: string
}

export type ComisionistaEditarInput = ComisionistaCrearInput

export type TipoCostoDto = {
  id: number
  nombre: string
}

export const CONDICIONES_IVA = ["RESPONSABLE_INSCRIPTO", "MONOTRIBUTISTA", "EXENTO", "CONSUMIDOR_FINAL"] as const
export type CondicionIva = (typeof CONDICIONES_IVA)[number]

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
  condicionIva: CondicionIva
  cuentaCxpId: number | null
  cuentaCxpCodigo: string | null
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
  condicionIva?: CondicionIva
  cuentaCxpId?: number
}

export type ProveedorEditarInput = {
  nombre: string
  jurisdiccionId: number
  monedaHabitualId?: number
  tiposCostoIds?: number[]
  contacto?: string
  email?: string
  telefono?: string
  condicionIva?: CondicionIva
  cuentaCxpId?: number
}

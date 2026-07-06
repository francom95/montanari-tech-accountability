/** Espejo de MonedaResponse/MonedaCrearRequest/MonedaEditarRequest (backend, F1.8 PL-1). */
export type Moneda = {
  id: number
  codigo: string
  nombre: string
  simbolo: string
  activo: boolean
}

export type MonedaCrearInput = {
  codigo: string
  nombre: string
  simbolo: string
}

export type MonedaEditarInput = {
  nombre: string
  simbolo: string
}

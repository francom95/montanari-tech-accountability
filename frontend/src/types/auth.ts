export type RolUsuario = "ADMINISTRADOR" | "CARGA" | "LECTURA"

export type UsuarioActual = {
  id: number
  email: string
  nombre: string
  rol: RolUsuario
}

export type TokenPair = {
  accessToken: string
  refreshToken: string
}

export type Usuario = {
  id: number
  email: string
  nombre: string
  rol: RolUsuario
  activo: boolean
  ultimoLoginEn: string | null
}

/** Forma de un Page<T> de Spring Data tal como lo serializa Jackson. */
export type PageResponse<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

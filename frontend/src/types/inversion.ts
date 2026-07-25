export type EstadoInversion = "ACTIVA" | "RESCATADA_TOTAL" | "CANCELADA"
export type TipoVinculoInversion = "COMPROMISO" | "VENCIMIENTO"

export const ESTADOS_INVERSION: EstadoInversion[] = ["ACTIVA", "RESCATADA_TOTAL", "CANCELADA"]
export const TIPOS_VINCULO_INVERSION: TipoVinculoInversion[] = ["COMPROMISO", "VENCIMIENTO"]

export type Inversion = {
  id: number
  instrumento: string
  cuentaOrigenId: number
  cuentaOrigenAlias: string
  objetivoDelDinero: string | null
  vinculoTipo: TipoVinculoInversion | null
  vinculoRefId: number | null
  estado: EstadoInversion
  activo: boolean
  cuotapartesAcumuladas: number
  montoNetoAplicado: number
  valuacionActual: number
  rendimiento: number
}

export type InversionCrearInput = {
  instrumento: string
  cuentaOrigenId: number
  objetivoDelDinero?: string
  vinculoTipo?: TipoVinculoInversion
  vinculoRefId?: number
}

export type InversionEditarInput = InversionCrearInput & {
  estado: EstadoInversion
}

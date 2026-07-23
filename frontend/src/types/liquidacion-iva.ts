export type TipoComponenteIva =
  | "DEBITO_FISCAL"
  | "RESTITUCION_CREDITO_FISCAL"
  | "CREDITO_FISCAL"
  | "RESTITUCION_DEBITO_FISCAL"
  | "SALDO_TECNICO_ANTERIOR"
  | "PERCEPCIONES"
  | "SALDO_LIBRE_DISPONIBILIDAD_ANTERIOR"
  | "OTRO_TECNICO"
  | "OTRO_INGRESO_DIRECTO"

/**
 * Las dos etapas del art. 24 de la Ley de IVA. El sobrante de cada una es un
 * saldo a favor de especie distinta y por eso se muestran separadas.
 */
export type EtapaIva = "TECNICA" | "INGRESOS_DIRECTOS"

export const ETAPA_DE: Record<TipoComponenteIva, EtapaIva> = {
  DEBITO_FISCAL: "TECNICA",
  RESTITUCION_CREDITO_FISCAL: "TECNICA",
  CREDITO_FISCAL: "TECNICA",
  RESTITUCION_DEBITO_FISCAL: "TECNICA",
  SALDO_TECNICO_ANTERIOR: "TECNICA",
  OTRO_TECNICO: "TECNICA",
  PERCEPCIONES: "INGRESOS_DIRECTOS",
  SALDO_LIBRE_DISPONIBILIDAD_ANTERIOR: "INGRESOS_DIRECTOS",
  OTRO_INGRESO_DIRECTO: "INGRESOS_DIRECTOS",
}

/** Los que el usuario puede agregar a mano; el resto los calcula el motor. */
export const TIPOS_MANUALES: TipoComponenteIva[] = ["OTRO_TECNICO", "OTRO_INGRESO_DIRECTO"]

export type ComponenteLiquidacionIva = {
  id: number
  tipo: TipoComponenteIva
  descripcion: string
  importeCalculado: number
  importeAjuste: number
  importeFinal: number
  /** Aporte con signo al resultado: negativo reduce lo que hay que pagar. */
  aporte: number
  motivoAjuste: string | null
  cuentaContableId: number | null
  cuentaContableCodigo: string | null
  cuentaContableNombre: string | null
  manual: boolean
  orden: number
}

export type LiquidacionIva = {
  id: number
  anio: number
  mes: number
  fechaDesde: string
  fechaHasta: string
  estado: "BORRADOR" | "CONFIRMADO" | "ANULADO"
  saldoAPagar: number
  /** Saldo técnico (art. 24, 1er párrafo): solo computable contra IVA futuro. */
  saldoAFavor: number
  /** Saldo de libre disponibilidad (art. 24, 2do párrafo): además compensable y devolvible. */
  saldoLibreDisponibilidad: number
  asientoId: number | null
  asientoNumero: number | null
  observaciones: string | null
  componentes: ComponenteLiquidacionIva[]
  advertencias: string[]
}

export type DetalleImputacionIva = {
  asientoId: number
  asientoNumero: number
  fecha: string
  descripcion: string
  documentoOrigenTipo: string | null
  documentoOrigenId: number | null
  importe: number
}

export type ComponentePrevisualizadoIva = {
  tipo: TipoComponenteIva
  descripcion: string
  importe: number
  aporte: number
  detalle: DetalleImputacionIva[]
}

export type PrevisualizacionIva = {
  anio: number
  mes: number
  fechaDesde: string
  fechaHasta: string
  saldoAPagar: number
  saldoAFavor: number
  saldoLibreDisponibilidad: number
  componentes: ComponentePrevisualizadoIva[]
  advertencias: string[]
}

export type AjustarComponenteInput = {
  importeAjuste: number
  motivoAjuste?: string
}

export type AgregarComponenteInput = {
  tipo: TipoComponenteIva
  descripcion: string
  importe: number
  cuentaContableId: number
  motivo?: string
}

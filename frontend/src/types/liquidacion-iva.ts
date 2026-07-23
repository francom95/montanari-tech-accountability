export type TipoComponenteIva =
  | "DEBITO_FISCAL"
  | "CREDITO_FISCAL"
  | "PERCEPCIONES"
  | "SALDO_TECNICO_ANTERIOR"
  | "RESTITUCIONES"
  | "OTRO"

/** Los que el motor recalcula desde los asientos: no se agregan ni se eliminan a mano. */
export const TIPOS_AUTOMATICOS: TipoComponenteIva[] = [
  "DEBITO_FISCAL",
  "CREDITO_FISCAL",
  "PERCEPCIONES",
  "SALDO_TECNICO_ANTERIOR",
]

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
  saldoAFavor: number
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

export type TipoComponenteIibb =
  | "PERCEPCIONES"
  | "RETENCIONES"
  | "SIRCREB"
  | "PAGOS_A_CUENTA"
  | "SALDO_A_FAVOR_ANTERIOR"
  | "OTRO"

export type ComponenteIibb = {
  id: number
  tipo: TipoComponenteIibb
  descripcion: string
  importeCalculado: number
  importeAjuste: number
  importeFinal: number
  /** Aporte con signo: las deducciones restan del impuesto determinado. */
  aporte: number
  motivoAjuste: string | null
  cuentaContableId: number | null
  cuentaContableCodigo: string | null
  cuentaContableNombre: string | null
  manual: boolean
  orden: number
}

export type JurisdiccionIibb = {
  id: number
  jurisdiccionId: number
  jurisdiccionCodigo: string
  jurisdiccionNombre: string
  /** Coeficiente de Convenio Multilateral (0..1). */
  coeficiente: number
  baseImponible: number
  alicuota: number
  impuestoDeterminado: number
  saldoAPagar: number
  saldoAFavor: number
  orden: number
  componentes: ComponenteIibb[]
}

export type LiquidacionIibb = {
  id: number
  anio: number
  mes: number
  fechaDesde: string
  fechaHasta: string
  estado: "BORRADOR" | "CONFIRMADO" | "ANULADO"
  baseTotal: number
  saldoAPagarTotal: number
  saldoAFavorTotal: number
  asientoId: number | null
  asientoNumero: number | null
  observaciones: string | null
  jurisdicciones: JurisdiccionIibb[]
  advertencias: string[]
}

export type JurisdiccionPrevisualizadaIibb = {
  jurisdiccionId: number
  jurisdiccionCodigo: string
  jurisdiccionNombre: string
  ventasDestino: number
  coeficiente: number
  baseImponible: number
  alicuota: number
  impuestoDeterminado: number
  saldoAFavorAnterior: number
}

export type PrevisualizacionIibb = {
  anio: number
  mes: number
  fechaDesde: string
  fechaHasta: string
  baseTotal: number
  deduccionesDisponibles: number
  jurisdicciones: JurisdiccionPrevisualizadaIibb[]
  advertencias: string[]
}

export type EditarJurisdiccionIibbInput = {
  coeficiente: number
  alicuota: number
}

export type AjustarComponenteIibbInput = {
  importeAjuste: number
  motivoAjuste?: string
}

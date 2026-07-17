export type MayorFila = {
  esSaldoAnterior: boolean
  fecha: string | null
  asientoId: number | null
  numeroAsiento: number | null
  descripcion: string
  cuentaContableId: number | null
  cuentaContableCodigo: string | null
  cuentaContableNombre: string | null
  debe: number | null
  haber: number | null
  saldoAcumulado: number
  monedaId: number | null
  monedaCodigo: string | null
  importeOriginal: number | null
  tipoCambio: number | null
  origen: string | null
}

export type SaldoFinalEtiqueta = "DEUDOR" | "ACREEDOR" | "SALDADA"

export type Mayor = {
  cuentaContableId: number
  cuentaContableCodigo: string
  cuentaContableNombre: string
  esCuentaMadre: boolean
  vistaAnalitica: boolean
  filas: MayorFila[]
  page: number
  size: number
  totalFilas: number
  totalPaginas: number
  saldoFinal: number
  saldoFinalEtiqueta: SaldoFinalEtiqueta
  contrarioAlEsperado: boolean | null
}

export type MayorFiltros = {
  rubroId?: number
  proyectoId?: number
  clienteId?: number
  proveedorId?: number
  origen?: string
  monedaId?: number
  fechaDesde?: string
  fechaHasta?: string
}

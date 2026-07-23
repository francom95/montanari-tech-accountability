export type LineaEstadoResultados =
  | "INGRESOS_POR_VENTAS"
  | "OTROS_INGRESOS_POR_VENTAS"
  | "COSTOS_DE_PRESTACION_DE_SERVICIOS"
  | "GASTOS_DE_COMERCIALIZACION"
  | "GASTOS_DE_ADMINISTRACION"
  | "GASTOS_FINANCIEROS"
  | "IMPUESTOS"
  | "OTROS_INGRESOS"
  | "OTROS_EGRESOS"

export const LINEAS_ESTADO_RESULTADOS: LineaEstadoResultados[] = [
  "INGRESOS_POR_VENTAS",
  "OTROS_INGRESOS_POR_VENTAS",
  "COSTOS_DE_PRESTACION_DE_SERVICIOS",
  "GASTOS_DE_COMERCIALIZACION",
  "GASTOS_DE_ADMINISTRACION",
  "GASTOS_FINANCIEROS",
  "IMPUESTOS",
  "OTROS_INGRESOS",
  "OTROS_EGRESOS",
]

export const ETIQUETA_LINEA: Record<LineaEstadoResultados, string> = {
  INGRESOS_POR_VENTAS: "Ingresos por ventas",
  OTROS_INGRESOS_POR_VENTAS: "Otros ingresos por ventas",
  COSTOS_DE_PRESTACION_DE_SERVICIOS: "Costos de prestación de servicios",
  GASTOS_DE_COMERCIALIZACION: "Gastos de comercialización",
  GASTOS_DE_ADMINISTRACION: "Gastos de administración",
  GASTOS_FINANCIEROS: "Gastos financieros",
  IMPUESTOS: "Impuestos",
  OTROS_INGRESOS: "Otros ingresos",
  OTROS_EGRESOS: "Otros egresos",
}

export type CuentaMonto = { cuentaId: number; codigo: string; nombre: string; monto: number }

export type LineaCalculada = { linea: LineaEstadoResultados; monto: number; cuentas: CuentaMonto[] }

export type EstadoResultadosCalculado = {
  lineas: LineaCalculada[]
  resultadoBruto: number
  resultadoOperativo: number
  resultadoFinal: number
  montoSinMapear: number
  cuentasSinMapear: CuentaMonto[]
  tieneMovimiento: boolean
}

export type ComparativoMes = {
  anioAnterior: number
  mesAnterior: number
  resultadoFinalAnterior: number
  variacionAbsoluta: number
  variacionPorcentual: number | null
}

export type EstadoResultadosResponse = {
  calculado: EstadoResultadosCalculado
  comparativoMesAnterior: ComparativoMes | null
}

export type EstadoResultadosPorProyectoItem = {
  proyectoId: number
  proyectoNombre: string
  calculado: EstadoResultadosCalculado
}

export type EstadoResultadosPorProyectoResponse = {
  porProyecto: EstadoResultadosPorProyectoItem[]
  sinProyecto: EstadoResultadosCalculado
}

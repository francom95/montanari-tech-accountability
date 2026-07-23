export type TipoLiquidacion = "IVA" | "IIBB"

export type CriterioAtribucion = "DIRECTO" | "FACTURACION" | "MARGEN" | "PORCENTAJE_MANUAL"

export const CRITERIOS: { value: CriterioAtribucion; label: string }[] = [
  { value: "FACTURACION", label: "Por facturación del período" },
  { value: "MARGEN", label: "Por margen (ventas − compras)" },
  { value: "PORCENTAJE_MANUAL", label: "Porcentaje manual" },
  { value: "DIRECTO", label: "Directo (100% a un proyecto)" },
]

export type LineaAtribucion = {
  proyectoId: number
  proyectoNombre: string
  porcentaje: number
  monto: number
}

export type Atribucion = {
  liquidacionTipo: TipoLiquidacion
  liquidacionId: number
  anio: number
  mes: number
  criterio: CriterioAtribucion
  montoTotal: number
  guardada: boolean
  lineas: LineaAtribucion[]
  advertencias: string[]
}

export type PorcentajeProyecto = { proyectoId: number; porcentaje: number }

export type CalcularAtribucionInput = {
  criterio: CriterioAtribucion
  proyectoIdDirecto?: number
  porcentajes?: PorcentajeProyecto[]
}

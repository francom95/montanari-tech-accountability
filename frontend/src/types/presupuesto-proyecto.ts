export type TipoProyecto = "ARGENTINA" | "EXTERIOR"

export type PresupuestoCalculado = {
  tipoProyecto: TipoProyecto
  totalCostoProduccion: number
  margenDeseadoUsd: number
  colchonImpuestoGanancias: number
  totalCostoMasGanancia: number

  comisionVenta: number | null
  precioSinIva: number | null
  iibbConvenioMultilateral: number | null
  impuestoDebitosCreditos: number | null
  ivaDebitoFiscal: number | null
  precioConIva: number | null

  comisionesBancariasIntermediasComex: number | null
  comisionBancariaComex: number | null
  percepcionIvaComex: number | null
  iibbSircrebComex: number | null
  ivaCreditoFiscalComex: number | null
  totalImpuestosYComisionesBancariasComex: number | null

  precioFinalCliente: number
}

export type LineaCostoPresupuesto = { id: number; nombre: string; importeUsd: number }

export type PresupuestoProyecto = {
  id: number
  proyectoId: number
  margenDeseadoUsd: number
  comisionesBancariasIntermediasComexUsd: number | null
  observaciones: string | null
  lineasCosto: LineaCostoPresupuesto[]
  calculado: PresupuestoCalculado
}

export type PresupuestoProyectoGuardarInput = {
  margenDeseadoUsd: number
  comisionesBancariasIntermediasComexUsd?: number
  observaciones?: string
  lineasCosto: { nombre: string; importeUsd: number }[]
}

export type ConfiguracionPresupuesto = {
  comisionVentaPorcentaje: number
  colchonImpuestoGananciasPorcentaje: number
  iibbConvenioMultilateralPorcentaje: number
  impuestoDebitosCreditosPorcentaje: number
  ivaPorcentaje: number
  diferenciaDolarComercializacionPorcentaje: number
  percepcionIvaComexPorcentaje: number
  iibbSircrebComexPorcentaje: number
  comexUmbralUnoUsd: number
  comexMontoUnoUsd: number
  comexUmbralDosUsd: number
  comexMontoDosUsd: number
  comexUmbralTresUsd: number
  comexMontoTresUsd: number
  comexPorcentajeExcedente: number
}

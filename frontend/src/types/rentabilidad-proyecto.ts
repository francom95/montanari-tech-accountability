import type { PresupuestoCalculado } from "@/types/presupuesto-proyecto"

export type EtapaResumen = { id: number; nombre: string; estado: string; porcentajeAvance: number | null }

export type ProveedorResumen = {
  proveedorId: number
  proveedorNombre: string
  facturadoArs: number
  pagadoArs: number
  pendienteArs: number
}

export type ComisionResumen = {
  id: number
  comisionistaNombre: string
  porcentajeComision: number
  estadoPago: string
  importeEstimado: number
  importeFinal: number | null
  monedaCodigo: string
}

export type TotalPorMoneda = { monedaId: number; monedaCodigo: string; total: number }

export type PresupuestoComparacion = {
  calculado: PresupuestoCalculado
  cantidadPagosPactados: number
  pagosEmparejadosConFactura: number
  presupuestoConvertidoArs: number
  facturadoEmparejadoArs: number
  diferenciaArs: number
}

export type ReporteRentabilidadProyecto = {
  proyectoId: number
  proyectoNombre: string
  clienteNombre: string
  tipoProyecto: string | null
  estado: string
  fechaEstimadaFinalizacion: string | null
  fechaRealFinalizacion: string | null
  etapas: EtapaResumen[]

  totalFacturadoVentaArs: number
  totalCobradoArs: number
  pendienteCobroArs: number
  facturasVentaConfirmadas: number
  facturasVentaSaldadas: number

  totalFacturadoCompraArs: number
  totalPagadoArs: number
  pendientePagoArs: number
  proveedores: ProveedorResumen[]

  comisiones: ComisionResumen[]
  comisionesPorMoneda: TotalPorMoneda[]
  comisionesArs: number

  impuestosAtribuidosArs: number

  presupuesto: PresupuestoComparacion | null

  margenRealArs: number
  advertencias: string[]
}

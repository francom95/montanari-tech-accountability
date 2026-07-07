export type BaseCalculo = "MONTO_TOTAL" | "MONTO_SIN_IMPUESTOS" | "MONTO_COBRADO"
export type EstadoPagoComision = "PENDIENTE" | "PAGADO"

export type ComisionProyecto = {
  id: number
  proyectoId: number
  proyectoNombre: string
  comisionistaId: number
  comisionistaNombre: string
  porcentajeComision: number
  baseCalculo: BaseCalculo
  monedaId: number
  monedaCodigo: string
  importeEstimado: number
  importeFinal: number | null
  estadoPago: EstadoPagoComision
  fechaEstimadaPago: string | null
  observaciones: string | null
  activo: boolean
}

export type ComisionProyectoCrearInput = {
  comisionistaId: number
  porcentajeComision: number
  baseCalculo: BaseCalculo
  monedaId: number
  fechaEstimadaPago?: string
  observaciones?: string
}

export type ComisionProyectoEditarInput = {
  comisionistaId: number
  porcentajeComision: number
  baseCalculo: BaseCalculo
  monedaId: number
  importeFinal?: number
  estadoPago?: EstadoPagoComision
  fechaEstimadaPago?: string
  observaciones?: string
}

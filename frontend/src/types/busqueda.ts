export type TipoEntidadBusqueda =
  | "ASIENTO"
  | "FACTURA_VENTA"
  | "FACTURA_COMPRA"
  | "CLIENTE"
  | "PROVEEDOR"
  | "PROYECTO"
  | "ETAPA"
  | "CUENTA_CONTABLE"
  | "PAGO"
  | "COBRO"
  | "MOVIMIENTO_BANCARIO"
  | "TARJETA_CREDITO"
  | "VENCIMIENTO"
  | "PENDIENTE_ADMINISTRATIVO"
  | "LIQUIDACION_IVA"
  | "LIQUIDACION_IIBB"

export type ResultadoBusqueda = {
  id: number
  tipo: TipoEntidadBusqueda
  resumen: string
  fecha: string | null
  contextoId: number | null
}

export type GrupoResultadoBusqueda = {
  items: ResultadoBusqueda[]
  total: number
}

export type BusquedaGlobalResponse = {
  grupos: Partial<Record<TipoEntidadBusqueda, GrupoResultadoBusqueda>>
}

export const ETIQUETA_TIPO_BUSQUEDA: Record<TipoEntidadBusqueda, string> = {
  ASIENTO: "Asientos",
  FACTURA_VENTA: "Facturas de venta",
  FACTURA_COMPRA: "Facturas de compra",
  CLIENTE: "Clientes",
  PROVEEDOR: "Proveedores",
  PROYECTO: "Proyectos",
  ETAPA: "Etapas",
  CUENTA_CONTABLE: "Cuentas contables",
  PAGO: "Pagos",
  COBRO: "Cobros",
  MOVIMIENTO_BANCARIO: "Movimientos bancarios",
  TARJETA_CREDITO: "Tarjetas de crédito",
  VENCIMIENTO: "Vencimientos",
  PENDIENTE_ADMINISTRATIVO: "Pendientes administrativos",
  LIQUIDACION_IVA: "Liquidaciones de IVA",
  LIQUIDACION_IIBB: "Liquidaciones de IIBB",
}

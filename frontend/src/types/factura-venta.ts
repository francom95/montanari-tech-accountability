export const TIPOS_COMPROBANTE = [
  "FACTURA_A", "FACTURA_B", "FACTURA_C", "FACTURA_E",
  "NOTA_CREDITO_A", "NOTA_CREDITO_B", "NOTA_CREDITO_C", "NOTA_CREDITO_E",
  "NOTA_DEBITO_A", "NOTA_DEBITO_B", "NOTA_DEBITO_C", "NOTA_DEBITO_E",
  "RECIBO", "TICKET", "OTRO",
] as const
export type TipoComprobante = (typeof TIPOS_COMPROBANTE)[number]

export type TipoLineaFactura = "GRAVADO" | "NO_GRAVADO" | "EXENTO"
export type TipoIngreso = "VENTA" | "OTRA_VENTA"
export type EstadoFactura = "BORRADOR" | "CONFIRMADO" | "ANULADO"

export type FacturaVentaLinea = {
  id: number
  orden: number
  descripcion: string
  tipo: TipoLineaFactura
  importeNeto: number
  alicuotaIva: number
  importeIva: number
  tipoIngreso: TipoIngreso
  cuentaContableId: number | null
  cuentaContableCodigo: string | null
}

export type FacturaVenta = {
  id: number
  clienteId: number
  clienteNombre: string
  proyectoId: number | null
  proyectoNombre: string | null
  fecha: string
  fechaVencimiento: string | null
  tipoComprobante: TipoComprobante
  puntoVenta: string | null
  numero: string
  jurisdiccionDestinoId: number | null
  monedaId: number
  monedaCodigo: string
  tipoCambio: number
  netoGravado: number
  noGravado: number
  exento: number
  importeIva: number
  total: number
  totalArs: number
  estado: EstadoFactura
  asientoId: number | null
  asientoNumero: number | null
  observaciones: string | null
  lineas: FacturaVentaLinea[]
}

export type FacturaVentaLineaInput = {
  descripcion: string
  tipo: TipoLineaFactura
  importeNeto: number
  alicuotaIva: number
  tipoIngreso?: TipoIngreso
  cuentaContableId?: number
}

export type FacturaVentaCrearInput = {
  clienteId: number
  proyectoId?: number
  fecha: string
  fechaVencimiento?: string
  tipoComprobante: TipoComprobante
  puntoVenta?: string
  numero: string
  jurisdiccionDestinoId?: number
  monedaId: number
  tipoCambio: number
  observaciones?: string
  lineas: FacturaVentaLineaInput[]
}

export type FacturaVentaEditarInput = FacturaVentaCrearInput

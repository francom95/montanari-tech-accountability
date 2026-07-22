import type { TipoComprobante } from "@/types/factura-venta"

export type EstadoFacturaCompra = "BORRADOR" | "CONFIRMADO" | "ANULADO"

export const TIPOS_TRIBUTO_COMPRA = ["PERCEPCION_IVA", "PERCEPCION_IIBB"] as const
export type TipoTributoCompra = (typeof TIPOS_TRIBUTO_COMPRA)[number]

export type FacturaCompraLinea = {
  id: number
  orden: number
  descripcion: string
  tipoCostoId: number
  tipoCostoNombre: string
  importeNeto: number
  alicuotaIva: number
  importeIva: number
  cuentaContableId: number | null
  cuentaContableCodigo: string | null
}

export type FacturaCompraTributo = {
  id: number
  tipo: TipoTributoCompra
  jurisdiccionId: number | null
  jurisdiccionNombre: string | null
  base: number | null
  alicuota: number | null
  importe: number
}

export type FacturaCompra = {
  id: number
  proveedorId: number
  proveedorNombre: string
  proyectoId: number | null
  proyectoNombre: string | null
  fecha: string
  fechaVencimiento: string | null
  tipoComprobante: TipoComprobante
  puntoVenta: string | null
  numero: string
  monedaId: number
  monedaCodigo: string
  tipoCambio: number
  neto: number
  importeIva: number
  importePercepciones: number
  total: number
  totalArs: number
  estado: EstadoFacturaCompra
  asientoId: number | null
  asientoNumero: number | null
  observaciones: string | null
  lineas: FacturaCompraLinea[]
  tributos: FacturaCompraTributo[]
}

export type FacturaCompraLineaInput = {
  descripcion: string
  tipoCostoId: number
  importeNeto: number
  alicuotaIva: number
  cuentaContableId?: number
}

export type FacturaCompraTributoInput = {
  tipo: TipoTributoCompra
  jurisdiccionId?: number
  base?: number
  alicuota?: number
  importe: number
}

export type FacturaCompraCrearInput = {
  proveedorId: number
  proyectoId?: number
  fecha: string
  fechaVencimiento?: string
  tipoComprobante: TipoComprobante
  puntoVenta?: string
  numero: string
  monedaId: number
  tipoCambio: number
  observaciones?: string
  lineas: FacturaCompraLineaInput[]
  tributos?: FacturaCompraTributoInput[]
}

export type FacturaCompraEditarInput = FacturaCompraCrearInput

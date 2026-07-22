import type { TipoComprobante } from "@/types/factura-venta"

export type TipoDocumentoImportacion = "VENTA" | "COMPRA"
export type EstadoDestinoImportacion = "BORRADOR" | "CONFIRMADO"

export type FilaImportacionPreview = {
  nombreArchivo: string
  tipoSugerido: TipoDocumentoImportacion
  tipoComprobante: TipoComprobante | null
  puntoVenta: string | null
  numero: string | null
  fecha: string | null
  cuitContraparte: string | null
  clienteId: number | null
  clienteNombre: string | null
  proveedorId: number | null
  proveedorNombre: string | null
  monedaCodigo: string
  monedaId: number | null
  tipoCambio: number | null
  netoGravado: number | null
  alicuotaIva: number | null
  total: number | null
  cae: string | null
  advertencias: string[]
  textoExtraido: string
}

export type FilaImportacionConfirmarInput = {
  nombreArchivo: string
  tipo: TipoDocumentoImportacion
  clienteId?: number
  proveedorId?: number
  altaRapidaNombre?: string
  altaRapidaCuit?: string
  altaRapidaJurisdiccionId?: number
  proyectoId?: number
  fecha: string
  fechaVencimiento?: string
  tipoComprobante: TipoComprobante
  puntoVenta?: string
  numero: string
  monedaId: number
  tipoCambio: number
  observaciones?: string
  descripcionLinea: string
  importeNeto: number
  alicuotaIva: number
  tipoIngreso?: string
  tipoCostoId?: number
  estadoDestino: EstadoDestinoImportacion
}

export type FilaImportacionResultado = {
  nombreArchivo: string
  exito: boolean
  tipo: string
  numero: string
  facturaId: number | null
  estadoFinal: string | null
  asientoId: number | null
  motivoRechazo: string | null
  advertencia: string | null
}

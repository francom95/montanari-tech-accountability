export type TipoCambio = { id: number; fecha: string; monedaId: number; criterio: string; valorCompra: string; valorVenta: string; fuente?: string; observaciones?: string; activo: boolean }
export type TipoCambioCrearInput = { fecha: string; monedaId: number; criterio: string; valorCompra: string; valorVenta: string; fuente?: string; observaciones?: string }
export type TipoCambioEditarInput = { valorCompra: string; valorVenta: string; fuente?: string; observaciones?: string }

import type { OrigenImportacionMovimiento } from "@/types/movimiento-bancario"

/** Orígenes con parser real (F5.2) — MANUAL no aplica acá, esa carga la cubre F5.1 directamente. */
export type OrigenConParser = Exclude<OrigenImportacionMovimiento, "MANUAL">

export type FilaImportacionBancariaPreview = {
  /** Nula cuando el origen no trae fecha en la fila (ej. Galicia ARS): se completa acá antes de confirmar. */
  fecha: string | null
  descripcion: string
  importe: number
  monedaCodigo: string
  referencia: string | null
  duplicado: boolean
  hash: string
}

export type FilaImportacionBancariaConfirmarInput = {
  fecha?: string
  descripcion: string
  importe: number
  monedaCodigo?: string
  referencia?: string
  hash: string
}

export type ResultadoImportacionBancaria = "IMPORTADO" | "DUPLICADO" | "ERROR"

export type FilaImportacionBancariaResultado = {
  descripcion: string
  resultado: ResultadoImportacionBancaria
  motivoError: string | null
  movimientoBancarioId: number | null
}

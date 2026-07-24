export type Periodicidad = "UNICA" | "MENSUAL" | "ANUAL"
export type Concepto = { id: number; nombre: string; descripcion?: string; cuentaSugerida?: string; periodicidad: Periodicidad; importe?: string; monedaId?: number; activo: boolean }
export type ConceptoCrearInput = { nombre: string; descripcion?: string; cuentaSugerida?: string; periodicidad: Periodicidad; importe?: string; monedaId?: number }
export type ConceptoEditarInput = Omit<ConceptoCrearInput, 'nombre'> & { nombre: string }

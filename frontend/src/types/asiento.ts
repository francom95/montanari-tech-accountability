export type EstadoAsiento = "BORRADOR" | "CONFIRMADO" | "ANULADO"
export type FuenteTc = "MANUAL" | "AUTOMATICO"

export type AsientoLinea = {
  id: number
  orden: number
  cuentaContableId: number
  cuentaContableCodigo: string
  cuentaContableNombre: string
  debe: number
  haber: number
  monedaId: number
  monedaCodigo: string
  tipoCambio: number | null
  importeOriginal: number | null
  fuenteTc: FuenteTc | null
  leyenda: string | null
  proyectoId: number | null
  proyectoNombre: string | null
  etapaId: number | null
  etapaNombre: string | null
  clienteId: number | null
  clienteNombre: string | null
  proveedorId: number | null
  proveedorNombre: string | null
  cuentaBancariaId: number | null
  cuentaBancariaAlias: string | null
  generadaAuto: boolean
}

export type Asiento = {
  id: number
  fecha: string
  descripcion: string
  estado: EstadoAsiento
  numero: number | null
  origen: string
  origenTipo: string | null
  origenId: number | null
  observaciones: string | null
  totalDebe: number
  totalHaber: number
  lineas: AsientoLinea[]
}

export type AsientoLineaInput = {
  cuentaContableId: number
  debe: number
  haber: number
  monedaId: number
  tipoCambio?: number
  importeOriginal?: number
  leyenda?: string
  proyectoId?: number
  etapaId?: number
  clienteId?: number
  proveedorId?: number
  cuentaBancariaId?: number
}

export type AsientoCrearInput = {
  fecha: string
  descripcion: string
  observaciones?: string
  lineas: AsientoLineaInput[]
}

export type AsientoEditarInput = AsientoCrearInput

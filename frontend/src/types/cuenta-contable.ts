export type Naturaleza = "ACTIVO" | "PASIVO" | "PN" | "RP" | "RN" | "OTROS_RESULTADOS"
export type SaldoEsperado = "DEUDOR" | "ACREEDOR"

export type ProyectoUsoHabitual = {
  id: number
  nombre: string
}

export type CuentaContable = {
  id: number
  codigo: string
  nombre: string
  padreId: number | null
  padreCodigo: string | null
  naturaleza: Naturaleza
  rubroId: number | null
  rubroNombre: string | null
  imputable: boolean
  saldoEsperado: SaldoEsperado
  activo: boolean
  proyectosUsoHabitual: ProyectoUsoHabitual[]
}

export type CuentaContableNodo = {
  id: number
  codigo: string
  nombre: string
  naturaleza: Naturaleza
  rubroId: number | null
  rubroNombre: string | null
  imputable: boolean
  saldoEsperado: SaldoEsperado
  activo: boolean
  hijos: CuentaContableNodo[]
}

export type CuentaContableCrearInput = {
  codigo: string
  nombre: string
  padreId?: number
  naturaleza: Naturaleza
  rubroId?: number
  imputable: boolean
  saldoEsperado: SaldoEsperado
  proyectosUsoHabitualIds?: number[]
}

export type CuentaContableEditarInput = CuentaContableCrearInput

import type { LineaEstadoResultados } from "./estado-resultados"

export type NaturalezaEr = "RP" | "RN"

export type MapeoRubroLineaEr = {
  id: number
  rubroId: number
  rubroNombre: string
  naturaleza: NaturalezaEr
  linea: LineaEstadoResultados
}

export type MapeoRubroLineaErCrearInput = {
  rubroId: number
  naturaleza: NaturalezaEr
  linea: LineaEstadoResultados
}

export type MapeoRubroLineaErEditarInput = {
  linea: LineaEstadoResultados
}

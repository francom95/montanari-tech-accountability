import { useQuery } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { EstadoResultadosPorProyectoResponse, EstadoResultadosResponse } from "@/types/estado-resultados"

const QUERY_KEY = ["estado-resultados"]

export function useEstadoResultadosPorMes(anio: number, mes: number, enabled = true) {
  return useQuery({
    queryKey: [...QUERY_KEY, "mes", anio, mes],
    queryFn: async () =>
      (await http.get<EstadoResultadosResponse>("/reportes/estado-resultados/mes", { params: { anio, mes } })).data,
    enabled,
  })
}

export function useEstadoResultadosPorAnio(anio: number, enabled = true) {
  return useQuery({
    queryKey: [...QUERY_KEY, "anio", anio],
    queryFn: async () =>
      (await http.get<EstadoResultadosResponse>("/reportes/estado-resultados/anio", { params: { anio } })).data,
    enabled,
  })
}

export function useEstadoResultadosAcumulado(anio: number, mes: number, enabled = true) {
  return useQuery({
    queryKey: [...QUERY_KEY, "acumulado", anio, mes],
    queryFn: async () =>
      (await http.get<EstadoResultadosResponse>("/reportes/estado-resultados/acumulado", { params: { anio, mes } })).data,
    enabled,
  })
}

export function useEstadoResultadosPorProyecto(anio: number, mes: number, enabled = true) {
  return useQuery({
    queryKey: [...QUERY_KEY, "por-proyecto", anio, mes],
    queryFn: async () =>
      (await http.get<EstadoResultadosPorProyectoResponse>("/reportes/estado-resultados/por-proyecto", { params: { anio, mes } })).data,
    enabled,
  })
}

function descargar(blob: Blob, nombreArchivo: string) {
  const url = window.URL.createObjectURL(blob)
  const enlace = document.createElement("a")
  enlace.href = url
  enlace.download = nombreArchivo
  document.body.appendChild(enlace)
  enlace.click()
  enlace.remove()
  window.URL.revokeObjectURL(url)
}

export type VistaEstadoResultados = "mes" | "anio" | "acumulado" | "por-proyecto"

export async function descargarEstadoResultadosExcel(vista: VistaEstadoResultados, anio: number, mes: number) {
  const respuesta = await http.get(`/reportes/estado-resultados/${vista}/exportar/excel`, {
    params: { anio, mes },
    responseType: "blob",
  })
  descargar(respuesta.data, `estado-resultados-${vista}.xlsx`)
}

export async function descargarEstadoResultadosPdf(vista: VistaEstadoResultados, anio: number, mes: number) {
  const respuesta = await http.get(`/reportes/estado-resultados/${vista}/exportar/pdf`, {
    params: { anio, mes },
    responseType: "blob",
  })
  descargar(respuesta.data, `estado-resultados-${vista}.pdf`)
}

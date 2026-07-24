import { useQuery } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type { ReporteRentabilidadProyecto } from "@/types/rentabilidad-proyecto"

const QUERY_KEY = ["reporte-rentabilidad-proyecto"]

export function useReporteRentabilidadProyecto(proyectoId: number | undefined) {
  return useQuery({
    queryKey: [...QUERY_KEY, proyectoId],
    queryFn: async () => (await http.get<ReporteRentabilidadProyecto>(`/proyectos/${proyectoId}/reporte-rentabilidad`)).data,
    enabled: proyectoId !== undefined,
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

export async function descargarReporteRentabilidadExcel(proyectoId: number) {
  const respuesta = await http.get(`/proyectos/${proyectoId}/reporte-rentabilidad/exportar/excel`, { responseType: "blob" })
  descargar(respuesta.data, "reporte-rentabilidad.xlsx")
}

export async function descargarReporteRentabilidadPdf(proyectoId: number) {
  const respuesta = await http.get(`/proyectos/${proyectoId}/reporte-rentabilidad/exportar/pdf`, { responseType: "blob" })
  descargar(respuesta.data, "reporte-rentabilidad.pdf")
}

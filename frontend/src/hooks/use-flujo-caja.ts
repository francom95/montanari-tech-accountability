import { useQuery } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type { FlujoCajaResponse, Granularidad } from "@/types/flujo-caja"

const QUERY_KEY = ["flujo-caja"]

export function useFlujoCajaReal(desde: string, hasta: string, granularidad: Granularidad) {
  return useQuery({
    queryKey: [...QUERY_KEY, "real", desde, hasta, granularidad],
    queryFn: async () => (await http.get<FlujoCajaResponse>("/flujo-caja/real", { params: { desde, hasta, granularidad } })).data,
    enabled: Boolean(desde && hasta),
  })
}

export function useFlujoCajaProyectado(dias: number) {
  return useQuery({
    queryKey: [...QUERY_KEY, "proyectado", dias],
    queryFn: async () => (await http.get<FlujoCajaResponse>("/flujo-caja/proyectado", { params: { dias } })).data,
  })
}

export function useFlujoCajaCombinado(diasAtras: number, diasAdelante: number) {
  return useQuery({
    queryKey: [...QUERY_KEY, "combinado", diasAtras, diasAdelante],
    queryFn: async () =>
      (await http.get<FlujoCajaResponse>("/flujo-caja/combinado", { params: { diasAtras, diasAdelante } })).data,
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

export async function descargarFlujoCajaExcel(diasAtras: number, diasAdelante: number) {
  const respuesta = await http.get("/flujo-caja/exportar/excel", { params: { diasAtras, diasAdelante }, responseType: "blob" })
  descargar(respuesta.data, "flujo-de-caja.xlsx")
}

export async function descargarFlujoCajaPdf(diasAtras: number, diasAdelante: number) {
  const respuesta = await http.get("/flujo-caja/exportar/pdf", { params: { diasAtras, diasAdelante }, responseType: "blob" })
  descargar(respuesta.data, "flujo-de-caja.pdf")
}

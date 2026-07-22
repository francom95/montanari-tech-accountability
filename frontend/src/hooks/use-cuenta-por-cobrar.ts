import { useQuery } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { CuentaPorCobrar, CuentaPorCobrarFiltros } from "@/types/cuenta-por-cobrar"

const QUERY_KEY = ["cuenta-por-cobrar"]

export function useCuentasPorCobrar(filtros: CuentaPorCobrarFiltros) {
  return useQuery({
    queryKey: [...QUERY_KEY, filtros],
    queryFn: async () => (await http.get<CuentaPorCobrar>("/reportes/cuentas-por-cobrar", { params: filtros })).data,
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

export async function descargarCuentasPorCobrarExcel(filtros: CuentaPorCobrarFiltros) {
  const respuesta = await http.get("/reportes/cuentas-por-cobrar/exportar/excel", { params: filtros, responseType: "blob" })
  descargar(respuesta.data, "cuentas-por-cobrar.xlsx")
}

export async function descargarCuentasPorCobrarPdf(filtros: CuentaPorCobrarFiltros) {
  const respuesta = await http.get("/reportes/cuentas-por-cobrar/exportar/pdf", { params: filtros, responseType: "blob" })
  descargar(respuesta.data, "cuentas-por-cobrar.pdf")
}

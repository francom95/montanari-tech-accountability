import { useQuery } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { CuentaPorPagar, CuentaPorPagarFiltros } from "@/types/cuenta-por-pagar"

const QUERY_KEY = ["cuenta-por-pagar"]

export function useCuentasPorPagar(filtros: CuentaPorPagarFiltros) {
  return useQuery({
    queryKey: [...QUERY_KEY, filtros],
    queryFn: async () => (await http.get<CuentaPorPagar>("/reportes/cuentas-por-pagar", { params: filtros })).data,
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

export async function descargarCuentasPorPagarExcel(filtros: CuentaPorPagarFiltros) {
  const respuesta = await http.get("/reportes/cuentas-por-pagar/exportar/excel", { params: filtros, responseType: "blob" })
  descargar(respuesta.data, "cuentas-por-pagar.xlsx")
}

export async function descargarCuentasPorPagarPdf(filtros: CuentaPorPagarFiltros) {
  const respuesta = await http.get("/reportes/cuentas-por-pagar/exportar/pdf", { params: filtros, responseType: "blob" })
  descargar(respuesta.data, "cuentas-por-pagar.pdf")
}

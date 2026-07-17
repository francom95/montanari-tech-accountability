import { useQuery } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { Mayor, MayorFiltros } from "@/types/mayor"

const QUERY_KEY = ["mayor"]

export function useMayor(cuentaId: number | undefined, filtros: MayorFiltros, page: number, size: number) {
  return useQuery({
    queryKey: [...QUERY_KEY, cuentaId, filtros, page, size],
    queryFn: async () =>
      (
        await http.get<Mayor>(`/cuentas-contables/${cuentaId}/mayor`, {
          params: { ...filtros, page, size },
        })
      ).data,
    enabled: cuentaId !== undefined,
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

export async function descargarMayorExcel(cuentaId: number, codigo: string, filtros: MayorFiltros) {
  const respuesta = await http.get(`/cuentas-contables/${cuentaId}/mayor/exportar/excel`, {
    params: filtros,
    responseType: "blob",
  })
  descargar(respuesta.data, `mayor-${codigo}.xlsx`)
}

export async function descargarMayorPdf(cuentaId: number, codigo: string, filtros: MayorFiltros) {
  const respuesta = await http.get(`/cuentas-contables/${cuentaId}/mayor/exportar/pdf`, {
    params: filtros,
    responseType: "blob",
  })
  descargar(respuesta.data, `mayor-${codigo}.pdf`)
}

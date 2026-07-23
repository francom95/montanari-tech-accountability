import { useQuery } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { BalanceSumasYSaldos, BalanceSumasYSaldosFiltros } from "@/types/balance-sumas-y-saldos"

const QUERY_KEY = ["balance-sumas-y-saldos"]

export function useBalanceSumasYSaldos(filtros: BalanceSumasYSaldosFiltros) {
  return useQuery({
    queryKey: [...QUERY_KEY, filtros],
    queryFn: async () =>
      (await http.get<BalanceSumasYSaldos>("/reportes/balance-sumas-y-saldos", { params: filtros })).data,
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

export async function descargarBalanceSumasYSaldosExcel(filtros: BalanceSumasYSaldosFiltros) {
  const respuesta = await http.get("/reportes/balance-sumas-y-saldos/exportar/excel", {
    params: filtros,
    responseType: "blob",
  })
  descargar(respuesta.data, "balance-sumas-y-saldos.xlsx")
}

export async function descargarBalanceSumasYSaldosPdf(filtros: BalanceSumasYSaldosFiltros) {
  const respuesta = await http.get("/reportes/balance-sumas-y-saldos/exportar/pdf", {
    params: filtros,
    responseType: "blob",
  })
  descargar(respuesta.data, "balance-sumas-y-saldos.pdf")
}

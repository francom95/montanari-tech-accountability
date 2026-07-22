import { useQuery } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { ConciliacionResumen } from "@/types/conciliacion"

export function useConciliacionResumen(params: {
  cuentaBancariaId?: number; fechaDesde: string; fechaHasta: string; toleranciaDias?: number
}) {
  return useQuery({
    queryKey: ["conciliacion", params],
    queryFn: async () =>
      (
        await http.get<ConciliacionResumen>("/conciliacion/resumen", {
          params: {
            cuentaBancariaId: params.cuentaBancariaId,
            fechaDesde: params.fechaDesde,
            fechaHasta: params.fechaHasta,
            toleranciaDias: params.toleranciaDias ?? 3,
          },
        })
      ).data,
    enabled: !!params.cuentaBancariaId && !!params.fechaDesde && !!params.fechaHasta,
  })
}

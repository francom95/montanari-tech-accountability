import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type { MovimientoInversion, MovimientoInversionCrearInput } from "@/types/movimiento-inversion"

const QUERY_KEY = ["movimientoInversion"]

export function useMovimientosInversion(inversionId: number | undefined, params: { page?: number; size?: number } = {}) {
  return useQuery({
    queryKey: [...QUERY_KEY, inversionId, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<MovimientoInversion>>("/movimientos-inversion", {
          params: { inversionId, page: params.page ?? 0, size: params.size ?? 20 },
        })
      ).data,
    enabled: inversionId !== undefined,
  })
}

export function useCrearMovimientoInversion() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: MovimientoInversionCrearInput) =>
      (await http.post<MovimientoInversion>("/movimientos-inversion", v)).data,
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: QUERY_KEY })
      await qc.invalidateQueries({ queryKey: ["inversion"] })
    },
  })
}

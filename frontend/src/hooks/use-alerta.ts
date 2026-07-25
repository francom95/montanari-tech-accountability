import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type { Alerta, EstadoAlerta, TipoAlerta } from "@/types/alerta"

const QUERY_KEY = ["alerta"]
const CONTADOR_KEY = ["alerta-contador"]

export function useAlertas(params: { estado?: EstadoAlerta; tipo?: TipoAlerta; page?: number; size?: number; enabled?: boolean }) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<Alerta>>("/alertas", {
          params: { estado: params.estado || undefined, tipo: params.tipo || undefined, page: params.page ?? 0, size: params.size ?? 20 },
        })
      ).data,
    enabled: params.enabled ?? true,
  })
}

/** Badge del header (F9.1): refresca solo, sin que el usuario tenga que recargar la página. */
export function useContadorAlertas() {
  return useQuery({
    queryKey: CONTADOR_KEY,
    queryFn: async () => (await http.get<{ noLeidas: number }>("/alertas/contador")).data,
    refetchInterval: 60_000,
  })
}

export function useMarcarLeidaAlerta() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.post(`/alertas/${id}/marcar-leida`) },
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: QUERY_KEY })
      await qc.invalidateQueries({ queryKey: CONTADOR_KEY })
    },
  })
}

export function useSincronizarAlertas() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async () => { await http.post("/alertas/sincronizar") },
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: QUERY_KEY })
      await qc.invalidateQueries({ queryKey: CONTADOR_KEY })
    },
  })
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type { TarjetaCredito, TarjetaCreditoCrearInput, TarjetaCreditoEditarInput } from "@/types/tarjeta-credito"

const QUERY_KEY = ["tarjetaCredito"]

export function useTarjetasCredito(params: { texto?: string; activo?: boolean; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () => (await http.get<PageResponse<TarjetaCredito>>("/tarjetas-credito", { params: { texto: params.texto || undefined, activo: params.activo, page: params.page ?? 0, size: params.size ?? 10 } })).data,
  })
}

export function useCrearTarjetaCredito() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: TarjetaCreditoCrearInput) => (await http.post<TarjetaCredito>("/tarjetas-credito", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarTarjetaCredito() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: TarjetaCreditoEditarInput }) => (await http.put<TarjetaCredito>(`/tarjetas-credito/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useCambiarEstadoTarjetaCredito() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, activo }: { id: number; activo: boolean }) => (await http.patch<TarjetaCredito>(`/tarjetas-credito/${id}/${activo ? "desactivar" : "activar"}`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarTarjetaCredito() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/tarjetas-credito/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

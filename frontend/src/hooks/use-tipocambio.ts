import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type { TipoCambio, TipoCambioCrearInput, TipoCambioEditarInput } from "@/types/tipocambio"

const QUERY_KEY = ["tipocambio"]

export function useTipoCambios(params: { texto?: string; activo?: boolean; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () => (await http.get<PageResponse<TipoCambio>>("/api/v1/tipo-cambios", { params: { texto: params.texto || undefined, activo: params.activo, page: params.page ?? 0, size: params.size ?? 10 } })).data,
  })
}

export function useCrearTipoCambio() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: TipoCambioCrearInput) => (await http.post<TipoCambio>("/api/v1/tipo-cambios", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarTipoCambio() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: TipoCambioEditarInput }) => (await http.put<TipoCambio>(`/api/v1/tipo-cambios/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useCambiarEstadoTipoCambio() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, activo }: { id: number; activo: boolean }) => (await http.patch<TipoCambio>(`/api/v1/tipo-cambios/${id}/${activo ? "desactivar" : "activar"}`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarTipoCambio() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/api/v1/tipo-cambios/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type { Compromiso, CompromisoCrearInput, CompromisoEditarInput, EstadoCompromiso } from "@/types/compromiso"

const QUERY_KEY = ["compromiso"]

export function useCompromisos(params: { texto?: string; estado?: EstadoCompromiso; activo?: boolean; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<Compromiso>>("/compromisos", {
          params: {
            texto: params.texto || undefined,
            estado: params.estado || undefined,
            activo: params.activo,
            page: params.page ?? 0,
            size: params.size ?? 10,
          },
        })
      ).data,
  })
}

export function useCrearCompromiso() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: CompromisoCrearInput) => (await http.post<Compromiso>("/compromisos", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarCompromiso() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: CompromisoEditarInput }) =>
      (await http.put<Compromiso>(`/compromisos/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useCambiarEstadoCompromiso() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, activo }: { id: number; activo: boolean }) =>
      (await http.patch<Compromiso>(`/compromisos/${id}/${activo ? "desactivar" : "activar"}`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarCompromiso() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/compromisos/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

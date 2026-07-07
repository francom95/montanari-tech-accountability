import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type { Comisionista, ComisionistaCrearInput, ComisionistaEditarInput } from "@/types/comisionista"

const QUERY_KEY = ["comisionista"]

export function useComisionistas(params: { texto?: string; activo?: boolean; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () => (await http.get<PageResponse<Comisionista>>("/comisionistas", { params: { texto: params.texto || undefined, activo: params.activo, page: params.page ?? 0, size: params.size ?? 10 } })).data,
  })
}

export function useCrearComisionista() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: ComisionistaCrearInput) => (await http.post<Comisionista>("/comisionistas", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarComisionista() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: ComisionistaEditarInput }) => (await http.put<Comisionista>(`/comisionistas/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useCambiarEstadoComisionista() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, activo }: { id: number; activo: boolean }) => (await http.patch<Comisionista>(`/comisionistas/${id}/${activo ? "desactivar" : "activar"}`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarComisionista() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/comisionistas/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

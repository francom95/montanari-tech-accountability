import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type { Jurisdiccion, JurisdiccionCrearInput, JurisdiccionEditarInput } from "@/types/jurisdiccion"

const QUERY_KEY = ["jurisdiccion"]

export function useJurisdiccions(params: { texto?: string; activo?: boolean; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () => (await http.get<PageResponse<Jurisdiccion>>("/api/v1/jurisdicciones", { params: { texto: params.texto || undefined, activo: params.activo, page: params.page ?? 0, size: params.size ?? 10 } })).data,
  })
}

export function useCrearJurisdiccion() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: JurisdiccionCrearInput) => (await http.post<Jurisdiccion>("/api/v1/jurisdicciones", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarJurisdiccion() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: JurisdiccionEditarInput }) => (await http.put<Jurisdiccion>(`/api/v1/jurisdicciones/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useCambiarEstadoJurisdiccion() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, activo }: { id: number; activo: boolean }) => (await http.patch<Jurisdiccion>(`/api/v1/jurisdicciones/${id}/${activo ? "desactivar" : "activar"}`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarJurisdiccion() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/api/v1/jurisdicciones/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

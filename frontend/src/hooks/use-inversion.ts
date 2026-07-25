import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type { EstadoInversion, Inversion, InversionCrearInput, InversionEditarInput } from "@/types/inversion"

const QUERY_KEY = ["inversion"]

export function useInversiones(params: { texto?: string; estado?: EstadoInversion; activo?: boolean; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<Inversion>>("/inversiones", {
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

export function useInversion(id: number | undefined) {
  return useQuery({
    queryKey: [...QUERY_KEY, id],
    queryFn: async () => (await http.get<Inversion>(`/inversiones/${id}`)).data,
    enabled: id !== undefined,
  })
}

export function useCrearInversion() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: InversionCrearInput) => (await http.post<Inversion>("/inversiones", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarInversion() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: InversionEditarInput }) =>
      (await http.put<Inversion>(`/inversiones/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useCambiarEstadoInversion() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, activo }: { id: number; activo: boolean }) =>
      (await http.patch<Inversion>(`/inversiones/${id}/${activo ? "desactivar" : "activar"}`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarInversion() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/inversiones/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

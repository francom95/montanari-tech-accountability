import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type { Asiento, AsientoCrearInput, AsientoEditarInput, EstadoAsiento } from "@/types/asiento"

const QUERY_KEY = ["asiento"]

export function useAsientos(params: { texto?: string; estado?: EstadoAsiento; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<Asiento>>("/asientos", {
          params: {
            texto: params.texto || undefined,
            estado: params.estado || undefined,
            page: params.page ?? 0,
            size: params.size ?? 10,
            sort: "fecha,desc",
          },
        })
      ).data,
  })
}

export function useAsiento(id: number | undefined) {
  return useQuery({
    queryKey: [...QUERY_KEY, id],
    queryFn: async () => (await http.get<Asiento>(`/asientos/${id}`)).data,
    enabled: id !== undefined,
  })
}

export function useCrearAsiento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: AsientoCrearInput) => (await http.post<Asiento>("/asientos", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarAsiento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: AsientoEditarInput }) =>
      (await http.put<Asiento>(`/asientos/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useConfirmarAsiento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => (await http.patch<Asiento>(`/asientos/${id}/confirmar`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarAsiento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/asientos/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

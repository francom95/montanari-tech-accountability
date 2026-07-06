import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type { Concepto, ConceptoCrearInput, ConceptoEditarInput } from "@/types/concepto"

const QUERY_KEY = ["concepto"]

export function useConceptos(params: { texto?: string; activo?: boolean; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () => (await http.get<PageResponse<Concepto>>("/api/v1/conceptos", { params: { texto: params.texto || undefined, activo: params.activo, page: params.page ?? 0, size: params.size ?? 10 } })).data,
  })
}

export function useCrearConcepto() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: ConceptoCrearInput) => (await http.post<Concepto>("/api/v1/conceptos", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarConcepto() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: ConceptoEditarInput }) => (await http.put<Concepto>(`/api/v1/conceptos/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useCambiarEstadoConcepto() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, activo }: { id: number; activo: boolean }) => (await http.patch<Concepto>(`/api/v1/conceptos/${id}/${activo ? "desactivar" : "activar"}`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarConcepto() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/api/v1/conceptos/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

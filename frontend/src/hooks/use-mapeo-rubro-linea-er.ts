import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { MapeoRubroLineaEr, MapeoRubroLineaErCrearInput, MapeoRubroLineaErEditarInput } from "@/types/mapeo-rubro-linea-er"

const QUERY_KEY = ["mapeo-rubro-linea-er"]

export function useMapeosRubroLineaEr() {
  return useQuery({
    queryKey: QUERY_KEY,
    queryFn: async () => (await http.get<MapeoRubroLineaEr[]>("/mapeos-rubro-linea-er")).data,
  })
}

export function useCrearMapeoRubroLineaEr() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: MapeoRubroLineaErCrearInput) => (await http.post<MapeoRubroLineaEr>("/mapeos-rubro-linea-er", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarMapeoRubroLineaEr() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: MapeoRubroLineaErEditarInput }) =>
      (await http.put<MapeoRubroLineaEr>(`/mapeos-rubro-linea-er/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarMapeoRubroLineaEr() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/mapeos-rubro-linea-er/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

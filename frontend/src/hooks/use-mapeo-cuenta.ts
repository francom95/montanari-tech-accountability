import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type { ConceptoContable, MapeoCuenta, MapeoCuentaCrearInput, MapeoCuentaEditarInput } from "@/types/mapeo-cuenta"

const QUERY_KEY = ["mapeo-cuenta"]

export function useMapeosCuenta(params: { concepto?: ConceptoContable; activo?: boolean; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<MapeoCuenta>>("/mapeos-cuenta", {
          params: { concepto: params.concepto || undefined, activo: params.activo, page: params.page ?? 0, size: params.size ?? 20 },
        })
      ).data,
  })
}

export function useCrearMapeoCuenta() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: MapeoCuentaCrearInput) => (await http.post<MapeoCuenta>("/mapeos-cuenta", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarMapeoCuenta() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: MapeoCuentaEditarInput }) =>
      (await http.put<MapeoCuenta>(`/mapeos-cuenta/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useCambiarEstadoMapeoCuenta() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, activo }: { id: number; activo: boolean }) =>
      (await http.patch<MapeoCuenta>(`/mapeos-cuenta/${id}/${activo ? "desactivar" : "activar"}`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarMapeoCuenta() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/mapeos-cuenta/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

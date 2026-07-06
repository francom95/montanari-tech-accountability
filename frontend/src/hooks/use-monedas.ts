import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type { Moneda, MonedaCrearInput, MonedaEditarInput } from "@/types/moneda"

/**
 * Molde de referencia de PL-2 (F1.8): un hook de lista con filtros/paginado
 * + un hook de mutación por operación, todos invalidando la misma
 * queryKey. Para una entidad nueva: copiar el archivo, cambiar el tipo y
 * las rutas de `http`.
 */
const MONEDAS_QUERY_KEY = ["monedas"]

export function useMonedas(params: { texto?: string; activo?: boolean; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...MONEDAS_QUERY_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<Moneda>>("/monedas", {
          params: {
            texto: params.texto || undefined,
            activo: params.activo,
            page: params.page ?? 0,
            size: params.size ?? 10,
          },
        })
      ).data,
  })
}

export function useCrearMoneda() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (valores: MonedaCrearInput) => (await http.post<Moneda>("/monedas", valores)).data,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: MONEDAS_QUERY_KEY })
    },
  })
}

export function useEditarMoneda() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: MonedaEditarInput }) =>
      (await http.put<Moneda>(`/monedas/${id}`, valores)).data,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: MONEDAS_QUERY_KEY })
    },
  })
}

export function useCambiarEstadoMoneda() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, activo }: { id: number; activo: boolean }) =>
      (await http.patch<Moneda>(`/monedas/${id}/${activo ? "desactivar" : "activar"}`)).data,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: MONEDAS_QUERY_KEY })
    },
  })
}

export function useEliminarMoneda() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => {
      await http.delete(`/monedas/${id}`)
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: MONEDAS_QUERY_KEY })
    },
  })
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type {
  EstadoPeriodo,
  GenerarAutomaticosPeriodosResponse,
  Periodo,
  PeriodoResumen,
} from "@/types/periodo"

const QUERY_KEY = ["periodo"]

export function usePeriodos(params: { estado?: EstadoPeriodo; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<Periodo>>("/periodos", {
          params: { estado: params.estado || undefined, page: params.page ?? 0, size: params.size ?? 50 },
        })
      ).data,
  })
}

export function usePeriodoResumen(id: number | undefined) {
  return useQuery({
    queryKey: [...QUERY_KEY, id, "resumen"],
    queryFn: async () => (await http.get<PeriodoResumen>(`/periodos/${id}/resumen`)).data,
    enabled: id !== undefined,
  })
}

export function useGenerarAutomaticosPeriodos() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async () =>
      (await http.post<GenerarAutomaticosPeriodosResponse>("/periodos/generar-automaticos")).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useCerrarPeriodo() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, motivo }: { id: number; motivo: string }) =>
      (await http.patch<Periodo>(`/periodos/${id}/cerrar`, { motivo })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useMarcarEnRevisionPeriodo() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => (await http.patch<Periodo>(`/periodos/${id}/marcar-en-revision`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useReabrirPeriodo() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, motivo }: { id: number; motivo: string }) =>
      (await http.patch<Periodo>(`/periodos/${id}/reabrir`, { motivo })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

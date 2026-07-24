import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type {
  GenerarAutomaticosResponse,
  Vencimiento,
  VencimientoBusquedaFiltros,
  VencimientoCrearInput,
  VencimientoEditarInput,
} from "@/types/vencimiento"

const QUERY_KEY = ["vencimiento"]

export function useVencimientos(params: VencimientoBusquedaFiltros & { page?: number; size?: number }) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<Vencimiento>>("/vencimientos", {
          params: {
            tipo: params.tipo || undefined,
            estado: params.estado || undefined,
            fechaDesde: params.fechaDesde || undefined,
            fechaHasta: params.fechaHasta || undefined,
            proyectoId: params.proyectoId ?? undefined,
            proveedorId: params.proveedorId ?? undefined,
            tarjetaId: params.tarjetaId ?? undefined,
            page: params.page ?? 0,
            size: params.size ?? 500,
            sort: "fecha,asc",
          },
        })
      ).data,
  })
}

export function useProximosVencimientos(dias: number) {
  return useQuery({
    queryKey: [...QUERY_KEY, "proximos", dias],
    queryFn: async () => (await http.get<Vencimiento[]>("/vencimientos/proximos", { params: { dias } })).data,
  })
}

export function useCrearVencimiento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: VencimientoCrearInput) => (await http.post<Vencimiento>("/vencimientos", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarVencimiento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: VencimientoEditarInput }) =>
      (await http.put<Vencimiento>(`/vencimientos/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useMarcarPagadoVencimiento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, asientoId }: { id: number; asientoId?: number }) =>
      (await http.patch<Vencimiento>(`/vencimientos/${id}/marcar-pagado`, null, { params: { asientoId } })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useReprogramarVencimiento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, nuevaFecha, motivo }: { id: number; nuevaFecha: string; motivo?: string }) =>
      (await http.patch<Vencimiento>(`/vencimientos/${id}/reprogramar`, { nuevaFecha, motivo })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useCancelarVencimiento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, motivo }: { id: number; motivo: string }) =>
      (await http.patch<Vencimiento>(`/vencimientos/${id}/cancelar`, { motivo })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useGenerarAutomaticosVencimientos() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async () =>
      (await http.post<GenerarAutomaticosResponse>("/vencimientos/generar-automaticos")).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

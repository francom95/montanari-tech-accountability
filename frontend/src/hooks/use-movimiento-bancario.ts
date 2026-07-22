import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type {
  EstadoMovimientoBancario,
  MovimientoBancario,
  MovimientoBancarioCorregirInput,
  MovimientoBancarioCrearInput,
} from "@/types/movimiento-bancario"

const QUERY_KEY = ["movimiento-bancario"]

export function useMovimientosBancarios(params: {
  cuentaBancariaId?: number; estado?: EstadoMovimientoBancario; fechaDesde?: string; fechaHasta?: string
  page?: number; size?: number
}) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<MovimientoBancario>>("/movimientos-bancarios", {
          params: {
            cuentaBancariaId: params.cuentaBancariaId,
            estado: params.estado,
            fechaDesde: params.fechaDesde || undefined,
            fechaHasta: params.fechaHasta || undefined,
            page: params.page ?? 0,
            size: params.size ?? 10,
            sort: "fecha,desc",
          },
        })
      ).data,
  })
}

export function useContadorPendientes(cuentaBancariaId?: number) {
  return useQuery({
    queryKey: [...QUERY_KEY, "contador", cuentaBancariaId],
    queryFn: async () =>
      (await http.get<{ cantidad: number }>("/movimientos-bancarios/pendientes/contador", { params: { cuentaBancariaId } })).data,
  })
}

function invalidar(qc: ReturnType<typeof useQueryClient>) {
  return qc.invalidateQueries({ queryKey: QUERY_KEY })
}

export function useCrearMovimientoBancario() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: MovimientoBancarioCrearInput) => (await http.post<MovimientoBancario>("/movimientos-bancarios", v)).data,
    onSuccess: () => invalidar(qc),
  })
}

export function useCorregirMovimientoBancario() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: MovimientoBancarioCorregirInput }) =>
      (await http.put<MovimientoBancario>(`/movimientos-bancarios/${id}`, valores)).data,
    onSuccess: () => invalidar(qc),
  })
}

export function useConfirmarMovimientoBancario() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => (await http.patch<MovimientoBancario>(`/movimientos-bancarios/${id}/confirmar`)).data,
    onSuccess: () => invalidar(qc),
  })
}

export function useImputarMovimientoBancario() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, cuentaContableId }: { id: number; cuentaContableId: number }) =>
      (await http.patch<MovimientoBancario>(`/movimientos-bancarios/${id}/imputar`, { cuentaContableId })).data,
    onSuccess: () => invalidar(qc),
  })
}

export function useAsociarMovimientoBancario() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, asientoNumero }: { id: number; asientoNumero: number }) =>
      (await http.patch<MovimientoBancario>(`/movimientos-bancarios/${id}/asociar`, { asientoNumero })).data,
    onSuccess: () => invalidar(qc),
  })
}

export function useDescartarMovimientoBancario() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, motivo }: { id: number; motivo: string }) =>
      (await http.patch<MovimientoBancario>(`/movimientos-bancarios/${id}/descartar`, { motivo })).data,
    onSuccess: () => invalidar(qc),
  })
}

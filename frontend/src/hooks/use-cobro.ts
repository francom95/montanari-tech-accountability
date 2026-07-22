import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { Cobro, CobroCrearInput, CobroEditarInput, EstadoCobro, SaldoFactura } from "@/types/cobro"
import type { PageResponse } from "@/types/auth"

const QUERY_KEY = ["cobro"]

export function useCobros(params: { estado?: EstadoCobro; clienteId?: number; fechaDesde?: string; fechaHasta?: string; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<Cobro>>("/cobros", {
          params: {
            estado: params.estado || undefined,
            clienteId: params.clienteId ?? undefined,
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

export function useSaldoFacturaVenta(facturaVentaId: number | undefined) {
  return useQuery({
    queryKey: [...QUERY_KEY, "saldo-venta", facturaVentaId],
    queryFn: async () => (await http.get<SaldoFactura>(`/cobros/saldo-venta/${facturaVentaId}`)).data,
    enabled: facturaVentaId !== undefined,
  })
}

export function useCrearCobro() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: CobroCrearInput) => (await http.post<Cobro>("/cobros", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarCobro() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: CobroEditarInput }) =>
      (await http.put<Cobro>(`/cobros/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarCobro() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/cobros/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useConfirmarCobro() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => (await http.patch<Cobro>(`/cobros/${id}/confirmar`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useAnularCobro() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, motivo }: { id: number; motivo: string }) =>
      (await http.patch<Cobro>(`/cobros/${id}/anular`, { motivo })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useAplicarAnticipoCobro() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, facturaVentaId, monto, fecha }: { id: number; facturaVentaId: number; monto: number; fecha: string }) =>
      (await http.post<Cobro>(`/cobros/${id}/aplicar-anticipo`, { facturaVentaId, monto, fecha })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

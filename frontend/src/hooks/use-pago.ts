import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { EstadoPago, Pago, PagoCrearInput, PagoEditarInput, SaldoFacturaCompra } from "@/types/pago"
import type { PageResponse } from "@/types/auth"

const QUERY_KEY = ["pago"]

export function usePagos(params: { estado?: EstadoPago; proveedorId?: number; fechaDesde?: string; fechaHasta?: string; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<Pago>>("/pagos", {
          params: {
            estado: params.estado || undefined,
            proveedorId: params.proveedorId ?? undefined,
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

export function useSaldoFacturaCompra(facturaCompraId: number | undefined) {
  return useQuery({
    queryKey: [...QUERY_KEY, "saldo-compra", facturaCompraId],
    queryFn: async () => (await http.get<SaldoFacturaCompra>(`/pagos/saldo-compra/${facturaCompraId}`)).data,
    enabled: facturaCompraId !== undefined,
  })
}

export function useCrearPago() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: PagoCrearInput) => (await http.post<Pago>("/pagos", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarPago() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: PagoEditarInput }) =>
      (await http.put<Pago>(`/pagos/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarPago() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/pagos/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useConfirmarPago() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => (await http.patch<Pago>(`/pagos/${id}/confirmar`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useAnularPago() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, motivo }: { id: number; motivo: string }) =>
      (await http.patch<Pago>(`/pagos/${id}/anular`, { motivo })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useAplicarAnticipoPago() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, facturaCompraId, monto, fecha }: { id: number; facturaCompraId: number; monto: number; fecha: string }) =>
      (await http.post<Pago>(`/pagos/${id}/aplicar-anticipo`, { facturaCompraId, monto, fecha })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

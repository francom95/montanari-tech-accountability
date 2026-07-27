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

/** Búsqueda global (F9.2): registro puntual por id, para el filtro exacto ?id= de la lista. */
export function usePago(id: number | undefined) {
  return useQuery({
    queryKey: [...QUERY_KEY, id],
    queryFn: async () => (await http.get<Pago>(`/pagos/${id}`)).data,
    enabled: id !== undefined,
  })
}

export function useSaldoFacturaCompra(facturaCompraId: number | undefined) {
  return useQuery({
    queryKey: [...QUERY_KEY, "saldo-compra", facturaCompraId],
    queryFn: async () => (await http.get<SaldoFacturaCompra>(`/pagos/saldo-compra/${facturaCompraId}`)).data,
    enabled: facturaCompraId !== undefined,
  })
}

/** F9.3: campos opcionales de override cuando la fecha cae en un período cerrado (ver usePeriodoCerradoOverride). */
type OverridePeriodo = { confirmarPeriodoCerrado?: boolean; motivoOverridePeriodo?: string }

export function useCrearPago() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ confirmarPeriodoCerrado, motivoOverridePeriodo, ...v }: PagoCrearInput & OverridePeriodo) =>
      (await http.post<Pago>("/pagos", v, { params: { confirmarPeriodoCerrado, motivoOverridePeriodo } })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarPago() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores, confirmarPeriodoCerrado, motivoOverridePeriodo }: { id: number; valores: PagoEditarInput } & OverridePeriodo) =>
      (await http.put<Pago>(`/pagos/${id}`, valores, { params: { confirmarPeriodoCerrado, motivoOverridePeriodo } })).data,
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
    mutationFn: async ({ id, confirmarPeriodoCerrado, motivoOverridePeriodo }: { id: number } & OverridePeriodo) =>
      (await http.patch<Pago>(`/pagos/${id}/confirmar`, null, { params: { confirmarPeriodoCerrado, motivoOverridePeriodo } })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useAnularPago() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, motivo, confirmarPeriodoCerrado, motivoOverridePeriodo }: { id: number; motivo: string } & OverridePeriodo) =>
      (await http.patch<Pago>(`/pagos/${id}/anular`, { motivo }, { params: { confirmarPeriodoCerrado, motivoOverridePeriodo } })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useAplicarAnticipoPago() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, facturaCompraId, monto, fecha, confirmarPeriodoCerrado, motivoOverridePeriodo }: { id: number; facturaCompraId: number; monto: number; fecha: string } & OverridePeriodo) =>
      (await http.post<Pago>(`/pagos/${id}/aplicar-anticipo`, { facturaCompraId, monto, fecha }, { params: { confirmarPeriodoCerrado, motivoOverridePeriodo } })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

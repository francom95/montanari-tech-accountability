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

/** Búsqueda global (F9.2): registro puntual por id, para el filtro exacto ?id= de la lista. */
export function useCobro(id: number | undefined) {
  return useQuery({
    queryKey: [...QUERY_KEY, id],
    queryFn: async () => (await http.get<Cobro>(`/cobros/${id}`)).data,
    enabled: id !== undefined,
  })
}

export function useSaldoFacturaVenta(facturaVentaId: number | undefined) {
  return useQuery({
    queryKey: [...QUERY_KEY, "saldo-venta", facturaVentaId],
    queryFn: async () => (await http.get<SaldoFactura>(`/cobros/saldo-venta/${facturaVentaId}`)).data,
    enabled: facturaVentaId !== undefined,
  })
}

/** F9.3: campos opcionales de override cuando la fecha cae en un período cerrado (ver usePeriodoCerradoOverride). */
type OverridePeriodo = { confirmarPeriodoCerrado?: boolean; motivoOverridePeriodo?: string }

export function useCrearCobro() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ confirmarPeriodoCerrado, motivoOverridePeriodo, ...v }: CobroCrearInput & OverridePeriodo) =>
      (await http.post<Cobro>("/cobros", v, { params: { confirmarPeriodoCerrado, motivoOverridePeriodo } })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarCobro() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores, confirmarPeriodoCerrado, motivoOverridePeriodo }: { id: number; valores: CobroEditarInput } & OverridePeriodo) =>
      (await http.put<Cobro>(`/cobros/${id}`, valores, { params: { confirmarPeriodoCerrado, motivoOverridePeriodo } })).data,
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
    mutationFn: async ({ id, confirmarPeriodoCerrado, motivoOverridePeriodo }: { id: number } & OverridePeriodo) =>
      (await http.patch<Cobro>(`/cobros/${id}/confirmar`, null, { params: { confirmarPeriodoCerrado, motivoOverridePeriodo } })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useAnularCobro() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, motivo, confirmarPeriodoCerrado, motivoOverridePeriodo }: { id: number; motivo: string } & OverridePeriodo) =>
      (await http.patch<Cobro>(`/cobros/${id}/anular`, { motivo }, { params: { confirmarPeriodoCerrado, motivoOverridePeriodo } })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useAplicarAnticipoCobro() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, facturaVentaId, monto, fecha, confirmarPeriodoCerrado, motivoOverridePeriodo }: { id: number; facturaVentaId: number; monto: number; fecha: string } & OverridePeriodo) =>
      (await http.post<Cobro>(`/cobros/${id}/aplicar-anticipo`, { facturaVentaId, monto, fecha }, { params: { confirmarPeriodoCerrado, motivoOverridePeriodo } })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

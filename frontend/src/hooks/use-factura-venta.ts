import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type {
  EstadoFactura,
  FacturaVenta,
  FacturaVentaCrearInput,
  FacturaVentaEditarInput,
} from "@/types/factura-venta"

const QUERY_KEY = ["factura-venta"]

export function useFacturasVenta(params: {
  texto?: string; estado?: EstadoFactura; clienteId?: number; proyectoId?: number;
  fechaDesde?: string; fechaHasta?: string; page?: number; size?: number
}) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<FacturaVenta>>("/facturas-venta", {
          params: {
            texto: params.texto || undefined,
            estado: params.estado || undefined,
            clienteId: params.clienteId ?? undefined,
            proyectoId: params.proyectoId ?? undefined,
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

export function useCrearFacturaVenta() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: FacturaVentaCrearInput) => (await http.post<FacturaVenta>("/facturas-venta", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarFacturaVenta() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: FacturaVentaEditarInput }) =>
      (await http.put<FacturaVenta>(`/facturas-venta/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarFacturaVenta() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/facturas-venta/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useConfirmarFacturaVenta() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => (await http.patch<FacturaVenta>(`/facturas-venta/${id}/confirmar`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useAnularFacturaVenta() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, motivo }: { id: number; motivo: string }) =>
      (await http.patch<FacturaVenta>(`/facturas-venta/${id}/anular`, { motivo })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

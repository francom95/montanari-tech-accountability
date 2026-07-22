import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type {
  EstadoFacturaCompra,
  FacturaCompra,
  FacturaCompraCrearInput,
  FacturaCompraEditarInput,
} from "@/types/factura-compra"

const QUERY_KEY = ["factura-compra"]

export function useFacturasCompra(params: {
  texto?: string; estado?: EstadoFacturaCompra; proveedorId?: number; proyectoId?: number;
  fechaDesde?: string; fechaHasta?: string; page?: number; size?: number
}) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<FacturaCompra>>("/facturas-compra", {
          params: {
            texto: params.texto || undefined,
            estado: params.estado || undefined,
            proveedorId: params.proveedorId ?? undefined,
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

export function useCrearFacturaCompra() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: FacturaCompraCrearInput) => (await http.post<FacturaCompra>("/facturas-compra", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarFacturaCompra() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: FacturaCompraEditarInput }) =>
      (await http.put<FacturaCompra>(`/facturas-compra/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarFacturaCompra() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/facturas-compra/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useConfirmarFacturaCompra() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => (await http.patch<FacturaCompra>(`/facturas-compra/${id}/confirmar`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useAnularFacturaCompra() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, motivo }: { id: number; motivo: string }) =>
      (await http.patch<FacturaCompra>(`/facturas-compra/${id}/anular`, { motivo })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

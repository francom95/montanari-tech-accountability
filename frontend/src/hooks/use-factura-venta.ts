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

/** Búsqueda global (F9.2): registro puntual por id, para el filtro exacto ?id= de la lista. */
export function useFacturaVenta(id: number | undefined) {
  return useQuery({
    queryKey: [...QUERY_KEY, id],
    queryFn: async () => (await http.get<FacturaVenta>(`/facturas-venta/${id}`)).data,
    enabled: id !== undefined,
  })
}

/** F9.3: campos opcionales de override cuando la fecha cae en un período cerrado (ver usePeriodoCerradoOverride). */
type OverridePeriodo = { confirmarPeriodoCerrado?: boolean; motivoOverridePeriodo?: string }

export function useCrearFacturaVenta() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ confirmarPeriodoCerrado, motivoOverridePeriodo, ...v }: FacturaVentaCrearInput & OverridePeriodo) =>
      (await http.post<FacturaVenta>("/facturas-venta", v, { params: { confirmarPeriodoCerrado, motivoOverridePeriodo } })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarFacturaVenta() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores, confirmarPeriodoCerrado, motivoOverridePeriodo }: { id: number; valores: FacturaVentaEditarInput } & OverridePeriodo) =>
      (await http.put<FacturaVenta>(`/facturas-venta/${id}`, valores, { params: { confirmarPeriodoCerrado, motivoOverridePeriodo } })).data,
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
    mutationFn: async ({ id, confirmarPeriodoCerrado, motivoOverridePeriodo }: { id: number } & OverridePeriodo) =>
      (await http.patch<FacturaVenta>(`/facturas-venta/${id}/confirmar`, null, { params: { confirmarPeriodoCerrado, motivoOverridePeriodo } })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useAnularFacturaVenta() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, motivo, confirmarPeriodoCerrado, motivoOverridePeriodo }: { id: number; motivo: string } & OverridePeriodo) =>
      (await http.patch<FacturaVenta>(`/facturas-venta/${id}/anular`, { motivo }, { params: { confirmarPeriodoCerrado, motivoOverridePeriodo } })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

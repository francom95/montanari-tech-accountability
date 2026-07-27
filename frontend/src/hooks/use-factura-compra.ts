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

/** Búsqueda global (F9.2): registro puntual por id, para el filtro exacto ?id= de la lista. */
export function useFacturaCompra(id: number | undefined) {
  return useQuery({
    queryKey: [...QUERY_KEY, id],
    queryFn: async () => (await http.get<FacturaCompra>(`/facturas-compra/${id}`)).data,
    enabled: id !== undefined,
  })
}

/** F9.3: campos opcionales de override cuando la fecha cae en un período cerrado (ver usePeriodoCerradoOverride). */
type OverridePeriodo = { confirmarPeriodoCerrado?: boolean; motivoOverridePeriodo?: string }

export function useCrearFacturaCompra() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ confirmarPeriodoCerrado, motivoOverridePeriodo, ...v }: FacturaCompraCrearInput & OverridePeriodo) =>
      (await http.post<FacturaCompra>("/facturas-compra", v, { params: { confirmarPeriodoCerrado, motivoOverridePeriodo } })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarFacturaCompra() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores, confirmarPeriodoCerrado, motivoOverridePeriodo }: { id: number; valores: FacturaCompraEditarInput } & OverridePeriodo) =>
      (await http.put<FacturaCompra>(`/facturas-compra/${id}`, valores, { params: { confirmarPeriodoCerrado, motivoOverridePeriodo } })).data,
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
    mutationFn: async ({ id, confirmarPeriodoCerrado, motivoOverridePeriodo }: { id: number } & OverridePeriodo) =>
      (await http.patch<FacturaCompra>(`/facturas-compra/${id}/confirmar`, null, { params: { confirmarPeriodoCerrado, motivoOverridePeriodo } })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useAnularFacturaCompra() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, motivo, confirmarPeriodoCerrado, motivoOverridePeriodo }: { id: number; motivo: string } & OverridePeriodo) =>
      (await http.patch<FacturaCompra>(`/facturas-compra/${id}/anular`, { motivo }, { params: { confirmarPeriodoCerrado, motivoOverridePeriodo } })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type {
  EstadoPendiente,
  PendienteAdministrativo,
  PendienteAdministrativoCrearInput,
  PendienteAdministrativoEditarInput,
  PrioridadPendiente,
} from "@/types/pendiente-administrativo"

const QUERY_KEY = ["pendienteAdministrativo"]

export function usePendientesAdministrativos(params: {
  texto?: string
  estado?: EstadoPendiente
  prioridad?: PrioridadPendiente
  responsableId?: number
  categoria?: string
  activo?: boolean
  page?: number
  size?: number
}) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<PendienteAdministrativo>>("/pendientes-administrativos", {
          params: {
            texto: params.texto || undefined,
            estado: params.estado || undefined,
            prioridad: params.prioridad || undefined,
            responsableId: params.responsableId ?? undefined,
            categoria: params.categoria || undefined,
            activo: params.activo,
            page: params.page ?? 0,
            size: params.size ?? 10,
          },
        })
      ).data,
  })
}

/** Búsqueda global (F9.2): registro puntual por id, para el filtro exacto ?id= de la lista. */
export function usePendienteAdministrativo(id: number | undefined) {
  return useQuery({
    queryKey: [...QUERY_KEY, id],
    queryFn: async () => (await http.get<PendienteAdministrativo>(`/pendientes-administrativos/${id}`)).data,
    enabled: id !== undefined,
  })
}

export function useCrearPendienteAdministrativo() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: PendienteAdministrativoCrearInput) =>
      (await http.post<PendienteAdministrativo>("/pendientes-administrativos", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarPendienteAdministrativo() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: PendienteAdministrativoEditarInput }) =>
      (await http.put<PendienteAdministrativo>(`/pendientes-administrativos/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useCambiarEstadoPendienteAdministrativo() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, activo }: { id: number; activo: boolean }) =>
      (await http.patch<PendienteAdministrativo>(`/pendientes-administrativos/${id}/${activo ? "desactivar" : "activar"}`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarPendienteAdministrativo() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/pendientes-administrativos/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

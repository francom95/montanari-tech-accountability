import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type { ComisionProyecto, ComisionProyectoCrearInput, ComisionProyectoEditarInput } from "@/types/comision-proyecto"

const QUERY_KEY = ["comision-proyecto"]

export function useComisionesDeProyecto(proyectoId: number | undefined, params: { activo?: boolean; page?: number; size?: number } = {}) {
  return useQuery({
    queryKey: [...QUERY_KEY, proyectoId, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<ComisionProyecto>>(`/proyectos/${proyectoId}/comisiones`, {
          params: { activo: params.activo, page: params.page ?? 0, size: params.size ?? 50 },
        })
      ).data,
    enabled: proyectoId !== undefined,
  })
}

export function useCrearComisionProyecto(proyectoId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: ComisionProyectoCrearInput) => (await http.post<ComisionProyecto>(`/proyectos/${proyectoId}/comisiones`, v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarComisionProyecto(proyectoId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: ComisionProyectoEditarInput }) =>
      (await http.put<ComisionProyecto>(`/proyectos/${proyectoId}/comisiones/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useCambiarEstadoComisionProyecto(proyectoId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, activo }: { id: number; activo: boolean }) =>
      (await http.patch<ComisionProyecto>(`/proyectos/${proyectoId}/comisiones/${id}/${activo ? "desactivar" : "activar"}`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarComisionProyecto(proyectoId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/proyectos/${proyectoId}/comisiones/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

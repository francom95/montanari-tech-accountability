import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type { Proyecto, ProyectoCrearInput, ProyectoEditarInput } from "@/types/proyecto"

const QUERY_KEY = ["proyecto"]

export function useProyectos(params: { texto?: string; activo?: boolean; clienteId?: number; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<Proyecto>>("/proyectos", {
          params: {
            texto: params.texto || undefined,
            activo: params.activo,
            clienteId: params.clienteId,
            page: params.page ?? 0,
            size: params.size ?? 10,
          },
        })
      ).data,
  })
}

export function useProyecto(id: number | undefined) {
  return useQuery({
    queryKey: [...QUERY_KEY, id],
    queryFn: async () => (await http.get<Proyecto>(`/proyectos/${id}`)).data,
    enabled: id !== undefined,
  })
}

export function useCrearProyecto() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: ProyectoCrearInput) => (await http.post<Proyecto>("/proyectos", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarProyecto() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: ProyectoEditarInput }) =>
      (await http.put<Proyecto>(`/proyectos/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useCambiarEstadoProyecto() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, activo }: { id: number; activo: boolean }) =>
      (await http.patch<Proyecto>(`/proyectos/${id}/${activo ? "desactivar" : "activar"}`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarProyecto() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/proyectos/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type { Etapa, EtapaCrearInput, EtapaEditarInput, EtapaImportFila, EtapaImportResultado } from "@/types/etapa"

const QUERY_KEY = ["etapa"]

export function useEtapas(proyectoId: number | undefined, params: { texto?: string; activo?: boolean; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...QUERY_KEY, proyectoId, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<Etapa>>(`/proyectos/${proyectoId}/etapas`, {
          params: {
            texto: params.texto || undefined,
            activo: params.activo,
            page: params.page ?? 0,
            size: params.size ?? 50,
          },
        })
      ).data,
    enabled: proyectoId !== undefined,
  })
}

export function useCrearEtapa(proyectoId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: EtapaCrearInput) => (await http.post<Etapa>(`/proyectos/${proyectoId}/etapas`, v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarEtapa(proyectoId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: EtapaEditarInput }) =>
      (await http.put<Etapa>(`/proyectos/${proyectoId}/etapas/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useCambiarEstadoEtapa(proyectoId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, activo }: { id: number; activo: boolean }) =>
      (await http.patch<Etapa>(`/proyectos/${proyectoId}/etapas/${id}/${activo ? "desactivar" : "activar"}`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarEtapa(proyectoId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/proyectos/${proyectoId}/etapas/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function usePrevisualizarImportacionEtapas(proyectoId: number) {
  return useMutation({
    mutationFn: async (archivo: File) => {
      const formData = new FormData()
      formData.append("archivo", archivo)
      return (
        await http.post<EtapaImportFila[]>(`/proyectos/${proyectoId}/etapas/importar/previsualizar`, formData, {
          headers: { "Content-Type": "multipart/form-data" },
        })
      ).data
    },
  })
}

export function useConfirmarImportacionEtapas(proyectoId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (filas: EtapaImportFila[]) =>
      (await http.post<EtapaImportResultado>(`/proyectos/${proyectoId}/etapas/importar/confirmar`, filas)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

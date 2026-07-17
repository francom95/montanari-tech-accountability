import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { Adjunto } from "@/types/adjunto"

const QUERY_KEY = ["adjunto"]

export function useAdjuntos(entidadTipo: string, entidadId: number | undefined) {
  return useQuery({
    queryKey: [...QUERY_KEY, entidadTipo, entidadId],
    queryFn: async () => (await http.get<Adjunto[]>("/adjuntos", { params: { entidadTipo, entidadId } })).data,
    enabled: entidadId !== undefined,
  })
}

export function useSubirAdjunto() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ entidadTipo, entidadId, archivo }: { entidadTipo: string; entidadId: number; archivo: File }) => {
      const formData = new FormData()
      formData.append("archivo", archivo)
      return (await http.post<Adjunto>("/adjuntos", formData, {
        params: { entidadTipo, entidadId },
        headers: { "Content-Type": "multipart/form-data" },
      })).data
    },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarAdjunto() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/adjuntos/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

/** Descarga autenticada (Bearer): un <a href> directo perdería el header y daría 401. */
export async function descargarAdjunto(id: number, nombreArchivo: string) {
  const respuesta = await http.get(`/adjuntos/${id}/descargar`, { responseType: "blob" })
  const url = window.URL.createObjectURL(respuesta.data)
  const enlace = document.createElement("a")
  enlace.href = url
  enlace.download = nombreArchivo
  document.body.appendChild(enlace)
  enlace.click()
  enlace.remove()
  window.URL.revokeObjectURL(url)
}

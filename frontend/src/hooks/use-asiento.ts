import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type {
  Asiento,
  AsientoBusquedaFiltros,
  AsientoCrearInput,
  AsientoEditarConfirmadoInput,
  AsientoEditarInput,
} from "@/types/asiento"

const QUERY_KEY = ["asiento"]

export function useAsientos(params: AsientoBusquedaFiltros & { page?: number; size?: number }) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<Asiento>>("/asientos", {
          params: {
            texto: params.texto || undefined,
            estado: params.estado || undefined,
            origen: params.origen || undefined,
            numero: params.numero ?? undefined,
            fechaDesde: params.fechaDesde || undefined,
            fechaHasta: params.fechaHasta || undefined,
            cuentaContableId: params.cuentaContableId ?? undefined,
            importe: params.importe ?? undefined,
            proyectoId: params.proyectoId ?? undefined,
            clienteId: params.clienteId ?? undefined,
            proveedorId: params.proveedorId ?? undefined,
            page: params.page ?? 0,
            size: params.size ?? 10,
            sort: "fecha,desc",
          },
        })
      ).data,
  })
}

export function useAsiento(id: number | undefined) {
  return useQuery({
    queryKey: [...QUERY_KEY, id],
    queryFn: async () => (await http.get<Asiento>(`/asientos/${id}`)).data,
    enabled: id !== undefined,
  })
}

export function useCrearAsiento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: AsientoCrearInput) => (await http.post<Asiento>("/asientos", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarAsiento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: AsientoEditarInput }) =>
      (await http.put<Asiento>(`/asientos/${id}`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEditarAsientoConfirmado() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, valores }: { id: number; valores: AsientoEditarConfirmadoInput }) =>
      (await http.put<Asiento>(`/asientos/${id}/confirmado`, valores)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useConfirmarAsiento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => (await http.patch<Asiento>(`/asientos/${id}/confirmar`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useEliminarAsiento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => { await http.delete(`/asientos/${id}`) },
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useDuplicarAsiento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => (await http.post<Asiento>(`/asientos/${id}/duplicar`)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useAnularAsiento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, motivo }: { id: number; motivo: string }) =>
      (await http.patch<Asiento>(`/asientos/${id}/anular`, { motivo })).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

function descargar(blob: Blob, nombreArchivo: string) {
  const url = window.URL.createObjectURL(blob)
  const enlace = document.createElement("a")
  enlace.href = url
  enlace.download = nombreArchivo
  document.body.appendChild(enlace)
  enlace.click()
  enlace.remove()
  window.URL.revokeObjectURL(url)
}

export async function descargarAsientosExcel(filtros: AsientoBusquedaFiltros) {
  const respuesta = await http.get("/asientos/exportar/excel", { params: filtros, responseType: "blob" })
  descargar(respuesta.data, "libro-diario.xlsx")
}

export async function descargarAsientosPdf(filtros: AsientoBusquedaFiltros) {
  const respuesta = await http.get("/asientos/exportar/pdf", { params: filtros, responseType: "blob" })
  descargar(respuesta.data, "libro-diario.pdf")
}

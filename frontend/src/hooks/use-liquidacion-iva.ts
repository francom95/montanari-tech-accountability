import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type {
  AgregarComponenteInput,
  AjustarComponenteInput,
  LiquidacionIva,
  PrevisualizacionIva,
} from "@/types/liquidacion-iva"

const KEY = ["liquidacionIva"]
const BASE = "/impuestos/liquidaciones-iva"

export function useLiquidacionesIva(params: { anio?: number; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<LiquidacionIva>>(BASE, {
          params: { anio: params.anio, page: params.page ?? 0, size: params.size ?? 20 },
        })
      ).data,
  })
}

export function useLiquidacionIva(id?: number) {
  return useQuery({
    queryKey: [...KEY, id],
    queryFn: async () => (await http.get<LiquidacionIva>(`${BASE}/${id}`)).data,
    enabled: !!id,
  })
}

/** Cómo daría el período si se liquidara ahora, sin persistir nada. */
export function usePrevisualizacionIva(anio?: number, mes?: number) {
  return useQuery({
    queryKey: [...KEY, "previsualizar", anio, mes],
    queryFn: async () =>
      (await http.get<PrevisualizacionIva>(`${BASE}/previsualizar`, { params: { anio, mes } })).data,
    enabled: !!anio && !!mes,
  })
}

function useLiquidacionMutation<TVars>(fn: (vars: TVars) => Promise<LiquidacionIva>) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })
}

export function useCrearLiquidacionIva() {
  return useLiquidacionMutation(async (vars: { anio: number; mes: number }) =>
    (await http.post<LiquidacionIva>(BASE, vars)).data,
  )
}

export function useRecalcularLiquidacionIva() {
  return useLiquidacionMutation(async (id: number) =>
    (await http.post<LiquidacionIva>(`${BASE}/${id}/recalcular`)).data,
  )
}

export function useAjustarComponenteIva() {
  return useLiquidacionMutation(
    async (vars: { id: number; componenteId: number } & AjustarComponenteInput) =>
      (
        await http.patch<LiquidacionIva>(`${BASE}/${vars.id}/componentes/${vars.componenteId}`, {
          importeAjuste: vars.importeAjuste,
          motivoAjuste: vars.motivoAjuste,
        })
      ).data,
  )
}

export function useAgregarComponenteIva() {
  return useLiquidacionMutation(async (vars: { id: number } & AgregarComponenteInput) => {
    const { id, ...body } = vars
    return (await http.post<LiquidacionIva>(`${BASE}/${id}/componentes`, body)).data
  })
}

export function useEliminarComponenteIva() {
  return useLiquidacionMutation(async (vars: { id: number; componenteId: number }) =>
    (await http.delete<LiquidacionIva>(`${BASE}/${vars.id}/componentes/${vars.componenteId}`)).data,
  )
}

export function useConfirmarLiquidacionIva() {
  return useLiquidacionMutation(async (id: number) =>
    (await http.patch<LiquidacionIva>(`${BASE}/${id}/confirmar`)).data,
  )
}

export function useAnularLiquidacionIva() {
  return useLiquidacionMutation(async (vars: { id: number; motivo: string }) =>
    (await http.patch<LiquidacionIva>(`${BASE}/${vars.id}/anular`, { motivo: vars.motivo })).data,
  )
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

export async function descargarLiquidacionesIvaExcel(filtros: { anio?: number }) {
  const respuesta = await http.get(`${BASE}/exportar/excel`, { params: filtros, responseType: "blob" })
  descargar(respuesta.data, "liquidaciones-iva.xlsx")
}

export async function descargarLiquidacionesIvaPdf(filtros: { anio?: number }) {
  const respuesta = await http.get(`${BASE}/exportar/pdf`, { params: filtros, responseType: "blob" })
  descargar(respuesta.data, "liquidaciones-iva.pdf")
}

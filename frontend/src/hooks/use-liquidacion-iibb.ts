import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type {
  AjustarComponenteIibbInput,
  EditarJurisdiccionIibbInput,
  LiquidacionIibb,
  PrevisualizacionIibb,
} from "@/types/liquidacion-iibb"

const KEY = ["liquidacionIibb"]
const BASE = "/impuestos/liquidaciones-iibb"

export function useLiquidacionesIibb(params: { anio?: number; page?: number; size?: number }) {
  return useQuery({
    queryKey: [...KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<LiquidacionIibb>>(BASE, {
          params: { anio: params.anio, page: params.page ?? 0, size: params.size ?? 20 },
        })
      ).data,
  })
}

export function usePrevisualizacionIibb(anio?: number, mes?: number) {
  return useQuery({
    queryKey: [...KEY, "previsualizar", anio, mes],
    queryFn: async () =>
      (await http.get<PrevisualizacionIibb>(`${BASE}/previsualizar`, { params: { anio, mes } })).data,
    enabled: !!anio && !!mes,
  })
}

function useIibbMutation<TVars>(fn: (vars: TVars) => Promise<LiquidacionIibb>) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })
}

export function useCrearLiquidacionIibb() {
  return useIibbMutation(async (vars: { anio: number; mes: number }) =>
    (await http.post<LiquidacionIibb>(BASE, vars)).data,
  )
}

export function useRecalcularLiquidacionIibb() {
  return useIibbMutation(async (id: number) =>
    (await http.post<LiquidacionIibb>(`${BASE}/${id}/recalcular`)).data,
  )
}

export function useEditarJurisdiccionIibb() {
  return useIibbMutation(async (vars: { id: number; jurLiqId: number } & EditarJurisdiccionIibbInput) =>
    (
      await http.patch<LiquidacionIibb>(`${BASE}/${vars.id}/jurisdicciones/${vars.jurLiqId}`, {
        coeficiente: vars.coeficiente,
        alicuota: vars.alicuota,
      })
    ).data,
  )
}

export function useAjustarComponenteIibb() {
  return useIibbMutation(
    async (vars: { id: number; jurLiqId: number; componenteId: number } & AjustarComponenteIibbInput) =>
      (
        await http.patch<LiquidacionIibb>(
          `${BASE}/${vars.id}/jurisdicciones/${vars.jurLiqId}/componentes/${vars.componenteId}`,
          { importeAjuste: vars.importeAjuste, motivoAjuste: vars.motivoAjuste },
        )
      ).data,
  )
}

export function useConfirmarLiquidacionIibb() {
  return useIibbMutation(async (id: number) =>
    (await http.patch<LiquidacionIibb>(`${BASE}/${id}/confirmar`)).data,
  )
}

export function useAnularLiquidacionIibb() {
  return useIibbMutation(async (vars: { id: number; motivo: string }) =>
    (await http.patch<LiquidacionIibb>(`${BASE}/${vars.id}/anular`, { motivo: vars.motivo })).data,
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

export async function descargarLiquidacionesIibbExcel(filtros: { anio?: number }) {
  const respuesta = await http.get(`${BASE}/exportar/excel`, { params: filtros, responseType: "blob" })
  descargar(respuesta.data, "liquidaciones-iibb.xlsx")
}

export async function descargarLiquidacionesIibbPdf(filtros: { anio?: number }) {
  const respuesta = await http.get(`${BASE}/exportar/pdf`, { params: filtros, responseType: "blob" })
  descargar(respuesta.data, "liquidaciones-iibb.pdf")
}

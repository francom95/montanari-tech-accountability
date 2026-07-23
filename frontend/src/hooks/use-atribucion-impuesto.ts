import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type {
  Atribucion,
  CalcularAtribucionInput,
  CriterioAtribucion,
  TipoLiquidacion,
} from "@/types/atribucion-impuesto"

const KEY = ["atribucionImpuesto"]
const BASE = "/impuestos/atribuciones"

export function useAtribucion(tipo: TipoLiquidacion, liquidacionId?: number) {
  return useQuery({
    queryKey: [...KEY, tipo, liquidacionId],
    queryFn: async () => (await http.get<Atribucion>(`${BASE}/${tipo}/${liquidacionId}`)).data,
    enabled: !!liquidacionId,
  })
}

export function useCriterioPorDefecto() {
  return useQuery({
    queryKey: [...KEY, "configuracion"],
    queryFn: async () =>
      (await http.get<{ criterioPorDefecto: CriterioAtribucion }>(`${BASE}/configuracion`)).data,
  })
}

/** Calcula una distribución sin persistir (para ver antes de guardar). */
export function usePrevisualizarAtribucion(tipo: TipoLiquidacion, liquidacionId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (input: CalcularAtribucionInput) =>
      (await http.post<Atribucion>(`${BASE}/${tipo}/${liquidacionId}/previsualizar`, input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })
}

export function useGuardarAtribucion(tipo: TipoLiquidacion, liquidacionId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (input: CalcularAtribucionInput) =>
      (await http.post<Atribucion>(`${BASE}/${tipo}/${liquidacionId}`, input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })
}

export function useActualizarCriterioPorDefecto() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (criterioPorDefecto: CriterioAtribucion) =>
      (await http.put<{ criterioPorDefecto: CriterioAtribucion }>(`${BASE}/configuracion`, { criterioPorDefecto }))
        .data,
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })
}

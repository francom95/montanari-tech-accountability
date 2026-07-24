import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { http } from "@/lib/http"

export type ConfiguracionCobranza = { diasGraciaMora: number; tasaMoraDiariaPorcentaje: number }
export type ConfiguracionTipoCambio = { criterioPorDefecto: string | null }

const COBRANZA_KEY = ["configuracion-cobranza"]
const TIPO_CAMBIO_KEY = ["configuracion-tipo-cambio"]

export function useConfiguracionCobranza() {
  return useQuery({
    queryKey: COBRANZA_KEY,
    queryFn: async () => (await http.get<ConfiguracionCobranza>("/cobros/configuracion-cobranza")).data,
  })
}

export function useActualizarConfiguracionCobranza() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: ConfiguracionCobranza) => (await http.put<ConfiguracionCobranza>("/cobros/configuracion-cobranza", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: COBRANZA_KEY }) },
  })
}

export function useConfiguracionTipoCambio() {
  return useQuery({
    queryKey: TIPO_CAMBIO_KEY,
    queryFn: async () => (await http.get<ConfiguracionTipoCambio>("/tipo-cambios/configuracion")).data,
  })
}

export function useActualizarConfiguracionTipoCambio() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: ConfiguracionTipoCambio) => (await http.put<ConfiguracionTipoCambio>("/tipo-cambios/configuracion", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: TIPO_CAMBIO_KEY }) },
  })
}

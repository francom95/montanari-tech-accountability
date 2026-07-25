import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type { ConfiguracionAlertas } from "@/types/configuracion-alertas"

const CONFIGURACION_KEY = ["configuracion-alertas"]

export function useConfiguracionAlertas() {
  return useQuery({
    queryKey: CONFIGURACION_KEY,
    queryFn: async () => (await http.get<ConfiguracionAlertas>("/configuracion-alertas")).data,
  })
}

export function useActualizarConfiguracionAlertas() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: ConfiguracionAlertas) => (await http.put<ConfiguracionAlertas>("/configuracion-alertas", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: CONFIGURACION_KEY }) },
  })
}

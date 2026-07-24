import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type { ConfiguracionDashboard, DashboardResponse } from "@/types/dashboard"

const QUERY_KEY = ["dashboard"]
const CONFIGURACION_KEY = ["dashboard-configuracion"]

export function useDashboard(anio: number, mes: number) {
  return useQuery({
    queryKey: [...QUERY_KEY, anio, mes],
    queryFn: async () => (await http.get<DashboardResponse>("/dashboard", { params: { anio, mes } })).data,
  })
}

export function useConfiguracionDashboard() {
  return useQuery({
    queryKey: CONFIGURACION_KEY,
    queryFn: async () => (await http.get<ConfiguracionDashboard>("/dashboard/configuracion")).data,
  })
}

export function useActualizarConfiguracionDashboard() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: ConfiguracionDashboard) => (await http.put<ConfiguracionDashboard>("/dashboard/configuracion", v)).data,
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: CONFIGURACION_KEY })
      await qc.invalidateQueries({ queryKey: QUERY_KEY })
    },
  })
}

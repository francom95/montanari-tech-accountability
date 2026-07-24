import axios from "axios"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type {
  ConfiguracionPresupuesto,
  PresupuestoProyecto,
  PresupuestoProyectoGuardarInput,
} from "@/types/presupuesto-proyecto"

const QUERY_KEY = ["presupuesto-proyecto"]
const CONFIG_QUERY_KEY = ["configuracion-presupuesto"]

export function usePresupuestoProyecto(proyectoId: number | undefined) {
  return useQuery({
    queryKey: [...QUERY_KEY, proyectoId],
    queryFn: async () => {
      try {
        return (await http.get<PresupuestoProyecto>(`/proyectos/${proyectoId}/presupuesto`)).data
      } catch (e) {
        if (axios.isAxiosError(e) && e.response?.status === 404) {
          return null
        }
        throw e
      }
    },
    enabled: proyectoId !== undefined,
  })
}

export function useGuardarPresupuestoProyecto(proyectoId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: PresupuestoProyectoGuardarInput) =>
      (await http.put<PresupuestoProyecto>(`/proyectos/${proyectoId}/presupuesto`, v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: QUERY_KEY }) },
  })
}

export function useConfiguracionPresupuesto() {
  return useQuery({
    queryKey: CONFIG_QUERY_KEY,
    queryFn: async () => (await http.get<ConfiguracionPresupuesto>("/presupuestos/configuracion")).data,
  })
}

export function useActualizarConfiguracionPresupuesto() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: ConfiguracionPresupuesto) =>
      (await http.put<ConfiguracionPresupuesto>("/presupuestos/configuracion", v)).data,
    onSuccess: async () => { await qc.invalidateQueries({ queryKey: CONFIG_QUERY_KEY }) },
  })
}

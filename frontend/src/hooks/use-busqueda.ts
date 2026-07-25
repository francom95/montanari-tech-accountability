import { useQuery } from "@tanstack/react-query"
import { http } from "@/lib/http"
import type { BusquedaGlobalResponse } from "@/types/busqueda"

const QUERY_KEY = ["busqueda-global"]

export function useBusquedaGlobal(termino: string) {
  const habilitada = termino.trim().length >= 2
  return useQuery({
    queryKey: [...QUERY_KEY, termino],
    queryFn: async () => (await http.get<BusquedaGlobalResponse>("/busqueda", { params: { q: termino } })).data,
    enabled: habilitada,
  })
}

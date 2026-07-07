import { useQuery } from "@tanstack/react-query"

import { http } from "@/lib/http"
import type { PageResponse, Usuario } from "@/types/auth"

const QUERY_KEY = ["usuarios"]

export function useUsuarios(params: { page?: number; size?: number } = {}) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: async () =>
      (
        await http.get<PageResponse<Usuario>>("/usuarios", {
          params: { page: params.page ?? 0, size: params.size ?? 100 },
        })
      ).data,
  })
}

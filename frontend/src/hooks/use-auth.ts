import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useNavigate } from "react-router-dom"

import { authToken } from "@/lib/auth-token"
import { http } from "@/lib/http"
import type { TokenPair, UsuarioActual } from "@/types/auth"

export const AUTH_ME_QUERY_KEY = ["auth", "me"]

export function useCurrentUser() {
  return useQuery({
    queryKey: AUTH_ME_QUERY_KEY,
    queryFn: async () => (await http.get<UsuarioActual>("/auth/me")).data,
    enabled: !!authToken.getAccessToken(),
    retry: false,
  })
}

export function useLogin() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: async (credenciales: { email: string; password: string }) =>
      (await http.post<TokenPair>("/auth/login", credenciales)).data,
    onSuccess: async (tokens) => {
      authToken.setTokens(tokens.accessToken, tokens.refreshToken)
      await queryClient.invalidateQueries({ queryKey: AUTH_ME_QUERY_KEY })
      navigate("/", { replace: true })
    },
  })
}

export function useLogout() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: async () => {
      const refreshToken = authToken.getRefreshToken()
      if (refreshToken) {
        await http.post("/auth/logout", { refreshToken })
      }
    },
    onSettled: () => {
      authToken.clear()
      queryClient.clear()
      navigate("/login", { replace: true })
    },
  })
}

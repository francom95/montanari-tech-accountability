import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios"
import { authToken } from "@/lib/auth-token"

/**
 * Cliente HTTP tipado único para todo el frontend. Inyecta el access token,
 * y ante un 401 intenta refrescar una sola vez (single-flight: si ya hay un
 * refresh en curso, los requests que llegan mientras tanto lo esperan en vez
 * de disparar refreshes en paralelo) antes de reintentar el request
 * original. Si el refresh falla, limpia los tokens y manda a /login.
 *
 * El endpoint /api/v1/auth/refresh todavía no existe (lo crea F1.5); esta
 * pieza ya queda lista para que F1.5 solo tenga que darle un backend real.
 */
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api/v1",
})

http.interceptors.request.use((config) => {
  const accessToken = authToken.getAccessToken()
  if (accessToken) {
    config.headers.set("Authorization", `Bearer ${accessToken}`)
  }
  return config
})

let refreshPromise: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = authToken.getRefreshToken()
  if (!refreshToken) {
    return null
  }

  try {
    const { data } = await axios.post<{ accessToken: string; refreshToken: string }>(
      `${import.meta.env.VITE_API_BASE_URL ?? "/api/v1"}/auth/refresh`,
      { refreshToken }
    )
    authToken.setTokens(data.accessToken, data.refreshToken)
    return data.accessToken
  } catch {
    return null
  }
}

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableConfig | undefined

    if (error.response?.status !== 401 || !original || original._retried) {
      if (error.response?.status === 401) {
        authToken.clear()
        window.location.assign("/login")
      }
      return Promise.reject(error)
    }

    original._retried = true
    refreshPromise ??= refreshAccessToken().finally(() => {
      refreshPromise = null
    })

    const newAccessToken = await refreshPromise
    if (!newAccessToken) {
      authToken.clear()
      window.location.assign("/login")
      return Promise.reject(error)
    }

    original.headers.set("Authorization", `Bearer ${newAccessToken}`)
    return http(original)
  }
)

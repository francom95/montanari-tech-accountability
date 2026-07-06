/**
 * Almacenamiento del par access/refresh token. F1.5 define el mecanismo de
 * autenticación real (login, refresh endpoint, expiración); esto es solo el
 * lugar único donde vive el token para que el interceptor de {@link http}
 * no necesite saber de dónde viene.
 */
const ACCESS_TOKEN_KEY = "montanari.accessToken"
const REFRESH_TOKEN_KEY = "montanari.refreshToken"

export const authToken = {
  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY)
  },
  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY)
  },
  setTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  },
  clear(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  },
}

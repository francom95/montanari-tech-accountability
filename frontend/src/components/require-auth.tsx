import type { ReactNode } from "react"
import { Navigate, useLocation } from "react-router-dom"

import { authToken } from "@/lib/auth-token"

/**
 * Gate de UX en el cliente: si no hay access token, ni intenta pintar la
 * pantalla. La autorización real la hace el backend en cada request (401
 * si no hay token válido, 403 si el rol no alcanza) — esto solo evita el
 * parpadeo de mostrar una pantalla protegida antes de que el backend responda.
 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const location = useLocation()

  if (!authToken.getAccessToken()) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return children
}

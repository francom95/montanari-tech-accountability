import type { ReactNode } from "react"

import { useCurrentUser } from "@/hooks/use-auth"
import { PlaceholderPage } from "@/pages/placeholder-page"

/** Defensa en profundidad en el cliente: el backend ya rechaza con 403 (F1.5); esto evita el parpadeo de la pantalla. */
export function RequireAdmin({ children }: { children: ReactNode }) {
  const { data: usuarioActual, isLoading } = useCurrentUser()

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">Cargando…</p>
  }

  if (usuarioActual?.rol !== "ADMINISTRADOR") {
    return <PlaceholderPage title="Acceso restringido" />
  }

  return children
}

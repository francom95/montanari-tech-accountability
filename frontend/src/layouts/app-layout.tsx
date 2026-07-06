import { NavLink, Outlet } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { useCurrentUser, useLogout } from "@/hooks/use-auth"
import { cn } from "@/lib/utils"
import { NAV_ITEMS } from "@/routes/nav-config"

export function AppLayout() {
  const { data: usuarioActual } = useCurrentUser()
  const logout = useLogout()

  return (
    <div className="flex min-h-svh w-full">
      <aside className="hidden w-64 shrink-0 border-r border-border bg-card md:flex md:flex-col">
        <div className="flex h-14 items-center gap-2 border-b border-border px-4">
          {/* Placeholder de logo — el equipo lo envía luego (F1.1 §"Pendiente del equipo"); reemplazar este div por <img>. */}
          <div
            aria-hidden
            className="size-6 shrink-0 rounded-md bg-primary text-primary-foreground"
          />
          <span className="text-sm font-semibold">Montanari Tech</span>
        </div>
        <nav className="flex-1 space-y-0.5 overflow-y-auto p-2">
          {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === "/"}
              className={({ isActive }) =>
                cn(
                  "flex items-center gap-2 rounded-md px-2.5 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground",
                  isActive && "bg-secondary text-secondary-foreground"
                )
              }
            >
              <Icon className="size-4 shrink-0" />
              <span className="truncate">{label}</span>
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 shrink-0 items-center justify-between border-b border-border px-4">
          <span className="text-sm font-medium text-muted-foreground">
            Sistema de Gestión Contable
          </span>
          <div className="flex items-center gap-3">
            {usuarioActual && (
              <span className="text-sm text-muted-foreground">
                {usuarioActual.nombre} · {usuarioActual.rol}
              </span>
            )}
            <Button variant="outline" size="sm" onClick={() => logout.mutate()}>
              Salir
            </Button>
          </div>
        </header>
        <main className="min-w-0 flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

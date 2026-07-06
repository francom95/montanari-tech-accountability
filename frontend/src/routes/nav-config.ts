import type { LucideIcon } from "lucide-react"
import {
  LayoutDashboard,
  Users,
  BookOpenText,
  Receipt,
  Landmark,
  BarChart3,
  Percent,
  CalendarClock,
  ListTodo,
  ShieldCheck,
  ScrollText,
  FormInput,
  Coins,
} from "lucide-react"

export type NavItem = {
  to: string
  label: string
  icon: LucideIcon
}

/**
 * Un ítem por módulo principal del sistema (funcional §2). Cada uno hoy
 * renderiza un placeholder; los CRUDs reales de F2 en adelante reemplazan
 * el contenido de su página sin tocar esta navegación.
 */
export const NAV_ITEMS: NavItem[] = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard },
  { to: "/maestros", label: "Maestros", icon: Users },
  { to: "/contabilidad", label: "Contabilidad", icon: BookOpenText },
  { to: "/facturacion", label: "Facturación, cobros y pagos", icon: Receipt },
  { to: "/bancos", label: "Bancos, tarjetas y conciliaciones", icon: Landmark },
  { to: "/reportes", label: "Reportes", icon: BarChart3 },
  { to: "/impuestos", label: "Impuestos", icon: Percent },
  { to: "/presupuesto", label: "Presupuesto y vencimientos", icon: CalendarClock },
  { to: "/pendientes", label: "Pendientes administrativos", icon: ListTodo },
  { to: "/seguridad", label: "Usuarios", icon: ShieldCheck },
  { to: "/auditoria", label: "Auditoría", icon: ScrollText },
  { to: "/ejemplo-formulario", label: "Ejemplo de formulario (F1.4)", icon: FormInput },
  { to: "/monedas", label: "Monedas (molde F1.8)", icon: Coins },
]

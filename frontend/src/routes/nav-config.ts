import type { LucideIcon } from "lucide-react"
import {
  LayoutDashboard,
  BookOpenText,
  BookText,
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
  TrendingUp,
  MapPin,
  Tags,
  ListTree,
  Repeat,
  Wrench,
  Users,
  Truck,
  Wallet,
  CreditCard,
  FolderKanban,
  HandCoins,
  FileSpreadsheet,
  Link2,
  ArrowDownToLine,
  ArrowUpFromLine,
  ClipboardList,
  FileUp,
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
  { to: "/monedas", label: "Monedas", icon: Coins },
  { to: "/tipos-cambio", label: "Tipos de cambio", icon: TrendingUp },
  { to: "/jurisdicciones", label: "Jurisdicciones", icon: MapPin },
  { to: "/clientes", label: "Clientes", icon: Users },
  { to: "/proveedores", label: "Proveedores", icon: Truck },
  { to: "/proyectos", label: "Proyectos", icon: FolderKanban },
  { to: "/comisionistas", label: "Comisionistas", icon: HandCoins },
  { to: "/categorias", label: "Categorías contables", icon: Tags },
  { to: "/rubros", label: "Rubros", icon: ListTree },
  { to: "/conceptos", label: "Conceptos recurrentes", icon: Repeat },
  { to: "/tipos-costo", label: "Tipos de costo", icon: Wrench },
  { to: "/cuentas-bancarias", label: "Cuentas bancarias", icon: Wallet },
  { to: "/tarjetas-credito", label: "Tarjetas de crédito", icon: CreditCard },
  { to: "/contabilidad", label: "Contabilidad", icon: BookOpenText },
  { to: "/contabilidad/asientos", label: "Asientos contables", icon: BookText },
  { to: "/facturacion", label: "Facturación, cobros y pagos", icon: Receipt },
  { to: "/facturacion/ventas", label: "Facturas de venta", icon: FileSpreadsheet },
  { to: "/facturacion/compras", label: "Facturas de compra", icon: FileSpreadsheet },
  { to: "/facturacion/cobros", label: "Cobros", icon: ArrowDownToLine },
  { to: "/facturacion/pagos", label: "Pagos", icon: ArrowUpFromLine },
  { to: "/facturacion/cuentas-por-cobrar", label: "Cuentas por cobrar", icon: ClipboardList },
  { to: "/facturacion/cuentas-por-pagar", label: "Cuentas por pagar", icon: ClipboardList },
  { to: "/facturacion/mapeo-cuentas", label: "Mapeo de cuentas", icon: Link2 },
  { to: "/facturacion/importacion-historica", label: "Importación histórica", icon: FileUp },
  { to: "/bancos", label: "Bancos, tarjetas y conciliaciones", icon: Landmark },
  { to: "/bancos/movimientos", label: "Movimientos bancarios", icon: Wallet },
  { to: "/reportes", label: "Reportes", icon: BarChart3 },
  { to: "/impuestos", label: "Impuestos", icon: Percent },
  { to: "/presupuesto", label: "Presupuesto y vencimientos", icon: CalendarClock },
  { to: "/pendientes", label: "Pendientes administrativos", icon: ListTodo },
  { to: "/seguridad", label: "Usuarios", icon: ShieldCheck },
  { to: "/auditoria", label: "Auditoría", icon: ScrollText },
  { to: "/ejemplo-formulario", label: "Ejemplo de formulario (F1.4)", icon: FormInput },
]

import { createBrowserRouter } from "react-router-dom"

import { RequireAuth } from "@/components/require-auth"
import { AppLayout } from "@/layouts/app-layout"
import { AuditoriaPage } from "@/pages/auditoria-page"
import { DashboardPage } from "@/pages/dashboard-page"
import { EjemploFormularioPage } from "@/pages/ejemplo-formulario-page"
import { LoginPage } from "@/pages/login-page"
import { PlaceholderPage } from "@/pages/placeholder-page"
import { UsuariosPage } from "@/pages/usuarios-page"

export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  {
    path: "/",
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <DashboardPage /> },
      { path: "maestros", element: <PlaceholderPage title="Maestros" /> },
      { path: "contabilidad", element: <PlaceholderPage title="Contabilidad" /> },
      {
        path: "facturacion",
        element: <PlaceholderPage title="Facturación, cobros y pagos" />,
      },
      {
        path: "bancos",
        element: <PlaceholderPage title="Bancos, tarjetas y conciliaciones" />,
      },
      { path: "reportes", element: <PlaceholderPage title="Reportes" /> },
      { path: "impuestos", element: <PlaceholderPage title="Impuestos" /> },
      {
        path: "presupuesto",
        element: <PlaceholderPage title="Presupuesto y vencimientos" />,
      },
      {
        path: "pendientes",
        element: <PlaceholderPage title="Pendientes administrativos" />,
      },
      { path: "seguridad", element: <UsuariosPage /> },
      { path: "auditoria", element: <AuditoriaPage /> },
      { path: "ejemplo-formulario", element: <EjemploFormularioPage /> },
    ],
  },
])

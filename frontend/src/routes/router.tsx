import { createBrowserRouter } from "react-router-dom"

import { RequireAuth } from "@/components/require-auth"
import { AppLayout } from "@/layouts/app-layout"
import { AuditoriaPage } from "@/pages/auditoria-page"
import { CategoriasPage } from "@/pages/categorias-page"
import { ConceptosPage } from "@/pages/conceptos-page"
import { DashboardPage } from "@/pages/dashboard-page"
import { EjemploFormularioPage } from "@/pages/ejemplo-formulario-page"
import { JurisdiccionesPage } from "@/pages/jurisdicciones-page"
import { LoginPage } from "@/pages/login-page"
import { MonedasPage } from "@/pages/monedas-page"
import { PlaceholderPage } from "@/pages/placeholder-page"
import { RubrosPage } from "@/pages/rubros-page"
import { TiposCambioPage } from "@/pages/tipos-cambio-page"
import { TiposCostoPage } from "@/pages/tipos-costo-page"
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
      { path: "monedas", element: <MonedasPage /> },
      { path: "tipos-cambio", element: <TiposCambioPage /> },
      { path: "jurisdicciones", element: <JurisdiccionesPage /> },
      { path: "categorias", element: <CategoriasPage /> },
      { path: "rubros", element: <RubrosPage /> },
      { path: "conceptos", element: <ConceptosPage /> },
      { path: "tipos-costo", element: <TiposCostoPage /> },
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

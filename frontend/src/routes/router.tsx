import { createBrowserRouter } from "react-router-dom"

import { RequireAuth } from "@/components/require-auth"
import { AppLayout } from "@/layouts/app-layout"
import { AuditoriaPage } from "@/pages/auditoria-page"
import { CategoriasPage } from "@/pages/categorias-page"
import { ClientesPage } from "@/pages/clientes-page"
import { ConceptosPage } from "@/pages/conceptos-page"
import { CuentasBancariasPage } from "@/pages/cuentas-bancarias-page"
import { DashboardPage } from "@/pages/dashboard-page"
import { EjemploFormularioPage } from "@/pages/ejemplo-formulario-page"
import { JurisdiccionesPage } from "@/pages/jurisdicciones-page"
import { LoginPage } from "@/pages/login-page"
import { MonedasPage } from "@/pages/monedas-page"
import { PlaceholderPage } from "@/pages/placeholder-page"
import { ProveedoresPage } from "@/pages/proveedores-page"
import { RubrosPage } from "@/pages/rubros-page"
import { TarjetasCreditoPage } from "@/pages/tarjetas-credito-page"
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
      { path: "clientes", element: <ClientesPage /> },
      { path: "proveedores", element: <ProveedoresPage /> },
      { path: "categorias", element: <CategoriasPage /> },
      { path: "rubros", element: <RubrosPage /> },
      { path: "conceptos", element: <ConceptosPage /> },
      { path: "tipos-costo", element: <TiposCostoPage /> },
      { path: "cuentas-bancarias", element: <CuentasBancariasPage /> },
      { path: "tarjetas-credito", element: <TarjetasCreditoPage /> },
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

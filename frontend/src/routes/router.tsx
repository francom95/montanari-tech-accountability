import { createBrowserRouter } from "react-router-dom"

import { RequireAuth } from "@/components/require-auth"
import { AppLayout } from "@/layouts/app-layout"
import { AsientosPage } from "@/pages/asientos-page"
import { AuditoriaPage } from "@/pages/auditoria-page"
import { BalanceSumasYSaldosPage } from "@/pages/balance-sumas-y-saldos-page"
import { CategoriasPage } from "@/pages/categorias-page"
import { ClientesPage } from "@/pages/clientes-page"
import { CobrosPage } from "@/pages/cobros-page"
import { ComisionistasPage } from "@/pages/comisionistas-page"
import { ConceptosPage } from "@/pages/conceptos-page"
import { ConciliacionPage } from "@/pages/conciliacion-page"
import { CuentasBancariasPage } from "@/pages/cuentas-bancarias-page"
import { CuentasPorCobrarPage } from "@/pages/cuentas-por-cobrar-page"
import { CuentasPorPagarPage } from "@/pages/cuentas-por-pagar-page"
import { DashboardPage } from "@/pages/dashboard-page"
import { EjemploFormularioPage } from "@/pages/ejemplo-formulario-page"
import { EstadoResultadosPage } from "@/pages/estado-resultados-page"
import { FacturasCompraPage } from "@/pages/facturas-compra-page"
import { FacturasVentaPage } from "@/pages/facturas-venta-page"
import { ImportacionHistoricaPage } from "@/pages/importacion-historica-page"
import { JurisdiccionesPage } from "@/pages/jurisdicciones-page"
import { LiquidacionIibbPage } from "@/pages/liquidacion-iibb-page"
import { LiquidacionIvaPage } from "@/pages/liquidacion-iva-page"
import { LoginPage } from "@/pages/login-page"
import { MapeoCuentaPage } from "@/pages/mapeo-cuenta-page"
import { MapeoRubroLineaErPage } from "@/pages/mapeo-rubro-linea-er-page"
import { MayorPage } from "@/pages/mayor-page"
import { MonedasPage } from "@/pages/monedas-page"
import { ImportacionBancariaPage } from "@/pages/importacion-bancaria-page"
import { MovimientosBancariosPage } from "@/pages/movimientos-bancarios-page"
import { PagosPage } from "@/pages/pagos-page"
import { PlaceholderPage } from "@/pages/placeholder-page"
import { PlanDeCuentasPage } from "@/pages/plan-de-cuentas-page"
import { ProveedoresPage } from "@/pages/proveedores-page"
import { ProyectoDetallePage } from "@/pages/proyecto-detalle-page"
import { ProyectosPage } from "@/pages/proyectos-page"
import { ReglasClasificacionConsumoPage } from "@/pages/reglas-clasificacion-consumo-page"
import { RubrosPage } from "@/pages/rubros-page"
import { TarjetaCreditoDetallePage } from "@/pages/tarjeta-credito-detalle-page"
import { TarjetasCreditoPage } from "@/pages/tarjetas-credito-page"
import { TiposCambioPage } from "@/pages/tipos-cambio-page"
import { TiposCostoPage } from "@/pages/tipos-costo-page"
import { UsuariosPage } from "@/pages/usuarios-page"
import { VencimientosPage } from "@/pages/vencimientos-page"

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
      { path: "proyectos", element: <ProyectosPage /> },
      { path: "proyectos/:id", element: <ProyectoDetallePage /> },
      { path: "comisionistas", element: <ComisionistasPage /> },
      { path: "categorias", element: <CategoriasPage /> },
      { path: "rubros", element: <RubrosPage /> },
      { path: "conceptos", element: <ConceptosPage /> },
      { path: "tipos-costo", element: <TiposCostoPage /> },
      { path: "cuentas-bancarias", element: <CuentasBancariasPage /> },
      { path: "tarjetas-credito", element: <TarjetasCreditoPage /> },
      { path: "tarjetas-credito/:id", element: <TarjetaCreditoDetallePage /> },
      { path: "bancos/reglas-clasificacion-consumo", element: <ReglasClasificacionConsumoPage /> },
      { path: "contabilidad", element: <PlanDeCuentasPage /> },
      { path: "contabilidad/asientos", element: <AsientosPage /> },
      { path: "contabilidad/mayor/:cuentaId", element: <MayorPage /> },
      {
        path: "facturacion",
        element: <PlaceholderPage title="Facturación, cobros y pagos" />,
      },
      { path: "facturacion/ventas", element: <FacturasVentaPage /> },
      { path: "facturacion/compras", element: <FacturasCompraPage /> },
      { path: "facturacion/cobros", element: <CobrosPage /> },
      { path: "facturacion/pagos", element: <PagosPage /> },
      { path: "facturacion/cuentas-por-cobrar", element: <CuentasPorCobrarPage /> },
      { path: "facturacion/cuentas-por-pagar", element: <CuentasPorPagarPage /> },
      { path: "facturacion/mapeo-cuentas", element: <MapeoCuentaPage /> },
      { path: "facturacion/importacion-historica", element: <ImportacionHistoricaPage /> },
      {
        path: "bancos",
        element: <PlaceholderPage title="Bancos, tarjetas y conciliaciones" />,
      },
      { path: "bancos/movimientos", element: <MovimientosBancariosPage /> },
      { path: "bancos/importacion", element: <ImportacionBancariaPage /> },
      { path: "bancos/conciliacion", element: <ConciliacionPage /> },
      { path: "reportes", element: <PlaceholderPage title="Reportes" /> },
      { path: "reportes/balance-sumas-y-saldos", element: <BalanceSumasYSaldosPage /> },
      { path: "reportes/estado-resultados", element: <EstadoResultadosPage /> },
      { path: "reportes/mapeo-rubro-linea-er", element: <MapeoRubroLineaErPage /> },
      { path: "impuestos", element: <PlaceholderPage title="Impuestos" /> },
      { path: "impuestos/iva", element: <LiquidacionIvaPage /> },
      { path: "impuestos/iibb", element: <LiquidacionIibbPage /> },
      {
        path: "presupuesto",
        element: <PlaceholderPage title="Presupuesto y vencimientos" />,
      },
      { path: "presupuesto/vencimientos", element: <VencimientosPage /> },
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

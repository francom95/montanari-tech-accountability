import { useState } from "react"
import { Link } from "react-router-dom"

import { ConfiguracionDashboardCard } from "@/components/configuracion-dashboard-card"
import { RequireAdmin } from "@/components/require-admin"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { useDashboard } from "@/hooks/use-dashboard"
import type { IndicadorMonto } from "@/types/dashboard"

const MESES = [
  "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
  "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
]

const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"

const pesos = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" })

function IndicadorCard({ titulo, indicador }: { titulo: string; indicador: IndicadorMonto }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm text-muted-foreground">{titulo}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-2xl font-semibold text-foreground">{pesos.format(indicador.valorArs)}</p>
        <Link to={indicador.ruta} className="text-sm hover:underline">
          Ver detalle →
        </Link>
      </CardContent>
    </Card>
  )
}

export function DashboardPage() {
  const hoy = new Date()
  const [anio, setAnio] = useState(hoy.getFullYear())
  const [mes, setMes] = useState(hoy.getMonth() + 1)

  const dashboard = useDashboard(anio, mes)

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-lg font-semibold text-foreground">Dashboard</h1>
        <p className="text-sm text-muted-foreground">
          Los indicadores del período seleccionado, con acceso directo al reporte de origen de cada uno.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Período</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap items-end gap-4">
          <label className="flex flex-col text-xs text-muted-foreground">
            Año
            <Input type="number" value={anio} onChange={(e) => setAnio(Number(e.target.value))} className="h-8 w-28" />
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Mes
            <select className={selectClase} value={mes} onChange={(e) => setMes(Number(e.target.value))}>
              {MESES.map((nombre, i) => (
                <option key={nombre} value={i + 1}>{nombre}</option>
              ))}
            </select>
          </label>
        </CardContent>
      </Card>

      {dashboard.isLoading && <p className="text-sm text-muted-foreground">Cargando…</p>}

      {dashboard.data && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <IndicadorCard titulo="Resultado mensual" indicador={dashboard.data.resultadoMensual} />
            <IndicadorCard titulo="Ventas del período" indicador={dashboard.data.ventasDelPeriodo} />
            <IndicadorCard titulo="Cobros realizados" indicador={dashboard.data.cobrosDelPeriodo} />
            <IndicadorCard titulo="Cuentas por cobrar" indicador={dashboard.data.cuentasPorCobrar} />
            <IndicadorCard titulo="Cuentas por pagar" indicador={dashboard.data.cuentasPorPagar} />
            <IndicadorCard titulo="Obligaciones próximas" indicador={dashboard.data.obligacionesProximas} />
            <IndicadorCard titulo="Saldo de caja" indicador={dashboard.data.saldoCaja} />
            <IndicadorCard titulo="Saldo por banco/cuenta" indicador={dashboard.data.saldoBanco} />
            <IndicadorCard titulo="Margen estimado" indicador={dashboard.data.margenEstimado} />
            <IndicadorCard titulo="Egresos proyectados" indicador={dashboard.data.egresosProyectados} />
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Impuestos próximos a vencer</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-2">
              <div>
                <p className="text-sm text-muted-foreground">IVA — vence {dashboard.data.proximoVencimientoIva.fechaVencimiento}</p>
                <p className="text-xl font-semibold text-foreground">
                  {pesos.format(dashboard.data.proximoVencimientoIva.saldoAPagarArs)}
                </p>
                <Link to={dashboard.data.proximoVencimientoIva.ruta} className="text-sm hover:underline">Ver detalle →</Link>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">IIBB — vence {dashboard.data.proximoVencimientoIibb.fechaVencimiento}</p>
                <p className="text-xl font-semibold text-foreground">
                  {pesos.format(dashboard.data.proximoVencimientoIibb.saldoAPagarArs)}
                </p>
                <Link to={dashboard.data.proximoVencimientoIibb.ruta} className="text-sm hover:underline">Ver detalle →</Link>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Alertas</CardTitle>
            </CardHeader>
            <CardContent className="text-sm text-muted-foreground">
              {dashboard.data.alertas.length === 0 ? (
                <p>Sin alertas configuradas todavía — se conecta con F9.1.</p>
              ) : (
                <ul className="list-inside list-disc">
                  {dashboard.data.alertas.map((alerta) => (
                    <li key={alerta}>{alerta}</li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>
        </>
      )}

      <RequireAdmin>
        <ConfiguracionDashboardCard />
      </RequireAdmin>
    </div>
  )
}

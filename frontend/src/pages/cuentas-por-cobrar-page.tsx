import { useMemo, useState } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { useClientes } from "@/hooks/use-cliente"
import { descargarCuentasPorCobrarExcel, descargarCuentasPorCobrarPdf, useCuentasPorCobrar } from "@/hooks/use-cuenta-por-cobrar"
import { useMonedas } from "@/hooks/use-monedas"
import { useProyectos } from "@/hooks/use-proyecto"
import type { CuentaPorCobrarFiltros, EstadoVencimiento } from "@/types/cuenta-por-cobrar"

const ESTADOS_VENCIMIENTO: EstadoVencimiento[] = ["VENCIDO", "POR_VENCER", "SIN_VENCIMIENTO"]
const ESTADO_LABEL: Record<EstadoVencimiento, string> = {
  VENCIDO: "Vencido",
  POR_VENCER: "Por vencer",
  SIN_VENCIMIENTO: "Sin vencimiento",
}
const ESTADO_CLASE: Record<EstadoVencimiento, string> = {
  VENCIDO: "bg-destructive/10 text-destructive",
  POR_VENCER: "bg-amber-500/10 text-amber-600",
  SIN_VENCIMIENTO: "bg-muted text-muted-foreground",
}

const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"
const inputClase = "h-8"

export function CuentasPorCobrarPage() {
  const [clienteId, setClienteId] = useState("")
  const [proyectoId, setProyectoId] = useState("")
  const [monedaId, setMonedaId] = useState("")
  const [fechaDesde, setFechaDesde] = useState("")
  const [fechaHasta, setFechaHasta] = useState("")
  const [estadoVencimiento, setEstadoVencimiento] = useState("")
  const [descargando, setDescargando] = useState<"excel" | "pdf" | null>(null)

  const filtros: CuentaPorCobrarFiltros = useMemo(() => ({
    clienteId: clienteId ? Number(clienteId) : undefined,
    proyectoId: proyectoId ? Number(proyectoId) : undefined,
    monedaId: monedaId ? Number(monedaId) : undefined,
    fechaDesde: fechaDesde || undefined,
    fechaHasta: fechaHasta || undefined,
    estadoVencimiento: (estadoVencimiento || undefined) as EstadoVencimiento | undefined,
  }), [clienteId, proyectoId, monedaId, fechaDesde, fechaHasta, estadoVencimiento])

  const reporte = useCuentasPorCobrar(filtros)
  const clientes = useClientes({ page: 0, size: 200 })
  const proyectos = useProyectos({ page: 0, size: 200 })
  const monedas = useMonedas({ page: 0, size: 20 })

  async function exportar(formato: "excel" | "pdf") {
    setDescargando(formato)
    try {
      if (formato === "excel") await descargarCuentasPorCobrarExcel(filtros)
      else await descargarCuentasPorCobrarPdf(filtros)
    } finally {
      setDescargando(null)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-semibold text-foreground">Cuentas por cobrar</h1>
          <p className="text-sm text-muted-foreground">Facturas de venta confirmadas con saldo pendiente (F4.4: no recalcula saldos).</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" disabled={descargando !== null || !reporte.data} onClick={() => exportar("excel")}>
            {descargando === "excel" ? "Exportando…" : "Exportar Excel"}
          </Button>
          <Button variant="outline" size="sm" disabled={descargando !== null || !reporte.data} onClick={() => exportar("pdf")}>
            {descargando === "pdf" ? "Exportando…" : "Exportar PDF"}
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader><CardTitle>Filtros</CardTitle></CardHeader>
        <CardContent className="flex flex-wrap items-end gap-2">
          <label className="flex flex-col text-xs text-muted-foreground">
            Fecha desde
            <Input type="date" value={fechaDesde} onChange={(e) => setFechaDesde(e.target.value)} className={`${inputClase} w-36`} />
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Fecha hasta
            <Input type="date" value={fechaHasta} onChange={(e) => setFechaHasta(e.target.value)} className={`${inputClase} w-36`} />
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Cliente
            <select value={clienteId} onChange={(e) => setClienteId(e.target.value)} className={`${selectClase} min-w-40`} disabled={clientes.isLoading}>
              <option value="">Todos</option>
              {clientes.data?.content?.map((c) => <option key={c.id} value={c.id.toString()}>{c.nombre}</option>)}
            </select>
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Proyecto
            <select value={proyectoId} onChange={(e) => setProyectoId(e.target.value)} className={`${selectClase} min-w-36`} disabled={proyectos.isLoading}>
              <option value="">Todos</option>
              {proyectos.data?.content?.map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
            </select>
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Moneda
            <select value={monedaId} onChange={(e) => setMonedaId(e.target.value)} className={`${selectClase} w-24`} disabled={monedas.isLoading}>
              <option value="">Todas</option>
              {monedas.data?.content?.map((m) => <option key={m.id} value={m.id.toString()}>{m.codigo}</option>)}
            </select>
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Vencimiento
            <select value={estadoVencimiento} onChange={(e) => setEstadoVencimiento(e.target.value)} className={`${selectClase} min-w-36`}>
              <option value="">Todos</option>
              {ESTADOS_VENCIMIENTO.map((e) => <option key={e} value={e}>{ESTADO_LABEL[e]}</option>)}
            </select>
          </label>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>Facturas pendientes</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {reporte.isLoading ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[900px] text-left text-sm">
                  <thead className="text-muted-foreground">
                    <tr className="border-b border-border">
                      <th className="py-2 pr-4 font-medium">Cliente</th>
                      <th className="py-2 pr-4 font-medium">Proyecto</th>
                      <th className="py-2 pr-4 font-medium">Factura</th>
                      <th className="py-2 pr-4 font-medium">Fecha</th>
                      <th className="py-2 pr-4 font-medium">Vencimiento</th>
                      <th className="py-2 pr-4 font-medium">Moneda</th>
                      <th className="py-2 pr-4 font-medium">Total</th>
                      <th className="py-2 pr-4 font-medium">Saldo</th>
                      <th className="py-2 pr-4 font-medium">Saldo ARS</th>
                      <th className="py-2 pr-4 font-medium">Estado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(reporte.data?.filas ?? []).map((f) => (
                      <tr key={f.facturaVentaId} className="border-b border-border last:border-0">
                        <td className="py-2 pr-4">{f.clienteNombre}</td>
                        <td className="py-2 pr-4">{f.proyectoNombre ?? "—"}</td>
                        <td className="py-2 pr-4">{f.numero}</td>
                        <td className="py-2 pr-4">{f.fecha}</td>
                        <td className="py-2 pr-4">{f.fechaVencimiento ?? "—"}</td>
                        <td className="py-2 pr-4">{f.monedaCodigo}</td>
                        <td className="py-2 pr-4">{f.total.toFixed(2)}</td>
                        <td className="py-2 pr-4">{f.saldo.toFixed(2)}</td>
                        <td className="py-2 pr-4">{f.saldoArs.toFixed(2)}</td>
                        <td className="py-2 pr-4"><span className={`rounded px-1.5 py-0.5 text-xs ${ESTADO_CLASE[f.estadoVencimiento]}`}>{ESTADO_LABEL[f.estadoVencimiento]}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {reporte.data && reporte.data.filas.length === 0 && (
                <p className="text-sm text-muted-foreground">No hay facturas con saldo pendiente para estos filtros.</p>
              )}
              {reporte.data && reporte.data.totalesPorMoneda.length > 0 && (
                <div className="flex flex-wrap gap-4 border-t border-border pt-4 text-sm">
                  <span className="text-muted-foreground">Totales por moneda:</span>
                  {reporte.data.totalesPorMoneda.map((t) => (
                    <span key={t.monedaId}>{t.monedaCodigo}: <strong>{t.totalSaldo.toFixed(2)}</strong> (ARS {t.totalSaldoArs.toFixed(2)})</span>
                  ))}
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

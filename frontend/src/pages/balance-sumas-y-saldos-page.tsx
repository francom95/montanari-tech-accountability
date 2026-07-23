import { useMemo, useState } from "react"
import { Link } from "react-router-dom"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import { Input } from "@/components/ui/input"
import {
  descargarBalanceSumasYSaldosExcel,
  descargarBalanceSumasYSaldosPdf,
  useBalanceSumasYSaldos,
} from "@/hooks/use-balance-sumas-y-saldos"
import type { BalanceSumasYSaldosFiltros, BalanceSumasYSaldosNodo } from "@/types/balance-sumas-y-saldos"

const inputClase = "h-8"
const ETIQUETA_CLASE: Record<string, string> = {
  DEUDOR: "bg-primary/10 text-primary",
  ACREEDOR: "bg-amber-500/10 text-amber-600",
  SALDADA: "bg-muted text-muted-foreground",
}

function formatearNumero(valor: number) {
  return valor.toFixed(2)
}

function enlaceMayor(cuentaId: number, filtros: BalanceSumasYSaldosFiltros): string {
  const params = new URLSearchParams()
  if (filtros.fechaDesde) params.set("fechaDesde", filtros.fechaDesde)
  if (filtros.fechaHasta) params.set("fechaHasta", filtros.fechaHasta)
  const query = params.toString()
  return `/contabilidad/mayor/${cuentaId}${query ? `?${query}` : ""}`
}

export function BalanceSumasYSaldosPage() {
  const [fechaDesde, setFechaDesde] = useState("")
  const [fechaHasta, setFechaHasta] = useState("")
  const [incluirSinMovimiento, setIncluirSinMovimiento] = useState(false)
  const [nivelMaximo, setNivelMaximo] = useState("")
  const [descargando, setDescargando] = useState<"excel" | "pdf" | null>(null)

  const filtros: BalanceSumasYSaldosFiltros = useMemo(() => ({
    fechaDesde: fechaDesde || undefined,
    fechaHasta: fechaHasta || undefined,
    incluirSinMovimiento,
    nivelMaximo: nivelMaximo ? Number(nivelMaximo) : undefined,
  }), [fechaDesde, fechaHasta, incluirSinMovimiento, nivelMaximo])

  const balance = useBalanceSumasYSaldos(filtros)

  async function exportar(formato: "excel" | "pdf") {
    setDescargando(formato)
    try {
      if (formato === "excel") await descargarBalanceSumasYSaldosExcel(filtros)
      else await descargarBalanceSumasYSaldosPdf(filtros)
    } finally {
      setDescargando(null)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-semibold text-foreground">Balance de sumas y saldos</h1>
          <p className="text-sm text-muted-foreground">Todas las cuentas del plan, con roll-up de madre = Σ hijas.</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" disabled={descargando !== null || !balance.data} onClick={() => exportar("excel")}>
            {descargando === "excel" ? "Exportando…" : "Exportar Excel"}
          </Button>
          <Button variant="outline" size="sm" disabled={descargando !== null || !balance.data} onClick={() => exportar("pdf")}>
            {descargando === "pdf" ? "Exportando…" : "Exportar PDF"}
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader><CardTitle>Filtros</CardTitle></CardHeader>
        <CardContent className="flex flex-wrap items-end gap-4">
          <label className="flex flex-col text-xs text-muted-foreground">
            Fecha desde
            <Input type="date" value={fechaDesde} onChange={(e) => setFechaDesde(e.target.value)} className={`${inputClase} w-36`} />
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Fecha hasta
            <Input type="date" value={fechaHasta} onChange={(e) => setFechaHasta(e.target.value)} className={`${inputClase} w-36`} />
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Nivel máximo
            <Input type="number" min={1} value={nivelMaximo} onChange={(e) => setNivelMaximo(e.target.value)} className={`${inputClase} w-24`} placeholder="Todos" />
          </label>
          <label className="flex items-center gap-2 text-sm">
            <Checkbox checked={incluirSinMovimiento} onCheckedChange={(checked) => setIncluirSinMovimiento(checked === true)} />
            Incluir cuentas sin movimiento
          </label>
        </CardContent>
      </Card>

      {balance.data && !balance.data.balancea && (
        <p className="rounded-md bg-destructive/10 px-3 py-2 text-sm font-medium text-destructive">
          ⚠ El balance NO cierra: Σ debe ({formatearNumero(balance.data.totalDebe)}) ≠ Σ haber ({formatearNumero(balance.data.totalHaber)}),
          diferencia de {formatearNumero(balance.data.diferencia)}. Esto es señal de un bug real — no debería pasar nunca.
        </p>
      )}

      <Card>
        <CardHeader><CardTitle>Cuentas</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {balance.isLoading ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : (balance.data?.raices.length ?? 0) === 0 ? (
            <p className="text-sm text-muted-foreground">Sin cuentas para mostrar.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[800px] text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr className="border-b border-border">
                    <th className="py-2 pr-4 font-medium">Código</th>
                    <th className="py-2 pr-4 font-medium">Cuenta</th>
                    <th className="py-2 pr-4 font-medium">Debe</th>
                    <th className="py-2 pr-4 font-medium">Haber</th>
                    <th className="py-2 pr-4 font-medium">Saldo</th>
                    <th className="py-2 pr-4 font-medium" />
                  </tr>
                </thead>
                <tbody>
                  {(balance.data?.raices ?? []).map((nodo) => (
                    <FilaBalance key={nodo.cuentaId} nodo={nodo} nivel={0} filtros={filtros} />
                  ))}
                </tbody>
                <tfoot>
                  <tr className="border-t-2 border-border font-semibold">
                    <td className="py-2 pr-4" />
                    <td className="py-2 pr-4">TOTALES</td>
                    <td className="py-2 pr-4">{formatearNumero(balance.data?.totalDebe ?? 0)}</td>
                    <td className="py-2 pr-4">{formatearNumero(balance.data?.totalHaber ?? 0)}</td>
                    <td className="py-2 pr-4" colSpan={2}>
                      {balance.data?.balancea ? (
                        <span className="rounded bg-primary/10 px-1.5 py-0.5 text-xs text-primary">Balancea</span>
                      ) : (
                        <span className="rounded bg-destructive/10 px-1.5 py-0.5 text-xs text-destructive">No balancea</span>
                      )}
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function FilaBalance({ nodo, nivel, filtros }: { nodo: BalanceSumasYSaldosNodo; nivel: number; filtros: BalanceSumasYSaldosFiltros }) {
  const [abierto, setAbierto] = useState(true)
  const tieneHijos = nodo.hijos.length > 0

  return (
    <>
      <tr className="border-b border-border last:border-0 hover:bg-muted/50">
        <td className="py-2 pr-4 font-mono text-xs text-muted-foreground">{nodo.codigo}</td>
        <td className="py-2 pr-4" style={{ paddingLeft: nivel * 20 }}>
          <span className="flex items-center gap-2">
            {tieneHijos ? (
              <button type="button" onClick={() => setAbierto((a) => !a)} className="w-4 text-muted-foreground">
                {abierto ? "▾" : "▸"}
              </button>
            ) : (
              <span className="w-4" />
            )}
            {nodo.nombre}
            {nodo.contrarioAlEsperado && (
              <span title="Saldo del lado contrario al esperado para esta cuenta" className="text-amber-600">⚠</span>
            )}
          </span>
        </td>
        <td className="py-2 pr-4">{formatearNumero(nodo.debe)}</td>
        <td className="py-2 pr-4">{formatearNumero(nodo.haber)}</td>
        <td className="py-2 pr-4">
          {formatearNumero(nodo.saldo)}{" "}
          <span className={`rounded px-1.5 py-0.5 text-xs ${ETIQUETA_CLASE[nodo.saldoEtiqueta]}`}>{nodo.saldoEtiqueta}</span>
        </td>
        <td className="py-2 pr-4">
          {nodo.imputable && (
            <Link to={enlaceMayor(nodo.cuentaId, filtros)}>
              <Button variant="outline" size="sm">Ver mayor</Button>
            </Link>
          )}
        </td>
      </tr>
      {tieneHijos && abierto && nodo.hijos.map((hijo) => (
        <FilaBalance key={hijo.cuentaId} nodo={hijo} nivel={nivel + 1} filtros={filtros} />
      ))}
    </>
  )
}

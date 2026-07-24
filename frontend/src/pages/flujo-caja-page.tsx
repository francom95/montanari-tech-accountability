import { useMemo, useState } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import {
  descargarFlujoCajaExcel,
  descargarFlujoCajaPdf,
  useFlujoCajaCombinado,
  useFlujoCajaReal,
} from "@/hooks/use-flujo-caja"
import type { Granularidad, PuntoFlujoCaja } from "@/types/flujo-caja"

const selectClase = "h-8 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
const inputClase = "h-8"

const GRANULARIDADES: Granularidad[] = ["DIARIO", "SEMANAL", "MENSUAL"]
const GRANULARIDAD_LABEL: Record<Granularidad, string> = { DIARIO: "Diario", SEMANAL: "Semanal", MENSUAL: "Mensual" }

const pesos = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" })

function hoyIso(offsetDias = 0): string {
  const d = new Date()
  d.setDate(d.getDate() + offsetDias)
  return d.toISOString().slice(0, 10)
}

function GraficoSerie({ puntos }: { puntos: PuntoFlujoCaja[] }) {
  if (puntos.length === 0) {
    return <p className="text-sm text-muted-foreground">Sin datos para graficar.</p>
  }
  const ancho = 900
  const alto = 260
  const margen = { top: 16, right: 16, bottom: 24, left: 72 }
  const w = ancho - margen.left - margen.right
  const h = alto - margen.top - margen.bottom

  const valores = puntos.map((p) => p.saldoFinal)
  const min = Math.min(0, ...valores)
  const max = Math.max(0, ...valores)
  const rango = max - min || 1

  const x = (i: number) => margen.left + (puntos.length === 1 ? 0 : (i / (puntos.length - 1)) * w)
  const y = (valor: number) => margen.top + h - ((valor - min) / rango) * h

  const indicePrimerProyectado = puntos.findIndex((p) => !p.esReal)
  const puntosReal = indicePrimerProyectado === -1 ? puntos : puntos.slice(0, indicePrimerProyectado + 1)
  const puntosProyectado = indicePrimerProyectado === -1 ? [] : puntos.slice(indicePrimerProyectado)

  function pathDe(lista: PuntoFlujoCaja[], offset: number) {
    return lista.map((p, i) => `${i === 0 ? "M" : "L"} ${x(i + offset).toFixed(1)} ${y(p.saldoFinal).toFixed(1)}`).join(" ")
  }

  const yCero = y(0)

  return (
    <svg viewBox={`0 0 ${ancho} ${alto}`} className="w-full" role="img" aria-label="Serie de saldo final real y proyectado">
      <line x1={margen.left} y1={yCero} x2={ancho - margen.right} y2={yCero} stroke="currentColor" strokeOpacity={0.3} strokeDasharray="2 2" />
      <text x={margen.left - 8} y={yCero + 4} textAnchor="end" fontSize={10} fill="currentColor" opacity={0.6}>0</text>
      <text x={margen.left - 8} y={margen.top + 4} textAnchor="end" fontSize={10} fill="currentColor" opacity={0.6}>{pesos.format(max)}</text>
      <text x={margen.left - 8} y={margen.top + h} textAnchor="end" fontSize={10} fill="currentColor" opacity={0.6}>{pesos.format(min)}</text>

      {indicePrimerProyectado > 0 && (
        <line x1={x(indicePrimerProyectado)} y1={margen.top} x2={x(indicePrimerProyectado)} y2={margen.top + h}
          stroke="currentColor" strokeOpacity={0.4} strokeDasharray="4 4" />
      )}

      {puntosReal.length > 0 && (
        <path d={pathDe(puntosReal, 0)} fill="none" stroke="#2563eb" strokeWidth={2} />
      )}
      {puntosProyectado.length > 0 && (
        <path d={pathDe(puntosProyectado, indicePrimerProyectado)} fill="none" stroke="#2563eb" strokeWidth={2} strokeDasharray="5 4" />
      )}
      {puntos.map((p, i) => (
        <circle key={p.fecha + i} cx={x(i)} cy={y(p.saldoFinal)} r={p.saldoNegativo ? 4 : 2.5}
          fill={p.saldoNegativo ? "#dc2626" : "#2563eb"}>
          <title>{`${p.fecha}: ${pesos.format(p.saldoFinal)}${p.saldoNegativo ? " (saldo negativo)" : ""}`}</title>
        </circle>
      ))}
    </svg>
  )
}

export function FlujoCajaPage() {
  const [vista, setVista] = useState<"combinado" | "real">("combinado")
  const [diasAtras, setDiasAtras] = useState(30)
  const [diasAdelante, setDiasAdelante] = useState(30)
  const [desde, setDesde] = useState(hoyIso(-30))
  const [hasta, setHasta] = useState(hoyIso())
  const [granularidad, setGranularidad] = useState<Granularidad>("DIARIO")
  const [descargando, setDescargando] = useState(false)

  const combinado = useFlujoCajaCombinado(diasAtras, diasAdelante)
  const real = useFlujoCajaReal(desde, hasta, granularidad)

  const data = vista === "combinado" ? combinado.data : real.data
  const puntos = useMemo(() => data?.consolidado ?? [], [data])

  async function exportar(formato: "excel" | "pdf") {
    setDescargando(true)
    try {
      if (formato === "excel") await descargarFlujoCajaExcel(diasAtras, diasAdelante)
      else await descargarFlujoCajaPdf(diasAtras, diasAdelante)
    } finally {
      setDescargando(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-semibold text-foreground">Flujo de caja</h1>
          <p className="text-sm text-muted-foreground">
            Real (F2.4/F5.3, F4.4) + proyectado (cuotas F2.5, compromisos F8.2, vencimientos F8.1, CxP F4.5) consolidado en ARS.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" disabled={descargando} onClick={() => exportar("excel")}>Exportar Excel</Button>
          <Button variant="outline" disabled={descargando} onClick={() => exportar("pdf")}>Exportar PDF</Button>
        </div>
      </div>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between gap-2">
          <CardTitle>{vista === "combinado" ? "Real + proyectado" : "Real por período"}</CardTitle>
          <div className="flex gap-2">
            <Button variant={vista === "combinado" ? "default" : "outline"} size="sm" onClick={() => setVista("combinado")}>Combinado</Button>
            <Button variant={vista === "real" ? "default" : "outline"} size="sm" onClick={() => setVista("real")}>Real por período</Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {vista === "combinado" ? (
            <div className="flex flex-wrap items-center gap-2">
              <label className="flex flex-col text-xs text-muted-foreground">
                Días hacia atrás (real)
                <Input type="number" min={0} value={diasAtras} onChange={(e) => setDiasAtras(Number(e.target.value))} className={`${inputClase} w-28`} />
              </label>
              <label className="flex flex-col text-xs text-muted-foreground">
                Días hacia adelante (proyectado)
                <Input type="number" min={1} value={diasAdelante} onChange={(e) => setDiasAdelante(Number(e.target.value))} className={`${inputClase} w-28`} />
              </label>
            </div>
          ) : (
            <div className="flex flex-wrap items-center gap-2">
              <label className="flex flex-col text-xs text-muted-foreground">
                Desde
                <Input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} className={`${inputClase} w-36`} />
              </label>
              <label className="flex flex-col text-xs text-muted-foreground">
                Hasta
                <Input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} className={`${inputClase} w-36`} />
              </label>
              <label className="flex flex-col text-xs text-muted-foreground">
                Granularidad
                <select value={granularidad} onChange={(e) => setGranularidad(e.target.value as Granularidad)} className={`${selectClase} w-32`}>
                  {GRANULARIDADES.map((g) => <option key={g} value={g}>{GRANULARIDAD_LABEL[g]}</option>)}
                </select>
              </label>
            </div>
          )}

          {(vista === "combinado" ? combinado.isLoading : real.isLoading) ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : (
            <>
              <GraficoSerie puntos={puntos} />
              {data && data.advertencias.length > 0 && (
                <ul className="list-inside list-disc text-xs text-amber-600">
                  {data.advertencias.map((a) => <li key={a}>{a}</li>)}
                </ul>
              )}
            </>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>Detalle</CardTitle></CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[700px] text-left text-sm">
              <thead className="text-muted-foreground">
                <tr className="border-b border-border">
                  <th className="py-2 pr-4 font-medium">Fecha</th>
                  <th className="py-2 pr-4 font-medium">Tipo</th>
                  <th className="py-2 pr-4 font-medium">Saldo inicial</th>
                  <th className="py-2 pr-4 font-medium">Ingresos</th>
                  <th className="py-2 pr-4 font-medium">Egresos</th>
                  <th className="py-2 pr-4 font-medium">Saldo final</th>
                </tr>
              </thead>
              <tbody>
                {puntos.map((p) => (
                  <tr key={p.fecha} className={`border-b border-border last:border-0 ${p.saldoNegativo ? "text-destructive" : ""}`}>
                    <td className="py-2 pr-4">{p.fecha}</td>
                    <td className="py-2 pr-4">{p.esReal ? "Real" : "Proyectado"}</td>
                    <td className="py-2 pr-4">{pesos.format(p.saldoInicial)}</td>
                    <td className="py-2 pr-4">{pesos.format(p.ingresos)}</td>
                    <td className="py-2 pr-4">{pesos.format(p.egresos)}</td>
                    <td className="py-2 pr-4">{pesos.format(p.saldoFinal)}{p.saldoNegativo && " ⚠"}</td>
                  </tr>
                ))}
                {puntos.length === 0 && (
                  <tr><td colSpan={6} className="py-4 text-center text-sm text-muted-foreground">Sin datos para el rango seleccionado.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

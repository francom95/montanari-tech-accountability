import { useState } from "react"
import { Link } from "react-router-dom"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import {
  descargarEstadoResultadosExcel,
  descargarEstadoResultadosPdf,
  useEstadoResultadosAcumulado,
  useEstadoResultadosPorAnio,
  useEstadoResultadosPorMes,
  useEstadoResultadosPorProyecto,
  type VistaEstadoResultados,
} from "@/hooks/use-estado-resultados"
import { ETIQUETA_LINEA, LINEAS_ESTADO_RESULTADOS } from "@/types/estado-resultados"
import type { EstadoResultadosCalculado, LineaEstadoResultados } from "@/types/estado-resultados"

const MESES = [
  "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
  "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
]

const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"

const VISTAS: { valor: VistaEstadoResultados; etiqueta: string }[] = [
  { valor: "mes", etiqueta: "Por mes" },
  { valor: "anio", etiqueta: "Por año" },
  { valor: "acumulado", etiqueta: "Acumulado" },
  { valor: "por-proyecto", etiqueta: "Por proyecto" },
]

function formatearNumero(valor: number) {
  return valor.toFixed(2)
}

export function EstadoResultadosPage() {
  const hoy = new Date()
  const [anio, setAnio] = useState(hoy.getFullYear())
  const [mes, setMes] = useState(hoy.getMonth() + 1)
  const [vista, setVista] = useState<VistaEstadoResultados>("mes")
  const [descargando, setDescargando] = useState<"excel" | "pdf" | null>(null)

  const porMes = useEstadoResultadosPorMes(anio, mes, vista === "mes")
  const porAnio = useEstadoResultadosPorAnio(anio, vista === "anio")
  const acumulado = useEstadoResultadosAcumulado(anio, mes, vista === "acumulado")
  const porProyecto = useEstadoResultadosPorProyecto(anio, mes, vista === "por-proyecto")

  const cargando = vista === "mes" ? porMes.isLoading
    : vista === "anio" ? porAnio.isLoading
    : vista === "acumulado" ? acumulado.isLoading
    : porProyecto.isLoading

  const hayDatos = vista === "mes" ? !!porMes.data
    : vista === "anio" ? !!porAnio.data
    : vista === "acumulado" ? !!acumulado.data
    : !!porProyecto.data

  async function exportar(formato: "excel" | "pdf") {
    setDescargando(formato)
    try {
      if (formato === "excel") await descargarEstadoResultadosExcel(vista, anio, mes)
      else await descargarEstadoResultadosPdf(vista, anio, mes)
    } finally {
      setDescargando(null)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-semibold text-foreground">Estado de resultados</h1>
          <p className="text-sm text-muted-foreground">
            Apertura fija de 9 líneas con roll-up de rubros (mapeo{" "}
            <Link to="/reportes/mapeo-rubro-linea-er" className="underline">configurable</Link>).
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" disabled={descargando !== null || !hayDatos} onClick={() => exportar("excel")}>
            {descargando === "excel" ? "Exportando…" : "Exportar Excel"}
          </Button>
          <Button variant="outline" size="sm" disabled={descargando !== null || !hayDatos} onClick={() => exportar("pdf")}>
            {descargando === "pdf" ? "Exportando…" : "Exportar PDF"}
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader><CardTitle>Período y vista</CardTitle></CardHeader>
        <CardContent className="flex flex-wrap items-end gap-4">
          <label className="flex flex-col text-xs text-muted-foreground">
            Año
            <Input type="number" value={anio} onChange={(e) => setAnio(Number(e.target.value))} className="h-8 w-24" />
          </label>
          {vista !== "anio" && (
            <label className="flex flex-col text-xs text-muted-foreground">
              Mes
              <select value={mes} onChange={(e) => setMes(Number(e.target.value))} className={`${selectClase} w-36`}>
                {MESES.map((m, i) => <option key={m} value={i + 1}>{m}</option>)}
              </select>
            </label>
          )}
          <div className="flex gap-1">
            {VISTAS.map((v) => (
              <Button key={v.valor} size="sm" variant={vista === v.valor ? "default" : "outline"} onClick={() => setVista(v.valor)}>
                {v.etiqueta}
              </Button>
            ))}
          </div>
        </CardContent>
      </Card>

      {cargando && <p className="text-sm text-muted-foreground">Cargando…</p>}

      {vista !== "por-proyecto" && !cargando && (
        <TablaCalculado
          respuesta={vista === "mes" ? porMes.data : vista === "anio" ? porAnio.data : acumulado.data}
        />
      )}

      {vista === "por-proyecto" && !cargando && porProyecto.data && (
        <div className="space-y-4">
          {porProyecto.data.porProyecto.map((item) => (
            <Card key={item.proyectoId}>
              <CardHeader><CardTitle>{item.proyectoNombre}</CardTitle></CardHeader>
              <CardContent>
                <TablaCalculado respuesta={{ calculado: item.calculado, comparativoMesAnterior: null }} />
              </CardContent>
            </Card>
          ))}
          <Card>
            <CardHeader><CardTitle>Sin proyecto</CardTitle></CardHeader>
            <CardContent>
              <TablaCalculado respuesta={{ calculado: porProyecto.data.sinProyecto, comparativoMesAnterior: null }} />
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}

function TablaCalculado({ respuesta }: { respuesta: { calculado: EstadoResultadosCalculado; comparativoMesAnterior: import("@/types/estado-resultados").ComparativoMes | null } | undefined }) {
  const [abiertas, setAbiertas] = useState<Set<LineaEstadoResultados>>(new Set())

  if (!respuesta) return <p className="text-sm text-muted-foreground">Sin datos.</p>
  const c = respuesta.calculado

  function alternar(linea: LineaEstadoResultados) {
    setAbiertas((actual) => {
      const nuevo = new Set(actual)
      if (nuevo.has(linea)) nuevo.delete(linea)
      else nuevo.add(linea)
      return nuevo
    })
  }

  return (
    <Card>
      <CardContent className="space-y-4 pt-6">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[600px] text-left text-sm">
            <thead className="text-muted-foreground">
              <tr className="border-b border-border">
                <th className="py-2 pr-4 font-medium">Línea</th>
                <th className="py-2 pr-4 font-medium">Monto</th>
              </tr>
            </thead>
            <tbody>
              {LINEAS_ESTADO_RESULTADOS.map((lineaId) => {
                const linea = c.lineas.find((l) => l.linea === lineaId)
                const abierta = abiertas.has(lineaId)
                return (
                  <FragmentoLinea key={lineaId} lineaId={lineaId} linea={linea} abierta={abierta} onToggle={() => alternar(lineaId)} />
                )
              })}
              {(c.montoSinMapear !== 0 || c.cuentasSinMapear.length > 0) && (
                <tr className="border-b border-border bg-amber-500/10">
                  <td className="py-2 pr-4">⚠ Sin mapear (rubro sin línea asignada)</td>
                  <td className="py-2 pr-4">{formatearNumero(c.montoSinMapear)}</td>
                </tr>
              )}
              <tr className="border-t-2 border-border font-medium">
                <td className="py-2 pr-4">Resultado bruto</td>
                <td className="py-2 pr-4">{formatearNumero(c.resultadoBruto)}</td>
              </tr>
              <tr className="font-medium">
                <td className="py-2 pr-4">Resultado operativo</td>
                <td className="py-2 pr-4">{formatearNumero(c.resultadoOperativo)}</td>
              </tr>
              <tr className="font-semibold">
                <td className="py-2 pr-4">Resultado final</td>
                <td className="py-2 pr-4">{formatearNumero(c.resultadoFinal)}</td>
              </tr>
            </tbody>
          </table>
        </div>

        {respuesta.comparativoMesAnterior && (
          <div className="rounded-md bg-muted px-3 py-2 text-sm">
            <p>
              Mes anterior ({respuesta.comparativoMesAnterior.mesAnterior}/{respuesta.comparativoMesAnterior.anioAnterior}):{" "}
              {formatearNumero(respuesta.comparativoMesAnterior.resultadoFinalAnterior)}
            </p>
            <p>
              Variación: {formatearNumero(respuesta.comparativoMesAnterior.variacionAbsoluta)}
              {respuesta.comparativoMesAnterior.variacionPorcentual !== null
                ? ` (${respuesta.comparativoMesAnterior.variacionPorcentual.toFixed(2)} %)`
                : " (N/A, mes anterior en cero)"}
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function FragmentoLinea({
  lineaId, linea, abierta, onToggle,
}: {
  lineaId: LineaEstadoResultados
  linea: EstadoResultadosCalculado["lineas"][number] | undefined
  abierta: boolean
  onToggle: () => void
}) {
  const cuentas = linea?.cuentas ?? []
  const tieneCuentas = cuentas.length > 0

  return (
    <>
      <tr className="border-b border-border hover:bg-muted/50">
        <td className="py-2 pr-4">
          <span className="flex items-center gap-2">
            {tieneCuentas ? (
              <button type="button" onClick={onToggle} className="w-4 text-muted-foreground">{abierta ? "▾" : "▸"}</button>
            ) : (
              <span className="w-4" />
            )}
            {ETIQUETA_LINEA[lineaId]}
          </span>
        </td>
        <td className="py-2 pr-4">{formatearNumero(linea?.monto ?? 0)}</td>
      </tr>
      {abierta && cuentas.map((cuenta) => (
        <tr key={cuenta.cuentaId} className="border-b border-border text-muted-foreground">
          <td className="py-1 pr-4 pl-6">
            <Link to={`/contabilidad/mayor/${cuenta.cuentaId}`} className="hover:underline">
              {cuenta.codigo} {cuenta.nombre}
            </Link>
          </td>
          <td className="py-1 pr-4">{formatearNumero(cuenta.monto)}</td>
        </tr>
      ))}
    </>
  )
}

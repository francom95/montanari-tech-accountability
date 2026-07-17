import { useMemo, useState } from "react"
import { Link, useParams } from "react-router-dom"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { descargarMayorExcel, descargarMayorPdf, useMayor } from "@/hooks/use-mayor"
import { useClientes } from "@/hooks/use-cliente"
import { useMonedas } from "@/hooks/use-monedas"
import { useProveedores } from "@/hooks/use-proveedor"
import { useProyectos } from "@/hooks/use-proyecto"
import { useRubros } from "@/hooks/use-rubro"
import type { MayorFiltros } from "@/types/mayor"

const ORIGENES = [
  "MANUAL", "AJUSTE", "APERTURA", "IMPORTACION", "FACTURA_VENTA", "FACTURA_COMPRA",
  "COBRO", "PAGO", "LIQUIDACION_IVA", "LIQUIDACION_IIBB", "RESUMEN_TARJETA", "MOVIMIENTO_BANCARIO",
]

const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"
const inputClase = "h-8"

const ETIQUETA_CLASE: Record<string, string> = {
  DEUDOR: "bg-primary/10 text-primary",
  ACREEDOR: "bg-amber-500/10 text-amber-600",
  SALDADA: "bg-muted text-muted-foreground",
}

export function MayorPage() {
  const { cuentaId } = useParams<{ cuentaId: string }>()
  const cuentaIdNumero = cuentaId ? Number(cuentaId) : undefined

  const [rubroId, setRubroId] = useState("")
  const [proyectoId, setProyectoId] = useState("")
  const [clienteId, setClienteId] = useState("")
  const [proveedorId, setProveedorId] = useState("")
  const [origen, setOrigen] = useState("")
  const [monedaId, setMonedaId] = useState("")
  const [fechaDesde, setFechaDesde] = useState("")
  const [fechaHasta, setFechaHasta] = useState("")
  const [page, setPage] = useState(0)
  const [descargando, setDescargando] = useState<"excel" | "pdf" | null>(null)

  const filtros: MayorFiltros = useMemo(() => ({
    rubroId: rubroId ? Number(rubroId) : undefined,
    proyectoId: proyectoId ? Number(proyectoId) : undefined,
    clienteId: clienteId ? Number(clienteId) : undefined,
    proveedorId: proveedorId ? Number(proveedorId) : undefined,
    origen: origen || undefined,
    monedaId: monedaId ? Number(monedaId) : undefined,
    fechaDesde: fechaDesde || undefined,
    fechaHasta: fechaHasta || undefined,
  }), [rubroId, proyectoId, clienteId, proveedorId, origen, monedaId, fechaDesde, fechaHasta])

  const mayor = useMayor(cuentaIdNumero, filtros, page, 50)
  const rubros = useRubros({ page: 0, size: 100 })
  const proyectos = useProyectos({ page: 0, size: 200 })
  const clientes = useClientes({ page: 0, size: 200 })
  const proveedores = useProveedores({ page: 0, size: 200 })
  const monedas = useMonedas({ page: 0, size: 20 })

  function reiniciarPagina() {
    setPage(0)
  }

  async function exportar(formato: "excel" | "pdf") {
    if (!cuentaIdNumero || !mayor.data) return
    setDescargando(formato)
    try {
      if (formato === "excel") await descargarMayorExcel(cuentaIdNumero, mayor.data.cuentaContableCodigo, filtros)
      else await descargarMayorPdf(cuentaIdNumero, mayor.data.cuentaContableCodigo, filtros)
    } finally {
      setDescargando(null)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <Link to="/contabilidad" className="text-sm text-muted-foreground hover:underline">← Volver al plan de cuentas</Link>
          <h1 className="text-lg font-semibold text-foreground">
            Mayor{mayor.data ? ` — ${mayor.data.cuentaContableCodigo} ${mayor.data.cuentaContableNombre}` : ""}
          </h1>
          <p className="text-sm text-muted-foreground">
            {mayor.data?.esCuentaMadre ? "Cuenta madre: agrega el mayor de todas sus imputables descendientes." : "Movimientos confirmados, orden fecha → número → línea."}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" disabled={descargando !== null || !mayor.data} onClick={() => exportar("excel")}>
            {descargando === "excel" ? "Exportando…" : "Exportar Excel"}
          </Button>
          <Button variant="outline" size="sm" disabled={descargando !== null || !mayor.data} onClick={() => exportar("pdf")}>
            {descargando === "pdf" ? "Exportando…" : "Exportar PDF"}
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader><CardTitle>Filtros</CardTitle></CardHeader>
        <CardContent className="flex flex-wrap items-end gap-2">
          <label className="flex flex-col text-xs text-muted-foreground">
            Fecha desde
            <Input type="date" value={fechaDesde} onChange={(e) => { setFechaDesde(e.target.value); reiniciarPagina() }} className={`${inputClase} w-36`} />
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Fecha hasta
            <Input type="date" value={fechaHasta} onChange={(e) => { setFechaHasta(e.target.value); reiniciarPagina() }} className={`${inputClase} w-36`} />
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Rubro
            <select value={rubroId} onChange={(e) => { setRubroId(e.target.value); reiniciarPagina() }} className={`${selectClase} min-w-40`} disabled={rubros.isLoading}>
              <option value="">Todos</option>
              {rubros.data?.content?.map((r) => <option key={r.id} value={r.id.toString()}>{r.nombre}</option>)}
            </select>
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Proyecto
            <select value={proyectoId} onChange={(e) => { setProyectoId(e.target.value); reiniciarPagina() }} className={`${selectClase} min-w-36`} disabled={proyectos.isLoading}>
              <option value="">Todos</option>
              {proyectos.data?.content?.map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
            </select>
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Cliente
            <select value={clienteId} onChange={(e) => { setClienteId(e.target.value); reiniciarPagina() }} className={`${selectClase} min-w-36`} disabled={clientes.isLoading}>
              <option value="">Todos</option>
              {clientes.data?.content?.map((c) => <option key={c.id} value={c.id.toString()}>{c.nombre}</option>)}
            </select>
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Proveedor
            <select value={proveedorId} onChange={(e) => { setProveedorId(e.target.value); reiniciarPagina() }} className={`${selectClase} min-w-36`} disabled={proveedores.isLoading}>
              <option value="">Todos</option>
              {proveedores.data?.content?.map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
            </select>
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Origen
            <select value={origen} onChange={(e) => { setOrigen(e.target.value); reiniciarPagina() }} className={`${selectClase} min-w-40`}>
              <option value="">Todos</option>
              {ORIGENES.map((o) => <option key={o} value={o}>{o}</option>)}
            </select>
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Moneda
            <select value={monedaId} onChange={(e) => { setMonedaId(e.target.value); reiniciarPagina() }} className={`${selectClase} w-24`} disabled={monedas.isLoading}>
              <option value="">Todas</option>
              {monedas.data?.content?.map((m) => <option key={m.id} value={m.id.toString()}>{m.codigo}</option>)}
            </select>
          </label>
        </CardContent>
      </Card>

      {mayor.data?.vistaAnalitica && (
        <p className="rounded-md bg-muted px-3 py-2 text-xs text-muted-foreground">
          Vista analítica: el acumulado corre solo sobre el subconjunto filtrado — no es el saldo contable de la cuenta.
        </p>
      )}
      {mayor.data?.contrarioAlEsperado && (
        <p className="rounded-md bg-amber-500/10 px-3 py-2 text-xs text-amber-700">
          ⚠ El saldo quedó del lado contrario al esperado para esta cuenta. Es solo informativo: no bloquea nada.
        </p>
      )}

      <Card>
        <CardHeader><CardTitle>Movimientos</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {mayor.isLoading ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[900px] text-left text-sm">
                  <thead className="text-muted-foreground">
                    <tr className="border-b border-border">
                      <th className="py-2 pr-4 font-medium">Fecha</th>
                      <th className="py-2 pr-4 font-medium">N°</th>
                      <th className="py-2 pr-4 font-medium">Descripción</th>
                      {mayor.data?.esCuentaMadre && <th className="py-2 pr-4 font-medium">Cuenta</th>}
                      <th className="py-2 pr-4 font-medium">Debe</th>
                      <th className="py-2 pr-4 font-medium">Haber</th>
                      <th className="py-2 pr-4 font-medium">Saldo acumulado</th>
                      <th className="py-2 pr-4 font-medium">Origen</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(mayor.data?.filas ?? []).map((f, i) => (
                      <tr key={i} className={`border-b border-border last:border-0 ${f.esSaldoAnterior ? "font-medium text-muted-foreground" : ""}`}>
                        <td className="py-2 pr-4">{f.fecha ?? "—"}</td>
                        <td className="py-2 pr-4">{f.numeroAsiento ?? "—"}</td>
                        <td className="py-2 pr-4">{f.descripcion}</td>
                        {mayor.data?.esCuentaMadre && <td className="py-2 pr-4 font-mono text-xs">{f.cuentaContableCodigo}</td>}
                        <td className="py-2 pr-4">{f.debe != null ? f.debe.toFixed(2) : ""}</td>
                        <td className="py-2 pr-4">{f.haber != null ? f.haber.toFixed(2) : ""}</td>
                        <td className="py-2 pr-4">{f.saldoAcumulado.toFixed(2)}</td>
                        <td className="py-2 pr-4">{f.origen ?? ""}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {mayor.data && (
                <div className="flex items-center justify-between border-t border-border pt-4">
                  <div className="flex items-center gap-2">
                    <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}>Anterior</Button>
                    <span className="text-sm text-muted-foreground">Página {page + 1} de {mayor.data.totalPaginas} ({mayor.data.totalFilas} filas)</span>
                    <Button variant="outline" size="sm" disabled={page + 1 >= mayor.data.totalPaginas} onClick={() => setPage((p) => p + 1)}>Siguiente</Button>
                  </div>
                  <div className="text-sm">
                    {mayor.data.vistaAnalitica ? "Saldo del filtro: " : "Saldo final: "}
                    <strong>{mayor.data.saldoFinal.toFixed(2)}</strong>{" "}
                    <span className={`rounded px-1.5 py-0.5 text-xs ${ETIQUETA_CLASE[mayor.data.saldoFinalEtiqueta]}`}>
                      {mayor.data.saldoFinalEtiqueta}
                    </span>
                  </div>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

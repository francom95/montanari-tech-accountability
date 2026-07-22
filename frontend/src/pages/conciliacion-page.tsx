import { useMemo, useState } from "react"
import { Link } from "react-router-dom"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { useConciliacionResumen } from "@/hooks/use-conciliacion"
import { useCuentasBancarias } from "@/hooks/use-cuenta-bancaria"
import { useAsociarMovimientoBancario, useImputarMovimientoBancario } from "@/hooks/use-movimiento-bancario"
import type { ConciliacionMovimiento } from "@/types/conciliacion"

const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"
const inputClase = "h-8"

function primerDiaDelMes(): string {
  const hoy = new Date()
  return new Date(hoy.getFullYear(), hoy.getMonth(), 1).toISOString().slice(0, 10)
}

function hoyIso(): string {
  return new Date().toISOString().slice(0, 10)
}

export function ConciliacionPage() {
  const [cuentaBancariaId, setCuentaBancariaId] = useState("")
  const [fechaDesde, setFechaDesde] = useState(primerDiaDelMes())
  const [fechaHasta, setFechaHasta] = useState(hoyIso())
  const [toleranciaDias, setToleranciaDias] = useState("3")
  const [rechazadas, setRechazadas] = useState<Set<number>>(new Set())

  const cuentasBancarias = useCuentasBancarias({ activo: true, page: 0, size: 100 })
  const resumen = useConciliacionResumen({
    cuentaBancariaId: cuentaBancariaId ? Number(cuentaBancariaId) : undefined,
    fechaDesde,
    fechaHasta,
    toleranciaDias: Number(toleranciaDias) || 3,
  })

  const asociar = useAsociarMovimientoBancario()
  const imputar = useImputarMovimientoBancario()

  function aceptarMatch(m: ConciliacionMovimiento) {
    if (!m.matchSugerido) return
    asociar.mutate({ id: m.movimientoBancarioId, asientoNumero: m.matchSugerido.asientoNumero }, { onSuccess: () => resumen.refetch() })
  }

  function aplicarSugerencia(m: ConciliacionMovimiento) {
    if (!m.cuentaSugerida) return
    imputar.mutate({ id: m.movimientoBancarioId, cuentaContableId: m.cuentaSugerida.cuentaContableId }, { onSuccess: () => resumen.refetch() })
  }

  function rechazarSugerencia(movimientoBancarioId: number) {
    setRechazadas((actual) => new Set(actual).add(movimientoBancarioId))
  }

  const diferencia = resumen.data?.diferencia ?? 0
  const cantidadPendientesConSugerencia = useMemo(
    () => (resumen.data?.movimientos ?? []).filter((m) => m.estado === "PENDIENTE" && (m.matchSugerido || m.cuentaSugerida) && !rechazadas.has(m.movimientoBancarioId)).length,
    [resumen.data, rechazadas]
  )

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-lg font-semibold text-foreground">Conciliación bancaria</h1>
        <p className="text-sm text-muted-foreground">
          Sugerencias por importe exacto + fecha con tolerancia, e imputación rápida por descripción — vos siempre decidís
          si aceptar cada una. Para carga manual, corregir o descartar un movimiento, usá la{" "}
          <Link to="/bancos/movimientos" className="underline">
            bandeja de movimientos bancarios
          </Link>
          .
        </p>
      </div>

      <Card>
        <CardHeader><CardTitle>Filtros</CardTitle></CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-4">
          <label className="flex flex-col text-xs text-muted-foreground">
            Cuenta bancaria
            <select value={cuentaBancariaId} onChange={(e) => setCuentaBancariaId(e.target.value)} disabled={cuentasBancarias.isLoading} className={selectClase}>
              <option value="">Seleccionar…</option>
              {cuentasBancarias.data?.content?.map((c) => <option key={c.id} value={c.id.toString()}>{c.alias}</option>)}
            </select>
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Desde
            <Input type="date" value={fechaDesde} onChange={(e) => setFechaDesde(e.target.value)} className={inputClase} />
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Hasta
            <Input type="date" value={fechaHasta} onChange={(e) => setFechaHasta(e.target.value)} className={inputClase} />
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Tolerancia (días)
            <Input type="number" min="0" max="30" value={toleranciaDias} onChange={(e) => setToleranciaDias(e.target.value)} className={inputClase} />
          </label>
        </CardContent>
      </Card>

      {!cuentaBancariaId ? (
        <p className="text-sm text-muted-foreground">Elegí una cuenta bancaria para ver la conciliación.</p>
      ) : resumen.isLoading ? (
        <p className="text-sm text-muted-foreground">Cargando…</p>
      ) : resumen.data ? (
        <>
          <Card>
            <CardHeader><CardTitle>Resumen — {resumen.data.cuentaBancariaAlias}</CardTitle></CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-4">
              <div>
                <div className="text-xs text-muted-foreground">Saldo banco</div>
                <div className="text-lg font-semibold">{resumen.data.saldoBanco.toFixed(2)} {resumen.data.monedaCodigo}</div>
              </div>
              <div>
                <div className="text-xs text-muted-foreground">Saldo sistema</div>
                <div className="text-lg font-semibold">{resumen.data.saldoSistema.toFixed(2)} {resumen.data.monedaCodigo}</div>
              </div>
              <div>
                <div className="text-xs text-muted-foreground">Diferencia</div>
                <div className={`text-lg font-semibold ${Math.abs(diferencia) > 0.005 ? "text-destructive" : "text-primary"}`}>
                  {diferencia.toFixed(2)} {resumen.data.monedaCodigo}
                </div>
              </div>
              <div>
                <div className="text-xs text-muted-foreground">Sugerencias disponibles</div>
                <div className="text-lg font-semibold">{cantidadPendientesConSugerencia}</div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>Movimientos del período ({resumen.data.movimientos.length})</CardTitle></CardHeader>
            <CardContent>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[900px] text-left text-sm">
                  <thead className="text-muted-foreground">
                    <tr className="border-b border-border">
                      <th className="py-2 pr-4 font-medium">Fecha</th>
                      <th className="py-2 pr-4 font-medium">Descripción</th>
                      <th className="py-2 pr-4 font-medium">Importe</th>
                      <th className="py-2 pr-4 font-medium">Estado</th>
                      <th className="py-2 pr-4 font-medium">Sugerencia</th>
                    </tr>
                  </thead>
                  <tbody>
                    {resumen.data.movimientos.map((m) => (
                      <tr key={m.movimientoBancarioId} className="border-b border-border last:border-0 align-top">
                        <td className="py-2 pr-4">{m.fecha}</td>
                        <td className="py-2 pr-4">{m.descripcion}</td>
                        <td className={`py-2 pr-4 ${m.importe < 0 ? "text-destructive" : ""}`}>{m.importe.toFixed(2)} {m.monedaCodigo}</td>
                        <td className="py-2 pr-4">{m.estado}</td>
                        <td className="py-2 pr-4">
                          {m.asientoIdAsociado ? (
                            <span className="text-xs text-muted-foreground">Asiento N° {m.asientoNumeroAsociado}</span>
                          ) : rechazadas.has(m.movimientoBancarioId) ? (
                            <span className="text-xs text-muted-foreground">Sugerencia rechazada</span>
                          ) : m.matchSugerido ? (
                            <div className="flex items-center gap-2">
                              <span className="text-xs">
                                Match: Asiento N° {m.matchSugerido.asientoNumero}
                                {m.matchSugerido.origenTipo && ` (${m.matchSugerido.origenTipo}${m.matchSugerido.origenId ? ` #${m.matchSugerido.origenId}` : ""})`}
                                {" "}— {m.matchSugerido.fecha}
                              </span>
                              <Button size="sm" disabled={asociar.isPending} onClick={() => aceptarMatch(m)}>Aceptar</Button>
                              <Button size="sm" variant="outline" onClick={() => rechazarSugerencia(m.movimientoBancarioId)}>Rechazar</Button>
                            </div>
                          ) : m.cuentaSugerida ? (
                            <div className="flex items-center gap-2">
                              <span className="text-xs">Cuenta: {m.cuentaSugerida.cuentaContableCodigo} — {m.cuentaSugerida.cuentaContableNombre}</span>
                              <Button size="sm" disabled={imputar.isPending} onClick={() => aplicarSugerencia(m)}>Aplicar</Button>
                              <Button size="sm" variant="outline" onClick={() => rechazarSugerencia(m.movimientoBancarioId)}>Rechazar</Button>
                            </div>
                          ) : m.estado === "PENDIENTE" ? (
                            <span className="text-xs text-muted-foreground">Sin sugerencia — resolver en la bandeja</span>
                          ) : (
                            <span className="text-xs text-muted-foreground">—</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        </>
      ) : null}
    </div>
  )
}

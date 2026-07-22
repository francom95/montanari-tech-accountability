import { useMemo, useState } from "react"
import { useForm } from "react-hook-form"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { useCuentasBancarias } from "@/hooks/use-cuenta-bancaria"
import { useCuentasContables } from "@/hooks/use-cuenta-contable"
import {
  useAsociarMovimientoBancario,
  useConfirmarMovimientoBancario,
  useContadorPendientes,
  useCorregirMovimientoBancario,
  useCrearMovimientoBancario,
  useDescartarMovimientoBancario,
  useImputarMovimientoBancario,
  useMovimientosBancarios,
} from "@/hooks/use-movimiento-bancario"
import { useMonedas } from "@/hooks/use-monedas"
import type { EstadoMovimientoBancario, MovimientoBancario } from "@/types/movimiento-bancario"

const ESTADOS: EstadoMovimientoBancario[] = ["PENDIENTE", "CONCILIADO", "DESCARTADO"]
const ESTADO_LABEL: Record<EstadoMovimientoBancario, string> = {
  PENDIENTE: "Pendiente",
  CONCILIADO: "Conciliado",
  DESCARTADO: "Descartado",
}
const ESTADO_CLASE: Record<EstadoMovimientoBancario, string> = {
  PENDIENTE: "bg-amber-500/10 text-amber-600",
  CONCILIADO: "bg-primary/10 text-primary",
  DESCARTADO: "bg-muted text-muted-foreground",
}

const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"
const inputClase = "h-8"

type Valores = {
  cuentaBancariaId: string
  fecha: string
  descripcion: string
  importe: string
  monedaId: string
  tipoCambio: string
  referencia: string
  cuentaContableSugeridaId: string
  observaciones: string
}

const VACIO: Valores = {
  cuentaBancariaId: "", fecha: "", descripcion: "", importe: "", monedaId: "",
  tipoCambio: "1", referencia: "", cuentaContableSugeridaId: "", observaciones: "",
}

type AccionEnCurso = { id: number; tipo: "imputar" | "asociar" | "descartar" } | null

export function MovimientosBancariosPage() {
  const [page, setPage] = useState(0)
  const [cuentaBancariaFiltro, setCuentaBancariaFiltro] = useState("")
  const [estadoFiltro, setEstadoFiltro] = useState<EstadoMovimientoBancario>("PENDIENTE")
  const [mostrarForm, setMostrarForm] = useState(false)
  const [editando, setEditando] = useState<MovimientoBancario | null>(null)
  const [accionEnCurso, setAccionEnCurso] = useState<AccionEnCurso>(null)
  const [valorAccion, setValorAccion] = useState("")

  const query = useMovimientosBancarios({
    cuentaBancariaId: cuentaBancariaFiltro ? Number(cuentaBancariaFiltro) : undefined,
    estado: estadoFiltro,
    page,
    size: 10,
  })
  const contador = useContadorPendientes(cuentaBancariaFiltro ? Number(cuentaBancariaFiltro) : undefined)
  const cuentasBancarias = useCuentasBancarias({ activo: true, page: 0, size: 100 })
  const cuentasContables = useCuentasContables({ activo: true, page: 0, size: 500 })
  const cuentasImputables = useMemo(() => (cuentasContables.data?.content ?? []).filter((c) => c.imputable), [cuentasContables.data])
  const monedas = useMonedas({ page: 0, size: 20 })

  const crear = useCrearMovimientoBancario()
  const corregir = useCorregirMovimientoBancario()
  const confirmar = useConfirmarMovimientoBancario()
  const imputar = useImputarMovimientoBancario()
  const asociar = useAsociarMovimientoBancario()
  const descartar = useDescartarMovimientoBancario()

  const form = useForm<Valores>({ defaultValues: VACIO })

  function nuevoMovimiento() {
    setEditando(null)
    form.reset(VACIO)
    setMostrarForm(true)
  }

  function iniciarEdicion(m: MovimientoBancario) {
    setEditando(m)
    form.reset({
      cuentaBancariaId: m.cuentaBancariaId.toString(),
      fecha: m.fecha ?? "",
      descripcion: m.descripcion,
      importe: m.importe.toString(),
      monedaId: m.monedaId.toString(),
      tipoCambio: m.tipoCambio.toString(),
      referencia: m.referencia ?? "",
      cuentaContableSugeridaId: m.cuentaContableSugeridaId?.toString() ?? "",
      observaciones: m.observaciones ?? "",
    })
    setMostrarForm(true)
  }

  function cancelar() {
    setMostrarForm(false)
    setEditando(null)
  }

  function onSubmit(valores: Valores) {
    const payload = {
      cuentaBancariaId: Number(valores.cuentaBancariaId),
      fecha: valores.fecha || undefined,
      descripcion: valores.descripcion,
      importe: Number(valores.importe),
      monedaId: Number(valores.monedaId),
      tipoCambio: Number(valores.tipoCambio),
      referencia: valores.referencia || undefined,
      cuentaContableSugeridaId: valores.cuentaContableSugeridaId ? Number(valores.cuentaContableSugeridaId) : undefined,
      observaciones: valores.observaciones || undefined,
    }
    if (editando) {
      corregir.mutate({ id: editando.id, valores: payload }, { onSuccess: cancelar })
    } else {
      crear.mutate(payload, { onSuccess: cancelar })
    }
  }

  function ejecutarAccion() {
    if (!accionEnCurso) return
    const { id, tipo } = accionEnCurso
    if (tipo === "imputar" && valorAccion) {
      imputar.mutate({ id, cuentaContableId: Number(valorAccion) }, { onSuccess: cerrarAccion })
    } else if (tipo === "asociar" && valorAccion) {
      asociar.mutate({ id, asientoNumero: Number(valorAccion) }, { onSuccess: cerrarAccion })
    } else if (tipo === "descartar" && valorAccion.trim()) {
      descartar.mutate({ id, motivo: valorAccion }, { onSuccess: cerrarAccion })
    }
  }

  function cerrarAccion() {
    setAccionEnCurso(null)
    setValorAccion("")
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-semibold text-foreground">Movimientos bancarios</h1>
          <p className="text-sm text-muted-foreground">
            Bandeja de revisión (F5.1): nada impacta la contabilidad hasta confirmar, asociar o imputar cada movimiento.
            {contador.data && (
              <span className="ml-2 rounded bg-amber-500/10 px-1.5 py-0.5 text-xs font-medium text-amber-600">
                {contador.data.cantidad} pendientes
              </span>
            )}
          </p>
        </div>
        {!mostrarForm && <Button onClick={nuevoMovimiento}>Nuevo movimiento</Button>}
      </div>

      {mostrarForm && (
        <Card>
          <CardHeader><CardTitle>{editando ? `Editar movimiento` : "Nuevo movimiento (manual)"}</CardTitle></CardHeader>
          <CardContent>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-4">
                <label className="flex flex-col text-xs text-muted-foreground">
                  Cuenta bancaria
                  <select {...form.register("cuentaBancariaId")} disabled={cuentasBancarias.isLoading} className={selectClase}>
                    <option value="">Seleccionar…</option>
                    {cuentasBancarias.data?.content?.map((c) => <option key={c.id} value={c.id.toString()}>{c.alias}</option>)}
                  </select>
                </label>
                <label className="flex flex-col text-xs text-muted-foreground">
                  Fecha
                  <Input type="date" {...form.register("fecha")} className={inputClase} />
                </label>
                <label className="flex flex-col text-xs text-muted-foreground sm:col-span-2">
                  Descripción
                  <Input {...form.register("descripcion")} placeholder="Descripción del extracto" className={inputClase} />
                </label>
              </div>
              <div className="grid gap-4 sm:grid-cols-4">
                <label className="flex flex-col text-xs text-muted-foreground">
                  Importe (+ ingreso / - egreso)
                  <Input type="number" step="0.01" {...form.register("importe")} className={inputClase} />
                </label>
                <label className="flex flex-col text-xs text-muted-foreground">
                  Moneda
                  <select {...form.register("monedaId")} disabled={monedas.isLoading} className={selectClase}>
                    <option value="">Seleccionar…</option>
                    {monedas.data?.content?.map((m) => <option key={m.id} value={m.id.toString()}>{m.codigo}</option>)}
                  </select>
                </label>
                <label className="flex flex-col text-xs text-muted-foreground">
                  Tipo de cambio
                  <Input type="number" step="0.000001" {...form.register("tipoCambio")} className={inputClase} />
                </label>
                <label className="flex flex-col text-xs text-muted-foreground">
                  Referencia
                  <Input {...form.register("referencia")} className={inputClase} />
                </label>
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="flex flex-col text-xs text-muted-foreground">
                  Cuenta sugerida (opcional, para "confirmar")
                  <select {...form.register("cuentaContableSugeridaId")} disabled={cuentasContables.isLoading} className={selectClase}>
                    <option value="">Sin sugerencia (solo se podrá imputar)</option>
                    {cuentasImputables.map((c) => <option key={c.id} value={c.id.toString()}>{c.codigo} — {c.nombre}</option>)}
                  </select>
                </label>
                <label className="flex flex-col text-xs text-muted-foreground">
                  Observaciones
                  <Input {...form.register("observaciones")} className={inputClase} />
                </label>
              </div>
              <div className="flex gap-2">
                <Button type="submit" disabled={crear.isPending || corregir.isPending}>{editando ? "Guardar" : "Crear"}</Button>
                <Button type="button" variant="outline" onClick={cancelar}>Cancelar</Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader><CardTitle>Bandeja</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="flex gap-2">
            <select value={cuentaBancariaFiltro} onChange={(e) => { setCuentaBancariaFiltro(e.target.value); setPage(0) }} className={`${selectClase} max-w-56`}>
              <option value="">Todas las cuentas</option>
              {cuentasBancarias.data?.content?.map((c) => <option key={c.id} value={c.id.toString()}>{c.alias}</option>)}
            </select>
            <select value={estadoFiltro} onChange={(e) => { setEstadoFiltro(e.target.value as EstadoMovimientoBancario); setPage(0) }} className={`${selectClase} max-w-40`}>
              {ESTADOS.map((e) => <option key={e} value={e}>{ESTADO_LABEL[e]}</option>)}
            </select>
          </div>

          {query.isLoading ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[900px] text-left text-sm">
                  <thead className="text-muted-foreground">
                    <tr className="border-b border-border">
                      <th className="py-2 pr-4 font-medium">Fecha</th>
                      <th className="py-2 pr-4 font-medium">Cuenta</th>
                      <th className="py-2 pr-4 font-medium">Descripción</th>
                      <th className="py-2 pr-4 font-medium">Importe</th>
                      <th className="py-2 pr-4 font-medium">Sugerida</th>
                      <th className="py-2 pr-4 font-medium">Estado</th>
                      <th className="py-2 pr-4 font-medium">Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(query.data?.content ?? []).map((m) => (
                      <tr key={m.id} className="border-b border-border last:border-0 align-top">
                        <td className="py-2 pr-4">
                          {m.fecha ?? <span className="text-xs font-medium text-amber-600">Sin fecha</span>}
                        </td>
                        <td className="py-2 pr-4">{m.cuentaBancariaAlias}</td>
                        <td className="py-2 pr-4">{m.descripcion}</td>
                        <td className={`py-2 pr-4 ${m.importe < 0 ? "text-destructive" : ""}`}>{m.importe.toFixed(2)} {m.monedaCodigo}</td>
                        <td className="py-2 pr-4">{m.cuentaContableSugeridaCodigo ?? "—"}</td>
                        <td className="py-2 pr-4">
                          <span className={`rounded px-1.5 py-0.5 text-xs ${ESTADO_CLASE[m.estado]}`}>{ESTADO_LABEL[m.estado]}</span>
                          {m.estado === "CONCILIADO" && m.asientoNumero && <div className="mt-1 text-xs text-muted-foreground">Asiento N° {m.asientoNumero}</div>}
                          {m.estado === "DESCARTADO" && m.motivoDescarte && <div className="mt-1 text-xs text-muted-foreground">{m.motivoDescarte}</div>}
                        </td>
                        <td className="py-2 pr-4">
                          {m.estado !== "PENDIENTE" ? (
                            <span className="text-xs text-muted-foreground">—</span>
                          ) : accionEnCurso?.id === m.id ? (
                            <div className="flex items-center gap-2">
                              {accionEnCurso.tipo === "imputar" && (
                                <select value={valorAccion} onChange={(e) => setValorAccion(e.target.value)} className={`${selectClase} min-w-48`}>
                                  <option value="">Elegir cuenta…</option>
                                  {cuentasImputables.map((c) => <option key={c.id} value={c.id.toString()}>{c.codigo} — {c.nombre}</option>)}
                                </select>
                              )}
                              {accionEnCurso.tipo === "asociar" && (
                                <Input type="number" placeholder="N° de asiento" value={valorAccion} onChange={(e) => setValorAccion(e.target.value)} className={`${inputClase} w-32`} />
                              )}
                              {accionEnCurso.tipo === "descartar" && (
                                <Input placeholder="Motivo…" value={valorAccion} onChange={(e) => setValorAccion(e.target.value)} className={`${inputClase} w-40`} />
                              )}
                              <Button size="sm" disabled={!valorAccion || imputar.isPending || asociar.isPending || descartar.isPending} onClick={ejecutarAccion}>Confirmar</Button>
                              <Button variant="outline" size="sm" onClick={cerrarAccion}>Cancelar</Button>
                            </div>
                          ) : (
                            <div className="flex flex-wrap gap-2">
                              <Button variant="outline" size="sm" onClick={() => iniciarEdicion(m)}>Editar</Button>
                              <Button variant="outline" size="sm" disabled={!m.fecha || !m.cuentaContableSugeridaId || confirmar.isPending} onClick={() => confirmar.mutate(m.id)}>Confirmar</Button>
                              <Button variant="outline" size="sm" disabled={!m.fecha} onClick={() => setAccionEnCurso({ id: m.id, tipo: "imputar" })}>Imputar</Button>
                              <Button variant="outline" size="sm" onClick={() => setAccionEnCurso({ id: m.id, tipo: "asociar" })}>Asociar</Button>
                              <Button variant="outline" size="sm" onClick={() => setAccionEnCurso({ id: m.id, tipo: "descartar" })}>Descartar</Button>
                              {!m.fecha && (
                                <span className="text-xs text-muted-foreground">Completá la fecha con "Editar" para poder confirmar/imputar</span>
                              )}
                            </div>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="flex items-center gap-2">
                <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}>Anterior</Button>
                <span className="text-sm text-muted-foreground">Página {page + 1} de {query.data?.totalPages || 1}</span>
                <Button variant="outline" size="sm" disabled={page + 1 >= (query.data?.totalPages ?? 1)} onClick={() => setPage((p) => p + 1)}>Siguiente</Button>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

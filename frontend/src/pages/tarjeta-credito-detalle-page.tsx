import { useMemo, useRef, useState } from "react"
import { Link, useParams } from "react-router-dom"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import {
  useAnularPagoTarjeta,
  useClasificarConsumo,
  useClasificarMasivamente,
  useConfirmarImportacionConsumos,
  useConfirmarPagoTarjeta,
  useConsumosTarjeta,
  useContadorSinClasificar,
  useCrearPagoTarjeta,
  usePagosTarjeta,
  usePrevisualizarConsumosTarjeta,
} from "@/hooks/use-consumo-tarjeta"
import { useCuentasContables } from "@/hooks/use-cuenta-contable"
import { useProveedores } from "@/hooks/use-proveedor"
import { useProyectos } from "@/hooks/use-proyecto"
import { useConceptos } from "@/hooks/use-concepto"
import { useTarjetaCredito } from "@/hooks/use-tarjeta-credito"
import type { ConsumoImportacionPreview, ConsumoTarjeta, PagoTarjeta } from "@/types/consumo-tarjeta"

const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"
const inputClase = "h-8"

type ConsumoEditable = ConsumoImportacionPreview & { incluir: boolean }

export function TarjetaCreditoDetallePage() {
  const { id } = useParams<{ id: string }>()
  const tarjetaCreditoId = id ? Number(id) : undefined

  const tarjeta = useTarjetaCredito(tarjetaCreditoId)
  const contadorSinClasificar = useContadorSinClasificar(tarjetaCreditoId)

  return (
    <div className="space-y-6">
      <div>
        <Link to="/bancos/tarjetas" className="text-sm text-muted-foreground underline">← Tarjetas de crédito</Link>
        <h1 className="text-lg font-semibold text-foreground">
          {tarjeta.data ? tarjeta.data.entidad : "Tarjeta de crédito"}
        </h1>
        {tarjeta.data && (
          <p className="text-sm text-muted-foreground">
            Cierre: día {tarjeta.data.diaCierre} · Vencimiento: día {tarjeta.data.diaVencimiento} · Cuenta de débito: {tarjeta.data.cuentaBancariaDebitoAlias}
            {contadorSinClasificar.data ? (
              <span className="ml-2 rounded bg-amber-500/10 px-1.5 py-0.5 text-xs font-medium text-amber-600">
                {contadorSinClasificar.data} consumos sin clasificar
              </span>
            ) : null}
          </p>
        )}
      </div>

      {tarjeta.data && (
        <Card>
          <CardHeader><CardTitle>Saldo</CardTitle></CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-3">
            <div>
              <div className="text-xs text-muted-foreground">Saldo actual</div>
              <div className="text-lg font-semibold">{tarjeta.data.saldoActual} {tarjeta.data.monedaCodigo}</div>
            </div>
            <div>
              <div className="text-xs text-muted-foreground">Saldo inicial</div>
              <div className="text-lg font-semibold">{tarjeta.data.saldoInicial} {tarjeta.data.monedaCodigo}</div>
            </div>
            <div>
              <div className="text-xs text-muted-foreground">Cuenta contable</div>
              <div className="text-lg font-semibold">
                {tarjeta.data.cuentaContableCodigo ?? <span className="text-destructive">Sin configurar</span>}
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {tarjetaCreditoId && <ImportarResumenSection tarjetaCreditoId={tarjetaCreditoId} />}
      {tarjetaCreditoId && <ConsumosSection tarjetaCreditoId={tarjetaCreditoId} />}
      {tarjetaCreditoId && <PagoResumenSection tarjetaCreditoId={tarjetaCreditoId} tarjeta={tarjeta.data} />}
    </div>
  )
}

function ImportarResumenSection({ tarjetaCreditoId }: { tarjetaCreditoId: number }) {
  const inputArchivoRef = useRef<HTMLInputElement>(null)
  const [filas, setFilas] = useState<ConsumoEditable[]>([])
  const [tipoCambioUsd, setTipoCambioUsd] = useState("")
  const [resultados, setResultados] = useState<{ descripcion: string; resultado: string; motivoError: string | null }[] | null>(null)

  const previsualizar = usePrevisualizarConsumosTarjeta()
  const confirmar = useConfirmarImportacionConsumos()

  const hayFilasEnUsd = useMemo(() => filas.some((f) => f.monedaCodigo === "USD"), [filas])
  const cantidadAImportar = filas.filter((f) => !f.duplicado && f.incluir).length

  async function onPrevisualizar() {
    const archivo = inputArchivoRef.current?.files?.[0]
    if (!archivo) return
    setResultados(null)
    const preview = await previsualizar.mutateAsync({ tarjetaCreditoId, archivo })
    setFilas(preview.map((p) => ({ ...p, incluir: !p.duplicado })))
    if (inputArchivoRef.current) inputArchivoRef.current.value = ""
  }

  async function onConfirmar() {
    const aImportar = filas.filter((f) => !f.duplicado && f.incluir)
    const resultado = await confirmar.mutateAsync({
      tarjetaCreditoId,
      tipoCambioUsd: hayFilasEnUsd && tipoCambioUsd ? Number(tipoCambioUsd) : undefined,
      filas: aImportar.map((f) => ({
        fecha: f.fecha, descripcion: f.descripcion, importe: f.importe, monedaCodigo: f.monedaCodigo,
        referencia: f.referencia ?? undefined, hash: f.hash,
      })),
    })
    setResultados(resultado)
    setFilas([])
  }

  return (
    <Card>
      <CardHeader><CardTitle>Importar resumen (PDF)</CardTitle></CardHeader>
      <CardContent className="space-y-4">
        <div className="flex items-center gap-2">
          <input ref={inputArchivoRef} type="file" accept="application/pdf" className="text-sm" />
          <Button onClick={onPrevisualizar} disabled={previsualizar.isPending}>
            {previsualizar.isPending ? "Leyendo…" : "Previsualizar"}
          </Button>
        </div>

        {filas.length > 0 && (
          <>
            <p className="rounded-md bg-muted px-3 py-2 text-xs text-muted-foreground">
              El resumen trae tanto pagos/devoluciones como el detalle de consumos del período. Desmarcá lo que no
              corresponda importar como consumo a clasificar acá.
            </p>
            {hayFilasEnUsd && (
              <label className="flex max-w-56 flex-col text-xs text-muted-foreground">
                Tipo de cambio para filas en USD
                <Input type="number" step="0.0001" value={tipoCambioUsd} onChange={(e) => setTipoCambioUsd(e.target.value)} className={inputClase} />
              </label>
            )}
            <div className="overflow-x-auto">
              <table className="w-full min-w-[700px] text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr className="border-b border-border">
                    <th className="py-2 pr-4 font-medium">Importar</th>
                    <th className="py-2 pr-4 font-medium">Fecha</th>
                    <th className="py-2 pr-4 font-medium">Descripción</th>
                    <th className="py-2 pr-4 font-medium">Importe</th>
                    <th className="py-2 pr-4 font-medium">Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {filas.map((f) => (
                    <tr key={f.hash} className={`border-b border-border last:border-0 ${f.duplicado ? "opacity-60" : ""}`}>
                      <td className="py-2 pr-4">
                        <input type="checkbox" checked={f.incluir && !f.duplicado} disabled={f.duplicado}
                          onChange={(e) => setFilas((actual) => actual.map((x) => (x.hash === f.hash ? { ...x, incluir: e.target.checked } : x)))} />
                      </td>
                      <td className="py-2 pr-4">{f.fecha}</td>
                      <td className="py-2 pr-4">{f.descripcion}</td>
                      <td className={`py-2 pr-4 ${f.importe < 0 ? "text-destructive" : ""}`}>{f.importe.toFixed(2)} {f.monedaCodigo}</td>
                      <td className="py-2 pr-4">
                        {f.duplicado
                          ? <span className="rounded bg-muted px-1.5 py-0.5 text-xs text-muted-foreground">Ya importado</span>
                          : <span className="rounded bg-primary/10 px-1.5 py-0.5 text-xs text-primary">Nuevo</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Button onClick={onConfirmar} disabled={cantidadAImportar === 0 || confirmar.isPending || (hayFilasEnUsd && !tipoCambioUsd)}>
              {confirmar.isPending ? "Importando…" : `Confirmar importación (${cantidadAImportar})`}
            </Button>
          </>
        )}

        {resultados && (
          <div className="space-y-1 text-sm">
            {resultados.map((r, i) => (
              <div key={i}>
                {r.resultado === "IMPORTADO" && <span className="text-primary">✓ {r.descripcion}</span>}
                {r.resultado === "DUPLICADO" && <span className="text-muted-foreground">– {r.descripcion} (ya importado)</span>}
                {r.resultado === "ERROR" && <span className="text-destructive">✗ {r.descripcion}: {r.motivoError}</span>}
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function ConsumosSection({ tarjetaCreditoId }: { tarjetaCreditoId: number }) {
  const [soloSinClasificar, setSoloSinClasificar] = useState(true)
  const [clasificando, setClasificando] = useState<number | null>(null)
  const [cuentaContableId, setCuentaContableId] = useState("")
  const [proveedorId, setProveedorId] = useState("")
  const [proyectoId, setProyectoId] = useState("")
  const [conceptoId, setConceptoId] = useState("")

  const consumos = useConsumosTarjeta({ tarjetaCreditoId, soloSinClasificar, size: 50 })
  const cuentasContables = useCuentasContables({ activo: true, page: 0, size: 500 })
  const cuentasImputables = useMemo(() => (cuentasContables.data?.content ?? []).filter((c) => c.imputable), [cuentasContables.data])
  const proveedores = useProveedores({ activo: true, page: 0, size: 200 })
  const proyectos = useProyectos({ activo: true, page: 0, size: 200 })
  const conceptos = useConceptos({ activo: true, page: 0, size: 200 })

  const clasificar = useClasificarConsumo()
  const clasificarMasivo = useClasificarMasivamente()

  function iniciarClasificacion(c: ConsumoTarjeta) {
    setClasificando(c.id)
    setCuentaContableId(c.cuentaContableId?.toString() ?? "")
    setProveedorId(c.proveedorId?.toString() ?? "")
    setProyectoId(c.proyectoId?.toString() ?? "")
    setConceptoId(c.conceptoId?.toString() ?? "")
  }

  function guardarClasificacion() {
    if (!clasificando || !cuentaContableId) return
    clasificar.mutate({
      id: clasificando,
      valores: {
        cuentaContableId: Number(cuentaContableId),
        proveedorId: proveedorId ? Number(proveedorId) : undefined,
        proyectoId: proyectoId ? Number(proyectoId) : undefined,
        conceptoId: conceptoId ? Number(conceptoId) : undefined,
      },
    }, { onSuccess: () => setClasificando(null) })
  }

  return (
    <Card>
      <CardHeader><CardTitle>Consumos ({consumos.data?.totalElements ?? 0})</CardTitle></CardHeader>
      <CardContent className="space-y-4">
        <div className="flex items-center justify-between">
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={soloSinClasificar} onChange={(e) => setSoloSinClasificar(e.target.checked)} />
            Solo sin clasificar
          </label>
          <Button
            variant="outline" size="sm"
            disabled={clasificarMasivo.isPending}
            onClick={() => clasificarMasivo.mutate(tarjetaCreditoId)}
          >
            {clasificarMasivo.isPending ? "Clasificando…" : "Clasificar masivamente por reglas"}
          </Button>
        </div>

        {consumos.isLoading ? (
          <p className="text-sm text-muted-foreground">Cargando…</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left text-sm">
              <thead className="text-muted-foreground">
                <tr className="border-b border-border">
                  <th className="py-2 pr-4 font-medium">Fecha</th>
                  <th className="py-2 pr-4 font-medium">Descripción</th>
                  <th className="py-2 pr-4 font-medium">Importe</th>
                  <th className="py-2 pr-4 font-medium">Clasificación</th>
                  <th className="py-2 pr-4 font-medium">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {(consumos.data?.content ?? []).map((c) => (
                  <tr key={c.id} className="border-b border-border last:border-0 align-top">
                    <td className="py-2 pr-4">{c.fecha}</td>
                    <td className="py-2 pr-4">{c.descripcion}</td>
                    <td className={`py-2 pr-4 ${c.importe < 0 ? "text-destructive" : ""}`}>{c.importe.toFixed(2)} {c.monedaCodigo}</td>
                    <td className="py-2 pr-4">
                      {c.cuentaContableCodigo
                        ? <span>{c.cuentaContableCodigo} — {c.cuentaContableNombre}{c.proveedorNombre ? ` · ${c.proveedorNombre}` : ""}</span>
                        : <span className="text-xs text-amber-600">Sin clasificar</span>}
                    </td>
                    <td className="py-2 pr-4">
                      {clasificando === c.id ? (
                        <div className="flex flex-wrap items-center gap-2">
                          <select value={cuentaContableId} onChange={(e) => setCuentaContableId(e.target.value)} className={`${selectClase} min-w-40`}>
                            <option value="">Cuenta…</option>
                            {cuentasImputables.map((cc) => <option key={cc.id} value={cc.id.toString()}>{cc.codigo} — {cc.nombre}</option>)}
                          </select>
                          <select value={proveedorId} onChange={(e) => setProveedorId(e.target.value)} className={`${selectClase} min-w-32`}>
                            <option value="">Proveedor</option>
                            {(proveedores.data?.content ?? []).map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
                          </select>
                          <select value={proyectoId} onChange={(e) => setProyectoId(e.target.value)} className={`${selectClase} min-w-32`}>
                            <option value="">Proyecto</option>
                            {(proyectos.data?.content ?? []).map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
                          </select>
                          <select value={conceptoId} onChange={(e) => setConceptoId(e.target.value)} className={`${selectClase} min-w-32`}>
                            <option value="">Suscripción</option>
                            {(conceptos.data?.content ?? []).map((cp) => <option key={cp.id} value={cp.id.toString()}>{cp.nombre}</option>)}
                          </select>
                          <Button size="sm" disabled={!cuentaContableId || clasificar.isPending} onClick={guardarClasificacion}>Guardar</Button>
                          <Button size="sm" variant="outline" onClick={() => setClasificando(null)}>Cancelar</Button>
                        </div>
                      ) : (
                        <Button size="sm" variant="outline" onClick={() => iniciarClasificacion(c)}>Clasificar</Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function PagoResumenSection({ tarjetaCreditoId, tarjeta }: { tarjetaCreditoId: number; tarjeta: ReturnType<typeof useTarjetaCredito>["data"] }) {
  const [importe, setImporte] = useState("")
  const [fecha, setFecha] = useState(new Date().toISOString().slice(0, 10))

  const pagos = usePagosTarjeta(tarjetaCreditoId, 0, 10)
  const crear = useCrearPagoTarjeta()
  const confirmar = useConfirmarPagoTarjeta()
  const anular = useAnularPagoTarjeta()

  function onCrear() {
    if (!tarjeta || !importe) return
    crear.mutate({
      tarjetaCreditoId, fecha, importe: Number(importe), monedaId: tarjeta.monedaId, tipoCambio: 1,
    }, { onSuccess: () => setImporte("") })
  }

  return (
    <Card>
      <CardHeader><CardTitle>Pagar resumen</CardTitle></CardHeader>
      <CardContent className="space-y-4">
        <p className="text-xs text-muted-foreground">
          Puede ser un pago parcial (pago mínimo) — el resto queda como saldo pendiente de la tarjeta.
        </p>
        <div className="grid gap-3 sm:grid-cols-3">
          <label className="flex flex-col text-xs text-muted-foreground">
            Fecha
            <Input type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} className={inputClase} />
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Importe a pagar
            <Input type="number" step="0.01" value={importe} onChange={(e) => setImporte(e.target.value)} className={inputClase} />
          </label>
          <div className="flex items-end">
            <Button onClick={onCrear} disabled={!importe || crear.isPending}>Registrar pago (borrador)</Button>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full min-w-[700px] text-left text-sm">
            <thead className="text-muted-foreground">
              <tr className="border-b border-border">
                <th className="py-2 pr-4 font-medium">Fecha</th>
                <th className="py-2 pr-4 font-medium">Importe</th>
                <th className="py-2 pr-4 font-medium">Estado</th>
                <th className="py-2 pr-4 font-medium">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {(pagos.data?.content ?? []).map((p: PagoTarjeta) => (
                <tr key={p.id} className="border-b border-border last:border-0">
                  <td className="py-2 pr-4">{p.fecha}</td>
                  <td className="py-2 pr-4">{p.importe.toFixed(2)} {p.monedaCodigo}</td>
                  <td className="py-2 pr-4">{p.estado}{p.asientoNumero ? ` (Asiento N° ${p.asientoNumero})` : ""}</td>
                  <td className="py-2 pr-4">
                    {p.estado === "BORRADOR" && (
                      <Button size="sm" variant="outline" disabled={confirmar.isPending} onClick={() => confirmar.mutate(p.id)}>Confirmar</Button>
                    )}
                    {p.estado === "CONFIRMADO" && (
                      <Button size="sm" variant="outline" disabled={anular.isPending} onClick={() => anular.mutate({ id: p.id, motivo: "Anulado por el usuario" })}>Anular</Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </CardContent>
    </Card>
  )
}

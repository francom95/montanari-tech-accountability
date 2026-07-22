import { useMemo, useRef, useState } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { useCuentasBancarias } from "@/hooks/use-cuenta-bancaria"
import { useConfirmarImportacionBancaria, usePrevisualizarResumenBancario } from "@/hooks/use-importacion-bancaria"
import type { FilaImportacionBancariaPreview, FilaImportacionBancariaResultado, OrigenConParser } from "@/types/importacion-bancaria"

const ORIGENES: { value: OrigenConParser; label: string; accept: string }[] = [
  { value: "GALICIA", label: "Banco Galicia (home banking, Excel)", accept: ".xlsx,.xls" },
  { value: "MERCADO_PAGO", label: "Mercado Pago (resumen de cuenta, Excel)", accept: ".xlsx,.xls" },
  { value: "TARJETA_CREDITO", label: "Resumen de tarjeta de crédito (PDF)", accept: "application/pdf" },
]

const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"
const inputClase = "h-8"

type FilaEditable = FilaImportacionBancariaPreview & { fechaEditada: string; incluir: boolean }

export function ImportacionBancariaPage() {
  const inputArchivoRef = useRef<HTMLInputElement>(null)
  const [origen, setOrigen] = useState<OrigenConParser>("GALICIA")
  const [cuentaBancariaId, setCuentaBancariaId] = useState("")
  const [tipoCambioUsd, setTipoCambioUsd] = useState("")
  const [filas, setFilas] = useState<FilaEditable[]>([])
  const [resultados, setResultados] = useState<FilaImportacionBancariaResultado[] | null>(null)

  const previsualizar = usePrevisualizarResumenBancario()
  const confirmar = useConfirmarImportacionBancaria()
  const cuentasBancarias = useCuentasBancarias({ activo: true, page: 0, size: 100 })

  const origenSeleccionado = ORIGENES.find((o) => o.value === origen)!
  const hayFilasEnUsd = useMemo(() => filas.some((f) => f.monedaCodigo === "USD"), [filas])
  const hayFechasFaltantes = useMemo(() => filas.some((f) => !f.fechaEditada), [filas])

  async function onPrevisualizar() {
    const archivo = inputArchivoRef.current?.files?.[0]
    if (!archivo || !cuentaBancariaId) return
    setResultados(null)
    const previews = await previsualizar.mutateAsync({ origen, cuentaBancariaId: Number(cuentaBancariaId), archivo })
    setFilas(previews.map((p) => ({ ...p, fechaEditada: p.fecha ?? "", incluir: !p.duplicado })))
    if (inputArchivoRef.current) inputArchivoRef.current.value = ""
  }

  async function onConfirmar() {
    const aImportar = filas.filter((f) => !f.duplicado && f.incluir)
    const payload = aImportar.map((f) => ({
      fecha: f.fechaEditada || undefined,
      descripcion: f.descripcion,
      importe: f.importe,
      monedaCodigo: f.monedaCodigo,
      referencia: f.referencia ?? undefined,
      hash: f.hash,
    }))
    const resultado = await confirmar.mutateAsync({
      origen,
      cuentaBancariaId: Number(cuentaBancariaId),
      tipoCambioUsd: hayFilasEnUsd && tipoCambioUsd ? Number(tipoCambioUsd) : undefined,
      filas: payload,
    })
    setResultados(resultado)
    setFilas([])
  }

  function actualizarFecha(hash: string, fecha: string) {
    setFilas((actual) => actual.map((f) => (f.hash === hash ? { ...f, fechaEditada: fecha } : f)))
  }

  function alternarIncluir(hash: string, incluir: boolean) {
    setFilas((actual) => actual.map((f) => (f.hash === hash ? { ...f, incluir } : f)))
  }

  const cantidadDuplicados = filas.filter((f) => f.duplicado).length
  const cantidadAImportar = filas.filter((f) => !f.duplicado && f.incluir).length

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-lg font-semibold text-foreground">Importación de resúmenes bancarios</h1>
        <p className="text-sm text-muted-foreground">
          Parsers de Galicia, Mercado Pago y tarjeta (F5.2): todo entra como "pendiente de revisar" a la bandeja de
          movimientos bancarios — nada impacta la contabilidad automáticamente.
        </p>
      </div>

      <Card>
        <CardHeader><CardTitle>1. Elegir origen, cuenta y archivo</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-3">
            <label className="flex flex-col text-xs text-muted-foreground">
              Origen
              <select value={origen} onChange={(e) => { setOrigen(e.target.value as OrigenConParser); setFilas([]); setResultados(null) }} className={selectClase}>
                {ORIGENES.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </label>
            <label className="flex flex-col text-xs text-muted-foreground">
              Cuenta bancaria destino
              <select value={cuentaBancariaId} onChange={(e) => setCuentaBancariaId(e.target.value)} disabled={cuentasBancarias.isLoading} className={selectClase}>
                <option value="">Seleccionar…</option>
                {cuentasBancarias.data?.content?.map((c) => <option key={c.id} value={c.id.toString()}>{c.alias}</option>)}
              </select>
            </label>
            <label className="flex flex-col text-xs text-muted-foreground">
              Archivo
              <input ref={inputArchivoRef} type="file" accept={origenSeleccionado.accept} className="text-sm" />
            </label>
          </div>
          <Button onClick={onPrevisualizar} disabled={!cuentaBancariaId || previsualizar.isPending}>
            {previsualizar.isPending ? "Leyendo…" : "Previsualizar"}
          </Button>
        </CardContent>
      </Card>

      {filas.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>
              2. Revisar ({cantidadAImportar} a importar{cantidadDuplicados > 0 ? `, ${cantidadDuplicados} ya importados` : ""})
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {hayFilasEnUsd && (
              <label className="flex max-w-56 flex-col text-xs text-muted-foreground">
                Tipo de cambio para filas en USD
                <Input type="number" step="0.0001" value={tipoCambioUsd} onChange={(e) => setTipoCambioUsd(e.target.value)} className={inputClase} />
              </label>
            )}
            {hayFechasFaltantes && (
              <p className="rounded-md bg-amber-500/10 px-3 py-2 text-xs text-amber-700">
                ⚠ Algunas filas no traen fecha (el archivo no la declara). Podés completarla acá, o dejarla en blanco y
                terminar de completarla después en la bandeja de movimientos bancarios.
              </p>
            )}
            {origen === "TARJETA_CREDITO" && (
              <p className="rounded-md bg-muted px-3 py-2 text-xs text-muted-foreground">
                El resumen de tarjeta trae tanto los pagos/devoluciones que mueven la cuenta bancaria como el detalle de
                consumos del período. Desmarcá las filas que no correspondan importar acá (ej. si el detalle de
                consumos se va a llevar por otro lado).
              </p>
            )}
            <div className="overflow-x-auto">
              <table className="w-full min-w-[800px] text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr className="border-b border-border">
                    <th className="py-2 pr-4 font-medium">Importar</th>
                    <th className="py-2 pr-4 font-medium">Fecha</th>
                    <th className="py-2 pr-4 font-medium">Descripción</th>
                    <th className="py-2 pr-4 font-medium">Importe</th>
                    <th className="py-2 pr-4 font-medium">Referencia</th>
                    <th className="py-2 pr-4 font-medium">Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {filas.map((f) => (
                    <tr key={f.hash} className={`border-b border-border last:border-0 ${f.duplicado ? "opacity-60" : ""}`}>
                      <td className="py-2 pr-4">
                        <input
                          type="checkbox"
                          checked={f.incluir && !f.duplicado}
                          disabled={f.duplicado}
                          onChange={(e) => alternarIncluir(f.hash, e.target.checked)}
                        />
                      </td>
                      <td className="py-2 pr-4">
                        <Input type="date" value={f.fechaEditada} onChange={(e) => actualizarFecha(f.hash, e.target.value)} className={`${inputClase} w-36`} />
                      </td>
                      <td className="py-2 pr-4">{f.descripcion}</td>
                      <td className={`py-2 pr-4 ${f.importe < 0 ? "text-destructive" : ""}`}>{f.importe.toFixed(2)} {f.monedaCodigo}</td>
                      <td className="py-2 pr-4">{f.referencia ?? "—"}</td>
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
            <Button
              onClick={onConfirmar}
              disabled={cantidadAImportar === 0 || confirmar.isPending || (hayFilasEnUsd && !tipoCambioUsd)}
            >
              {confirmar.isPending ? "Importando…" : `Confirmar importación (${cantidadAImportar})`}
            </Button>
          </CardContent>
        </Card>
      )}

      {resultados && (
        <Card>
          <CardHeader><CardTitle>3. Resultado</CardTitle></CardHeader>
          <CardContent>
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground">
                <tr className="border-b border-border">
                  <th className="py-2 pr-4 font-medium">Descripción</th>
                  <th className="py-2 pr-4 font-medium">Resultado</th>
                </tr>
              </thead>
              <tbody>
                {resultados.map((r, i) => (
                  <tr key={i} className="border-b border-border last:border-0">
                    <td className="py-2 pr-4">{r.descripcion}</td>
                    <td className="py-2 pr-4">
                      {r.resultado === "IMPORTADO" && <span className="text-primary">Importado (movimiento #{r.movimientoBancarioId})</span>}
                      {r.resultado === "DUPLICADO" && <span className="text-muted-foreground">Ya importado anteriormente</span>}
                      {r.resultado === "ERROR" && <span className="text-destructive">Error: {r.motivoError}</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

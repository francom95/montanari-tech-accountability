import { useMemo, useRef, useState } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { useClientes } from "@/hooks/use-cliente"
import {
  descargarRechazosImportacionExcel,
  useConfirmarImportacionFacturas,
  usePrevisualizarFacturasPdf,
} from "@/hooks/use-importacion-factura"
import { useJurisdiccions } from "@/hooks/use-jurisdiccion"
import { useMonedas } from "@/hooks/use-monedas"
import { useProveedores } from "@/hooks/use-proveedor"
import { useProyectos } from "@/hooks/use-proyecto"
import { useTipoCostos } from "@/hooks/use-tipocosto"
import type {
  EstadoDestinoImportacion,
  FilaImportacionConfirmarInput,
  FilaImportacionPreview,
  FilaImportacionResultado,
  TipoDocumentoImportacion,
} from "@/types/importacion-factura"
import { TIPOS_COMPROBANTE, type TipoComprobante } from "@/types/factura-venta"

const ALICUOTAS = ["0", "2.5", "5", "10.5", "21", "27"]
const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"
const inputClase = "h-8"

type FilaEditable = {
  nombreArchivo: string
  tipo: TipoDocumentoImportacion
  clienteId: string
  proveedorId: string
  usarAltaRapida: boolean
  altaRapidaNombre: string
  altaRapidaCuit: string
  altaRapidaJurisdiccionId: string
  proyectoId: string
  fecha: string
  fechaVencimiento: string
  tipoComprobante: string
  puntoVenta: string
  numero: string
  monedaId: string
  tipoCambio: string
  observaciones: string
  descripcionLinea: string
  importeNeto: string
  alicuotaIva: string
  tipoIngreso: string
  tipoCostoId: string
  estadoDestino: EstadoDestinoImportacion
  cuitContraparte: string | null
  advertencias: string[]
  textoExtraido: string
}

function previewAEditable(p: FilaImportacionPreview): FilaEditable {
  const sinContraparte = p.tipoSugerido === "VENTA" ? p.clienteId == null : p.proveedorId == null
  return {
    nombreArchivo: p.nombreArchivo,
    tipo: p.tipoSugerido,
    clienteId: p.clienteId?.toString() ?? "",
    proveedorId: p.proveedorId?.toString() ?? "",
    usarAltaRapida: sinContraparte,
    altaRapidaNombre: "",
    altaRapidaCuit: p.cuitContraparte ?? "",
    altaRapidaJurisdiccionId: "",
    proyectoId: "",
    fecha: p.fecha ?? "",
    fechaVencimiento: "",
    tipoComprobante: p.tipoComprobante ?? "",
    puntoVenta: p.puntoVenta ?? "",
    numero: p.numero ?? "",
    monedaId: p.monedaId?.toString() ?? "",
    tipoCambio: p.tipoCambio?.toString() ?? "1",
    observaciones: "",
    descripcionLinea: `Importado de ${p.nombreArchivo}`,
    importeNeto: (p.netoGravado ?? p.total)?.toString() ?? "",
    alicuotaIva: p.alicuotaIva?.toString() ?? "0",
    tipoIngreso: "VENTA",
    tipoCostoId: "",
    estadoDestino: "BORRADOR",
    cuitContraparte: p.cuitContraparte,
    advertencias: p.advertencias,
    textoExtraido: p.textoExtraido,
  }
}

function editableAConfirmar(f: FilaEditable): FilaImportacionConfirmarInput {
  return {
    nombreArchivo: f.nombreArchivo,
    tipo: f.tipo,
    clienteId: f.tipo === "VENTA" && !f.usarAltaRapida && f.clienteId ? Number(f.clienteId) : undefined,
    proveedorId: f.tipo === "COMPRA" && !f.usarAltaRapida && f.proveedorId ? Number(f.proveedorId) : undefined,
    altaRapidaNombre: f.usarAltaRapida ? f.altaRapidaNombre : undefined,
    altaRapidaCuit: f.usarAltaRapida ? f.altaRapidaCuit : undefined,
    altaRapidaJurisdiccionId: f.usarAltaRapida && f.altaRapidaJurisdiccionId ? Number(f.altaRapidaJurisdiccionId) : undefined,
    proyectoId: f.proyectoId ? Number(f.proyectoId) : undefined,
    fecha: f.fecha,
    fechaVencimiento: f.fechaVencimiento || undefined,
    tipoComprobante: f.tipoComprobante as TipoComprobante,
    puntoVenta: f.puntoVenta || undefined,
    numero: f.numero,
    monedaId: Number(f.monedaId),
    tipoCambio: Number(f.tipoCambio),
    observaciones: f.observaciones || undefined,
    descripcionLinea: f.descripcionLinea,
    importeNeto: Number(f.importeNeto),
    alicuotaIva: Number(f.alicuotaIva),
    tipoIngreso: f.tipo === "VENTA" ? f.tipoIngreso : undefined,
    tipoCostoId: f.tipo === "COMPRA" && f.tipoCostoId ? Number(f.tipoCostoId) : undefined,
    estadoDestino: f.estadoDestino,
  }
}

export function ImportacionHistoricaPage() {
  const inputArchivosRef = useRef<HTMLInputElement>(null)
  const [filas, setFilas] = useState<FilaEditable[]>([])
  const [resultados, setResultados] = useState<FilaImportacionResultado[] | null>(null)

  const previsualizar = usePrevisualizarFacturasPdf()
  const confirmar = useConfirmarImportacionFacturas()

  const clientes = useClientes({ activo: true, page: 0, size: 200 })
  const proveedores = useProveedores({ activo: true, page: 0, size: 200 })
  const proyectos = useProyectos({ activo: true, page: 0, size: 200 })
  const jurisdicciones = useJurisdiccions({ page: 0, size: 100 })
  const monedas = useMonedas({ page: 0, size: 20 })
  const tiposCosto = useTipoCostos({ activo: true, page: 0, size: 200 })

  function actualizarFila(indice: number, cambios: Partial<FilaEditable>) {
    setFilas((actual) => actual.map((f, i) => (i === indice ? { ...f, ...cambios } : f)))
  }

  function quitarFila(indice: number) {
    setFilas((actual) => actual.filter((_, i) => i !== indice))
  }

  async function onPrevisualizar() {
    const archivos = inputArchivosRef.current?.files
    if (!archivos || archivos.length === 0) return
    setResultados(null)
    const previews = await previsualizar.mutateAsync(Array.from(archivos))
    setFilas(previews.map(previewAEditable))
    if (inputArchivosRef.current) inputArchivosRef.current.value = ""
  }

  async function onConfirmar() {
    const payload = filas.map(editableAConfirmar)
    const resultado = await confirmar.mutateAsync(payload)
    setResultados(resultado)
    // Deja en la lista solo las filas rechazadas, para poder corregirlas y reintentar.
    const numerosRechazados = new Set(resultado.filter((r) => !r.exito).map((r) => r.nombreArchivo))
    setFilas((actual) => actual.filter((f) => numerosRechazados.has(f.nombreArchivo)))
  }

  const rechazos = useMemo(() => (resultados ?? []).filter((r) => !r.exito), [resultados])

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-lg font-semibold text-foreground">Importación de facturación histórica</h1>
        <p className="text-sm text-muted-foreground">
          Extracción básica de PDF (F4.6): revisá y completá cada factura antes de confirmar — nada se carga automáticamente sin tu revisión.
        </p>
      </div>

      <Card>
        <CardHeader><CardTitle>1. Subir PDFs</CardTitle></CardHeader>
        <CardContent className="flex items-center gap-2">
          <input ref={inputArchivosRef} type="file" accept="application/pdf" multiple className="text-sm" />
          <Button onClick={onPrevisualizar} disabled={previsualizar.isPending}>
            {previsualizar.isPending ? "Leyendo…" : "Previsualizar"}
          </Button>
        </CardContent>
      </Card>

      {filas.length > 0 && (
        <Card>
          <CardHeader><CardTitle>2. Revisar y completar ({filas.length})</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            {filas.map((f, i) => (
              <FilaFacturaCard
                key={f.nombreArchivo + i}
                fila={f}
                onChange={(cambios) => actualizarFila(i, cambios)}
                onQuitar={() => quitarFila(i)}
                clientes={clientes.data?.content ?? []}
                proveedores={proveedores.data?.content ?? []}
                proyectos={proyectos.data?.content ?? []}
                jurisdicciones={jurisdicciones.data?.content ?? []}
                monedas={monedas.data?.content ?? []}
                tiposCosto={tiposCosto.data?.content ?? []}
              />
            ))}
            <Button onClick={onConfirmar} disabled={confirmar.isPending}>
              {confirmar.isPending ? "Confirmando…" : `Confirmar importación (${filas.length})`}
            </Button>
          </CardContent>
        </Card>
      )}

      {resultados && (
        <Card>
          <CardHeader><CardTitle>3. Resultado</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground">
                <tr className="border-b border-border">
                  <th className="py-2 pr-4 font-medium">Archivo</th>
                  <th className="py-2 pr-4 font-medium">Tipo</th>
                  <th className="py-2 pr-4 font-medium">Número</th>
                  <th className="py-2 pr-4 font-medium">Resultado</th>
                </tr>
              </thead>
              <tbody>
                {resultados.map((r, i) => (
                  <tr key={i} className="border-b border-border last:border-0">
                    <td className="py-2 pr-4">{r.nombreArchivo}</td>
                    <td className="py-2 pr-4">{r.tipo}</td>
                    <td className="py-2 pr-4">{r.numero}</td>
                    <td className="py-2 pr-4">
                      {r.exito ? (
                        <span className="text-primary">
                          {r.estadoFinal === "CONFIRMADO" ? "Confirmada" : "Creada como borrador"} (factura #{r.facturaId})
                          {r.advertencia && <span className="block text-xs text-amber-600">{r.advertencia}</span>}
                        </span>
                      ) : (
                        <span className="text-destructive">Rechazada: {r.motivoRechazo}</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {rechazos.length > 0 && (
              <Button variant="outline" size="sm" onClick={() => descargarRechazosImportacionExcel(rechazos)}>
                Descargar rechazos ({rechazos.length})
              </Button>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  )
}

function FilaFacturaCard({ fila, onChange, onQuitar, clientes, proveedores, proyectos, jurisdicciones, monedas, tiposCosto }: {
  fila: FilaEditable
  onChange: (cambios: Partial<FilaEditable>) => void
  onQuitar: () => void
  clientes: { id: number; nombre: string }[]
  proveedores: { id: number; nombre: string }[]
  proyectos: { id: number; nombre: string }[]
  jurisdicciones: { id: number; nombre: string }[]
  monedas: { id: number; codigo: string }[]
  tiposCosto: { id: number; nombre: string }[]
}) {
  const esVenta = fila.tipo === "VENTA"

  return (
    <div className="space-y-3 rounded-lg border border-border p-4">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium">{fila.nombreArchivo}</span>
        <Button variant="outline" size="sm" onClick={onQuitar}>Quitar</Button>
      </div>

      {fila.advertencias.length > 0 && (
        <ul className="rounded-md bg-amber-500/10 px-3 py-2 text-xs text-amber-700">
          {fila.advertencias.map((a, i) => <li key={i}>⚠ {a}</li>)}
        </ul>
      )}

      <div className="grid gap-3 sm:grid-cols-4">
        <label className="flex flex-col text-xs text-muted-foreground">
          Tipo
          <select value={fila.tipo} onChange={(e) => onChange({ tipo: e.target.value as TipoDocumentoImportacion })} className={selectClase}>
            <option value="VENTA">Venta</option>
            <option value="COMPRA">Compra</option>
          </select>
        </label>
        <label className="flex flex-col text-xs text-muted-foreground">
          Tipo de comprobante
          <select value={fila.tipoComprobante} onChange={(e) => onChange({ tipoComprobante: e.target.value })} className={selectClase}>
            <option value="">Seleccionar…</option>
            {TIPOS_COMPROBANTE.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
        </label>
        <label className="flex flex-col text-xs text-muted-foreground">
          Punto de venta
          <Input value={fila.puntoVenta} onChange={(e) => onChange({ puntoVenta: e.target.value })} className={inputClase} />
        </label>
        <label className="flex flex-col text-xs text-muted-foreground">
          Número
          <Input value={fila.numero} onChange={(e) => onChange({ numero: e.target.value })} className={inputClase} />
        </label>
      </div>

      <div className="grid gap-3 sm:grid-cols-4">
        <label className="flex flex-col text-xs text-muted-foreground">
          Fecha
          <Input type="date" value={fila.fecha} onChange={(e) => onChange({ fecha: e.target.value })} className={inputClase} />
        </label>
        <label className="flex flex-col text-xs text-muted-foreground">
          Vencimiento
          <Input type="date" value={fila.fechaVencimiento} onChange={(e) => onChange({ fechaVencimiento: e.target.value })} className={inputClase} />
        </label>
        <label className="flex flex-col text-xs text-muted-foreground">
          Proyecto
          <select value={fila.proyectoId} onChange={(e) => onChange({ proyectoId: e.target.value })} className={selectClase}>
            <option value="">Sin proyecto</option>
            {proyectos.map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
          </select>
        </label>
        <label className="flex flex-col text-xs text-muted-foreground">
          Destino
          <select value={fila.estadoDestino} onChange={(e) => onChange({ estadoDestino: e.target.value as EstadoDestinoImportacion })} className={selectClase}>
            <option value="BORRADOR">Borrador</option>
            <option value="CONFIRMADO">Confirmado (genera asiento)</option>
          </select>
        </label>
      </div>

      <div className="grid gap-3 sm:grid-cols-4">
        {!fila.usarAltaRapida ? (
          <label className="flex flex-col text-xs text-muted-foreground sm:col-span-2">
            {esVenta ? "Cliente" : "Proveedor"}
            <div className="flex gap-2">
              <select
                value={esVenta ? fila.clienteId : fila.proveedorId}
                onChange={(e) => onChange(esVenta ? { clienteId: e.target.value } : { proveedorId: e.target.value })}
                className={selectClase}
              >
                <option value="">Seleccionar…</option>
                {(esVenta ? clientes : proveedores).map((c) => <option key={c.id} value={c.id.toString()}>{c.nombre}</option>)}
              </select>
              <Button type="button" variant="outline" size="sm" onClick={() => onChange({ usarAltaRapida: true })}>Nuevo</Button>
            </div>
          </label>
        ) : (
          <>
            <label className="flex flex-col text-xs text-muted-foreground">
              Nombre (alta rápida)
              <div className="flex gap-2">
                <Input value={fila.altaRapidaNombre} onChange={(e) => onChange({ altaRapidaNombre: e.target.value })} className={inputClase} />
                <Button type="button" variant="outline" size="sm" onClick={() => onChange({ usarAltaRapida: false })}>Ya existe</Button>
              </div>
            </label>
            <label className="flex flex-col text-xs text-muted-foreground">
              CUIT
              <Input value={fila.altaRapidaCuit} onChange={(e) => onChange({ altaRapidaCuit: e.target.value })} placeholder="XX-XXXXXXXX-X" className={inputClase} />
            </label>
            <label className="flex flex-col text-xs text-muted-foreground">
              Jurisdicción
              <select value={fila.altaRapidaJurisdiccionId} onChange={(e) => onChange({ altaRapidaJurisdiccionId: e.target.value })} className={selectClase}>
                <option value="">Seleccionar…</option>
                {jurisdicciones.map((j) => <option key={j.id} value={j.id.toString()}>{j.nombre}</option>)}
              </select>
            </label>
          </>
        )}
        <label className="flex flex-col text-xs text-muted-foreground">
          Moneda
          <select value={fila.monedaId} onChange={(e) => onChange({ monedaId: e.target.value })} className={selectClase}>
            <option value="">Seleccionar…</option>
            {monedas.map((m) => <option key={m.id} value={m.id.toString()}>{m.codigo}</option>)}
          </select>
        </label>
      </div>

      <div className="grid gap-3 sm:grid-cols-4">
        <label className="flex flex-col text-xs text-muted-foreground">
          Tipo de cambio
          <Input type="number" step="0.000001" value={fila.tipoCambio} onChange={(e) => onChange({ tipoCambio: e.target.value })} className={inputClase} />
        </label>
        <label className="flex flex-col text-xs text-muted-foreground sm:col-span-2">
          Descripción
          <Input value={fila.descripcionLinea} onChange={(e) => onChange({ descripcionLinea: e.target.value })} className={inputClase} />
        </label>
        {esVenta ? (
          <label className="flex flex-col text-xs text-muted-foreground">
            Tipo de ingreso
            <select value={fila.tipoIngreso} onChange={(e) => onChange({ tipoIngreso: e.target.value })} className={selectClase}>
              <option value="VENTA">Venta</option>
              <option value="OTRA_VENTA">Otra venta</option>
            </select>
          </label>
        ) : (
          <label className="flex flex-col text-xs text-muted-foreground">
            Tipo de costo
            <select value={fila.tipoCostoId} onChange={(e) => onChange({ tipoCostoId: e.target.value })} className={selectClase}>
              <option value="">Seleccionar…</option>
              {tiposCosto.map((t) => <option key={t.id} value={t.id.toString()}>{t.nombre}</option>)}
            </select>
          </label>
        )}
      </div>

      <div className="grid gap-3 sm:grid-cols-4">
        <label className="flex flex-col text-xs text-muted-foreground">
          Importe neto
          <Input type="number" step="0.01" value={fila.importeNeto} onChange={(e) => onChange({ importeNeto: e.target.value })} className={inputClase} />
        </label>
        <label className="flex flex-col text-xs text-muted-foreground">
          Alícuota IVA
          <select value={fila.alicuotaIva} onChange={(e) => onChange({ alicuotaIva: e.target.value })} className={selectClase}>
            {ALICUOTAS.map((a) => <option key={a} value={a}>{a}%</option>)}
          </select>
        </label>
        <label className="flex flex-col text-xs text-muted-foreground sm:col-span-2">
          Observaciones
          <Input value={fila.observaciones} onChange={(e) => onChange({ observaciones: e.target.value })} className={inputClase} />
        </label>
      </div>
    </div>
  )
}

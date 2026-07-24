import { zodResolver } from "@hookform/resolvers/zod"
import { useMemo, useState } from "react"
import { useForm, useWatch } from "react-hook-form"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useCuentasContables } from "@/hooks/use-cuenta-contable"
import { useConceptos } from "@/hooks/use-concepto"
import { useMonedas } from "@/hooks/use-monedas"
import { useProveedores } from "@/hooks/use-proveedor"
import { useProyectos } from "@/hooks/use-proyecto"
import { useTarjetasCredito } from "@/hooks/use-tarjeta-credito"
import {
  useCancelarVencimiento,
  useCrearVencimiento,
  useEditarVencimiento,
  useGenerarAutomaticosVencimientos,
  useMarcarPagadoVencimiento,
  useReprogramarVencimiento,
  useVencimientos,
} from "@/hooks/use-vencimiento"
import {
  ESTADOS_VENCIMIENTO_FILTRO,
  RECURRENCIAS,
  TIPOS_VENCIMIENTO,
  type EstadoVencimiento,
  type EstadoVencimientoFiltro,
  type TipoRecurrencia,
  type TipoVencimiento,
  type Vencimiento,
} from "@/types/vencimiento"

const TIPO_LABEL: Record<TipoVencimiento, string> = {
  IVA: "IVA", IIBB: "IIBB", GANANCIAS: "Ganancias", BIENES_PERSONALES: "Bienes personales",
  CARGAS_SOCIALES: "Cargas sociales", SUELDOS: "Sueldos", CONTADOR: "Contador", TARJETA: "Tarjeta de crédito",
  SUSCRIPCION: "Suscripción", PRESTAMO: "Préstamo", PLAN_DE_PAGO: "Plan de pago",
  PAGO_AUTOMATICO: "Pago automático", OTRO: "Otro",
}

const RECURRENCIA_LABEL: Record<TipoRecurrencia, string> = {
  UNICA: "Única", MENSUAL: "Mensual", ANUAL: "Anual", PERSONALIZADA: "Personalizada",
}

const ESTADO_LABEL: Record<EstadoVencimiento, string> = {
  PENDIENTE: "Pendiente", VENCIDO: "Vencido", PAGADO: "Pagado", REPROGRAMADO: "Reprogramado", CANCELADO: "Cancelado",
}

const ESTADO_CLASE: Record<EstadoVencimiento, string> = {
  PENDIENTE: "text-blue-600",
  VENCIDO: "text-destructive font-medium",
  PAGADO: "text-green-600",
  REPROGRAMADO: "text-amber-600",
  CANCELADO: "text-muted-foreground line-through",
}

const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"
const inputClase = "h-8"

const esquema = z.object({
  descripcion: z.string().min(1, "La descripción es obligatoria"),
  tipo: z.string().min(1, "El tipo es obligatorio"),
  fecha: z.string().min(1, "La fecha es obligatoria"),
  importeEstimado: z.string().optional(),
  monedaId: z.string().min(1, "La moneda es obligatoria"),
  recurrencia: z.string().min(1),
  intervaloDiasPersonalizado: z.string().optional(),
  cuentaContableId: z.string().optional(),
  proveedorId: z.string().optional(),
  tarjetaCreditoId: z.string().optional(),
  proyectoId: z.string().optional(),
  conceptoRecurrenteId: z.string().optional(),
  observaciones: z.string().optional(),
})

type Valores = z.infer<typeof esquema>

const VACIO: Valores = {
  descripcion: "", tipo: "OTRO", fecha: "", importeEstimado: "", monedaId: "", recurrencia: "UNICA",
  intervaloDiasPersonalizado: "", cuentaContableId: "", proveedorId: "", tarjetaCreditoId: "", proyectoId: "",
  conceptoRecurrenteId: "", observaciones: "",
}

function vencimientoAValores(v: Vencimiento): Valores {
  return {
    descripcion: v.descripcion,
    tipo: v.tipo,
    fecha: v.fecha,
    importeEstimado: v.importeEstimado?.toString() ?? "",
    monedaId: v.monedaId.toString(),
    recurrencia: v.recurrencia,
    intervaloDiasPersonalizado: v.intervaloDiasPersonalizado?.toString() ?? "",
    cuentaContableId: v.cuentaContableId?.toString() ?? "",
    proveedorId: v.proveedorId?.toString() ?? "",
    tarjetaCreditoId: v.tarjetaCreditoId?.toString() ?? "",
    proyectoId: v.proyectoId?.toString() ?? "",
    conceptoRecurrenteId: v.conceptoRecurrenteId?.toString() ?? "",
    observaciones: v.observaciones ?? "",
  }
}

function aFechaIso(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, "0")
  const dia = String(d.getDate()).padStart(2, "0")
  return `${y}-${m}-${dia}`
}

function generarDiasCalendario(anio: number, mes: number): Date[] {
  const primerDiaMes = new Date(anio, mes - 1, 1)
  const ultimoDiaMes = new Date(anio, mes, 0)
  const diaSemanaPrimero = (primerDiaMes.getDay() + 6) % 7
  const inicio = new Date(primerDiaMes)
  inicio.setDate(inicio.getDate() - diaSemanaPrimero)
  const totalCeldas = Math.ceil((diaSemanaPrimero + ultimoDiaMes.getDate()) / 7) * 7
  return Array.from({ length: totalCeldas }, (_, i) => {
    const d = new Date(inicio)
    d.setDate(inicio.getDate() + i)
    return d
  })
}

const NOMBRES_MES = [
  "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
]
const NOMBRES_DIA = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"]

export function VencimientosPage() {
  const hoy = new Date()
  const [vista, setVista] = useState<"calendario" | "lista">("calendario")
  const [anioMes, setAnioMes] = useState({ anio: hoy.getFullYear(), mes: hoy.getMonth() + 1 })
  const [tipoFiltro, setTipoFiltro] = useState<TipoVencimiento | "">("")
  const [estadoFiltro, setEstadoFiltro] = useState<EstadoVencimientoFiltro | "">("")
  const [fechaDesdeFiltro, setFechaDesdeFiltro] = useState("")
  const [fechaHastaFiltro, setFechaHastaFiltro] = useState("")
  const [editando, setEditando] = useState<Vencimiento | null>(null)
  const [mostrarForm, setMostrarForm] = useState(false)
  const [seleccionado, setSeleccionado] = useState<Vencimiento | null>(null)
  const [reprogramando, setReprogramando] = useState<number | null>(null)
  const [nuevaFechaReprogramar, setNuevaFechaReprogramar] = useState("")
  const [cancelando, setCancelando] = useState<number | null>(null)
  const [motivoCancelacion, setMotivoCancelacion] = useState("")

  const diasCalendario = useMemo(() => generarDiasCalendario(anioMes.anio, anioMes.mes), [anioMes])
  const primerDiaVisible = aFechaIso(diasCalendario[0])
  const ultimoDiaVisible = aFechaIso(diasCalendario[diasCalendario.length - 1])

  const query = useVencimientos({
    tipo: tipoFiltro || undefined,
    estado: estadoFiltro || undefined,
    fechaDesde: vista === "calendario" ? primerDiaVisible : fechaDesdeFiltro || undefined,
    fechaHasta: vista === "calendario" ? ultimoDiaVisible : fechaHastaFiltro || undefined,
    page: 0,
    size: 500,
  })

  const crear = useCrearVencimiento()
  const editar = useEditarVencimiento()
  const marcarPagado = useMarcarPagadoVencimiento()
  const reprogramar = useReprogramarVencimiento()
  const cancelar = useCancelarVencimiento()
  const generarAutomaticos = useGenerarAutomaticosVencimientos()

  const monedas = useMonedas({ activo: true, page: 0, size: 20 })
  const cuentasContables = useCuentasContables({ activo: true, page: 0, size: 500 })
  const proveedores = useProveedores({ activo: true, page: 0, size: 200 })
  const tarjetas = useTarjetasCredito({ activo: true, page: 0, size: 100 })
  const proyectos = useProyectos({ activo: true, page: 0, size: 200 })
  const conceptos = useConceptos({ activo: true, page: 0, size: 200 })

  const form = useForm<Valores>({ resolver: zodResolver(esquema), defaultValues: VACIO })
  const recurrenciaObservada = useWatch({ control: form.control, name: "recurrencia" })

  function nuevoVencimiento(fechaSugerida?: string) {
    setEditando(null)
    form.reset({ ...VACIO, fecha: fechaSugerida ?? "" })
    setMostrarForm(true)
    setSeleccionado(null)
  }

  function iniciarEdicion(v: Vencimiento) {
    setEditando(v)
    form.reset(vencimientoAValores(v))
    setMostrarForm(true)
  }

  function cancelarForm() {
    setMostrarForm(false)
    setEditando(null)
  }

  function onSubmit(valores: Valores) {
    const payload = {
      descripcion: valores.descripcion,
      tipo: valores.tipo as TipoVencimiento,
      fecha: valores.fecha,
      importeEstimado: valores.importeEstimado ? Number(valores.importeEstimado) : undefined,
      monedaId: Number(valores.monedaId),
      recurrencia: valores.recurrencia as TipoRecurrencia,
      intervaloDiasPersonalizado: valores.intervaloDiasPersonalizado ? Number(valores.intervaloDiasPersonalizado) : undefined,
      cuentaContableId: valores.cuentaContableId ? Number(valores.cuentaContableId) : undefined,
      proveedorId: valores.proveedorId ? Number(valores.proveedorId) : undefined,
      tarjetaCreditoId: valores.tarjetaCreditoId ? Number(valores.tarjetaCreditoId) : undefined,
      proyectoId: valores.proyectoId ? Number(valores.proyectoId) : undefined,
      conceptoRecurrenteId: valores.conceptoRecurrenteId ? Number(valores.conceptoRecurrenteId) : undefined,
      observaciones: valores.observaciones || undefined,
    }
    if (editando) {
      editar.mutate({ id: editando.id, valores: payload }, { onSuccess: cancelarForm })
    } else {
      crear.mutate(payload, { onSuccess: cancelarForm })
    }
  }

  function confirmarReprogramacion(id: number) {
    if (!nuevaFechaReprogramar) return
    reprogramar.mutate(
      { id, nuevaFecha: nuevaFechaReprogramar },
      { onSuccess: () => { setReprogramando(null); setNuevaFechaReprogramar(""); setSeleccionado(null) } }
    )
  }

  function confirmarCancelacion(id: number) {
    if (!motivoCancelacion.trim()) return
    cancelar.mutate(
      { id, motivo: motivoCancelacion },
      { onSuccess: () => { setCancelando(null); setMotivoCancelacion(""); setSeleccionado(null) } }
    )
  }

  const vencimientos = query.data?.content ?? []
  const vencimientosPorFecha = useMemo(() => {
    const mapa = new Map<string, Vencimiento[]>()
    for (const v of query.data?.content ?? []) {
      const lista = mapa.get(v.fecha) ?? []
      lista.push(v)
      mapa.set(v.fecha, lista)
    }
    return mapa
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query.data])

  function accionesDisponibles(v: Vencimiento) {
    const puedeEditar = v.estado === "PENDIENTE" || v.estado === "REPROGRAMADO"
    const puedeResolver = v.estado !== "PAGADO" && v.estado !== "CANCELADO"
    return { puedeEditar, puedeResolver }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-semibold text-foreground">Calendario de vencimientos</h1>
          <p className="text-sm text-muted-foreground">
            Obligaciones de pago futuras: impuestos, tarjeta, sueldos, suscripciones y demás compromisos (F8.1).
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" disabled={generarAutomaticos.isPending} onClick={() => generarAutomaticos.mutate()}>
            {generarAutomaticos.isPending ? "Generando…" : "Generar ahora"}
          </Button>
          {!mostrarForm && <Button onClick={() => nuevoVencimiento()}>Nuevo vencimiento</Button>}
        </div>
      </div>

      {generarAutomaticos.data && (
        <p className="text-sm text-muted-foreground">
          Generación automática: {generarAutomaticos.data.total} vencimiento(s) nuevo(s)
          {generarAutomaticos.data.generados.filter((g) => g.cantidad > 0).length > 0 && (
            <> ({generarAutomaticos.data.generados.filter((g) => g.cantidad > 0).map((g) => `${g.origen}: ${g.cantidad}`).join(", ")})</>
          )}.
        </p>
      )}

      {mostrarForm && (
        <Card>
          <CardHeader>
            <CardTitle>{editando ? `Editar vencimiento #${editando.id}` : "Nuevo vencimiento"}</CardTitle>
          </CardHeader>
          <CardContent>
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <div className="grid gap-4 sm:grid-cols-3">
                  <FormField control={form.control} name="descripcion" render={({ field }) => (
                    <FormItem><FormLabel>Descripción</FormLabel><FormControl><Input {...field} className={inputClase} /></FormControl><FormMessage /></FormItem>
                  )} />
                  <FormField control={form.control} name="tipo" render={({ field }) => (
                    <FormItem>
                      <FormLabel>Tipo</FormLabel>
                      <FormControl>
                        <select {...field} className={selectClase}>
                          {TIPOS_VENCIMIENTO.map((t) => <option key={t} value={t}>{TIPO_LABEL[t]}</option>)}
                        </select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )} />
                  <FormField control={form.control} name="fecha" render={({ field }) => (
                    <FormItem><FormLabel>Fecha</FormLabel><FormControl><Input {...field} type="date" className={inputClase} /></FormControl><FormMessage /></FormItem>
                  )} />
                </div>
                <div className="grid gap-4 sm:grid-cols-3">
                  <FormField control={form.control} name="importeEstimado" render={({ field }) => (
                    <FormItem><FormLabel>Importe estimado</FormLabel><FormControl><Input {...field} type="number" step="0.01" className={inputClase} /></FormControl><FormMessage /></FormItem>
                  )} />
                  <FormField control={form.control} name="monedaId" render={({ field }) => (
                    <FormItem>
                      <FormLabel>Moneda</FormLabel>
                      <FormControl>
                        <select {...field} disabled={monedas.isLoading} className={selectClase}>
                          <option value="">Seleccionar…</option>
                          {monedas.data?.content?.map((m) => <option key={m.id} value={m.id.toString()}>{m.codigo}</option>)}
                        </select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )} />
                  <FormField control={form.control} name="recurrencia" render={({ field }) => (
                    <FormItem>
                      <FormLabel>Recurrencia</FormLabel>
                      <FormControl>
                        <select {...field} className={selectClase}>
                          {RECURRENCIAS.map((r) => <option key={r} value={r}>{RECURRENCIA_LABEL[r]}</option>)}
                        </select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )} />
                </div>
                {recurrenciaObservada === "PERSONALIZADA" && (
                  <FormField control={form.control} name="intervaloDiasPersonalizado" render={({ field }) => (
                    <FormItem className="max-w-xs"><FormLabel>Intervalo (días)</FormLabel><FormControl><Input {...field} type="number" className={inputClase} /></FormControl><FormMessage /></FormItem>
                  )} />
                )}
                <div className="grid gap-4 sm:grid-cols-3">
                  <FormField control={form.control} name="cuentaContableId" render={({ field }) => (
                    <FormItem>
                      <FormLabel>Cuenta contable (opcional)</FormLabel>
                      <FormControl>
                        <select {...field} disabled={cuentasContables.isLoading} className={selectClase}>
                          <option value="">—</option>
                          {cuentasContables.data?.content?.map((c) => <option key={c.id} value={c.id.toString()}>{c.codigo} — {c.nombre}</option>)}
                        </select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )} />
                  <FormField control={form.control} name="proveedorId" render={({ field }) => (
                    <FormItem>
                      <FormLabel>Proveedor (opcional)</FormLabel>
                      <FormControl>
                        <select {...field} disabled={proveedores.isLoading} className={selectClase}>
                          <option value="">—</option>
                          {proveedores.data?.content?.map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
                        </select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )} />
                  <FormField control={form.control} name="tarjetaCreditoId" render={({ field }) => (
                    <FormItem>
                      <FormLabel>Tarjeta de crédito (opcional)</FormLabel>
                      <FormControl>
                        <select {...field} disabled={tarjetas.isLoading} className={selectClase}>
                          <option value="">—</option>
                          {tarjetas.data?.content?.map((t) => <option key={t.id} value={t.id.toString()}>{t.entidad}</option>)}
                        </select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )} />
                </div>
                <div className="grid gap-4 sm:grid-cols-3">
                  <FormField control={form.control} name="proyectoId" render={({ field }) => (
                    <FormItem>
                      <FormLabel>Proyecto (opcional)</FormLabel>
                      <FormControl>
                        <select {...field} disabled={proyectos.isLoading} className={selectClase}>
                          <option value="">—</option>
                          {proyectos.data?.content?.map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
                        </select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )} />
                  <FormField control={form.control} name="conceptoRecurrenteId" render={({ field }) => (
                    <FormItem>
                      <FormLabel>Concepto recurrente (opcional)</FormLabel>
                      <FormControl>
                        <select {...field} disabled={conceptos.isLoading} className={selectClase}>
                          <option value="">—</option>
                          {conceptos.data?.content?.map((c) => <option key={c.id} value={c.id.toString()}>{c.nombre}</option>)}
                        </select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )} />
                  <FormField control={form.control} name="observaciones" render={({ field }) => (
                    <FormItem><FormLabel>Observaciones</FormLabel><FormControl><Input {...field} className={inputClase} /></FormControl><FormMessage /></FormItem>
                  )} />
                </div>
                <div className="flex gap-2">
                  <Button type="submit" disabled={crear.isPending || editar.isPending}>{editando ? "Guardar" : "Crear"}</Button>
                  <Button type="button" variant="outline" onClick={cancelarForm}>Cancelar</Button>
                </div>
              </form>
            </Form>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader className="flex flex-row items-center justify-between gap-2">
          <CardTitle>{vista === "calendario" ? "Calendario" : "Listado"}</CardTitle>
          <div className="flex gap-2">
            <Button variant={vista === "calendario" ? "default" : "outline"} size="sm" onClick={() => setVista("calendario")}>Calendario</Button>
            <Button variant={vista === "lista" ? "default" : "outline"} size="sm" onClick={() => setVista("lista")}>Lista</Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap items-center gap-2">
            <select value={tipoFiltro} onChange={(e) => setTipoFiltro(e.target.value as TipoVencimiento | "")} className={`${selectClase} max-w-48`}>
              <option value="">Todos los tipos</option>
              {TIPOS_VENCIMIENTO.map((t) => <option key={t} value={t}>{TIPO_LABEL[t]}</option>)}
            </select>
            <select value={estadoFiltro} onChange={(e) => setEstadoFiltro(e.target.value as EstadoVencimientoFiltro | "")} className={`${selectClase} max-w-40`}>
              <option value="">Todos los estados</option>
              {ESTADOS_VENCIMIENTO_FILTRO.map((e) => <option key={e} value={e}>{ESTADO_LABEL[e]}</option>)}
            </select>
            {vista === "lista" && (
              <>
                <Input type="date" value={fechaDesdeFiltro} onChange={(e) => setFechaDesdeFiltro(e.target.value)} className={`${inputClase} w-36`} placeholder="Desde" />
                <Input type="date" value={fechaHastaFiltro} onChange={(e) => setFechaHastaFiltro(e.target.value)} className={`${inputClase} w-36`} placeholder="Hasta" />
              </>
            )}
          </div>

          {query.isLoading ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : vista === "calendario" ? (
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Button variant="outline" size="sm" onClick={() => setAnioMes((am) => am.mes === 1 ? { anio: am.anio - 1, mes: 12 } : { anio: am.anio, mes: am.mes - 1 })}>Anterior</Button>
                <span className="text-sm font-medium">{NOMBRES_MES[anioMes.mes - 1]} {anioMes.anio}</span>
                <Button variant="outline" size="sm" onClick={() => setAnioMes((am) => am.mes === 12 ? { anio: am.anio + 1, mes: 1 } : { anio: am.anio, mes: am.mes + 1 })}>Siguiente</Button>
              </div>
              <div className="grid grid-cols-7 gap-1 text-xs">
                {NOMBRES_DIA.map((n) => <div key={n} className="px-1 py-1 text-center font-medium text-muted-foreground">{n}</div>)}
                {diasCalendario.map((dia) => {
                  const iso = aFechaIso(dia)
                  const delMes = dia.getMonth() === anioMes.mes - 1
                  const vencimientosDelDia = vencimientosPorFecha.get(iso) ?? []
                  return (
                    <div
                      key={iso}
                      className={`min-h-24 rounded-md border border-border p-1 ${delMes ? "bg-background" : "bg-muted/40 text-muted-foreground"}`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="text-xs">{dia.getDate()}</span>
                        {delMes && (
                          <button type="button" className="text-xs text-muted-foreground hover:text-foreground" onClick={() => nuevoVencimiento(iso)}>+</button>
                        )}
                      </div>
                      <div className="mt-1 space-y-0.5">
                        {vencimientosDelDia.map((v) => (
                          <button
                            key={v.id}
                            type="button"
                            onClick={() => setSeleccionado(v)}
                            className={`block w-full truncate rounded px-1 py-0.5 text-left text-[11px] hover:bg-muted ${ESTADO_CLASE[v.estado]}`}
                            title={v.descripcion}
                          >
                            {v.descripcion}
                          </button>
                        ))}
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[900px] text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr className="border-b border-border">
                    <th className="py-2 pr-4 font-medium">Fecha</th>
                    <th className="py-2 pr-4 font-medium">Descripción</th>
                    <th className="py-2 pr-4 font-medium">Tipo</th>
                    <th className="py-2 pr-4 font-medium">Importe estimado</th>
                    <th className="py-2 pr-4 font-medium">Estado</th>
                    <th className="py-2 pr-4 font-medium">Recurrencia</th>
                    <th className="py-2 pr-4 font-medium">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {vencimientos.map((v) => {
                    const { puedeEditar, puedeResolver } = accionesDisponibles(v)
                    return (
                      <tr key={v.id} className="border-b border-border last:border-0 align-top">
                        <td className="py-2 pr-4">{v.fecha}</td>
                        <td className="py-2 pr-4">{v.descripcion}</td>
                        <td className="py-2 pr-4">{TIPO_LABEL[v.tipo]}</td>
                        <td className="py-2 pr-4">{v.importeEstimado != null ? `${v.importeEstimado.toFixed(2)} ${v.monedaCodigo}` : "—"}</td>
                        <td className={`py-2 pr-4 ${ESTADO_CLASE[v.estado]}`}>{ESTADO_LABEL[v.estado]}</td>
                        <td className="py-2 pr-4">{RECURRENCIA_LABEL[v.recurrencia]}</td>
                        <td className="py-2 pr-4">
                          {reprogramando === v.id ? (
                            <div className="flex items-center gap-2">
                              <Input type="date" autoFocus value={nuevaFechaReprogramar} onChange={(e) => setNuevaFechaReprogramar(e.target.value)} className={`${inputClase} w-36`} />
                              <Button size="sm" disabled={reprogramar.isPending || !nuevaFechaReprogramar} onClick={() => confirmarReprogramacion(v.id)}>Confirmar</Button>
                              <Button variant="outline" size="sm" onClick={() => setReprogramando(null)}>Cancelar</Button>
                            </div>
                          ) : cancelando === v.id ? (
                            <div className="flex items-center gap-2">
                              <Input autoFocus placeholder="Motivo…" value={motivoCancelacion} onChange={(e) => setMotivoCancelacion(e.target.value)} className={`${inputClase} w-40`} />
                              <Button size="sm" disabled={cancelar.isPending || !motivoCancelacion.trim()} onClick={() => confirmarCancelacion(v.id)}>Confirmar</Button>
                              <Button variant="outline" size="sm" onClick={() => setCancelando(null)}>Cancelar</Button>
                            </div>
                          ) : (
                            <div className="flex flex-wrap gap-2">
                              {puedeEditar && <Button variant="outline" size="sm" onClick={() => iniciarEdicion(v)}>Editar</Button>}
                              {puedeResolver && (
                                <>
                                  <Button variant="outline" size="sm" disabled={marcarPagado.isPending} onClick={() => marcarPagado.mutate({ id: v.id })}>Marcar pagado</Button>
                                  <Button variant="outline" size="sm" onClick={() => setReprogramando(v.id)}>Reprogramar</Button>
                                  <Button variant="outline" size="sm" onClick={() => setCancelando(v.id)}>Cancelar</Button>
                                </>
                              )}
                            </div>
                          )}
                        </td>
                      </tr>
                    )
                  })}
                  {vencimientos.length === 0 && (
                    <tr><td colSpan={7} className="py-4 text-center text-sm text-muted-foreground">No hay vencimientos para los filtros aplicados.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {seleccionado && (
        <Card>
          <CardHeader>
            <CardTitle>{seleccionado.descripcion}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            <p>Fecha: <strong>{seleccionado.fecha}</strong> · Tipo: <strong>{TIPO_LABEL[seleccionado.tipo]}</strong> · Estado: <strong className={ESTADO_CLASE[seleccionado.estado]}>{ESTADO_LABEL[seleccionado.estado]}</strong></p>
            {seleccionado.importeEstimado != null && <p>Importe estimado: <strong>{seleccionado.importeEstimado.toFixed(2)} {seleccionado.monedaCodigo}</strong></p>}
            {seleccionado.proveedorNombre && <p>Proveedor: {seleccionado.proveedorNombre}</p>}
            {seleccionado.tarjetaCreditoEntidad && <p>Tarjeta: {seleccionado.tarjetaCreditoEntidad}</p>}
            {seleccionado.proyectoNombre && <p>Proyecto: {seleccionado.proyectoNombre}</p>}
            {seleccionado.observaciones && <p>Observaciones: {seleccionado.observaciones}</p>}
            {seleccionado.motivoCancelacion && <p>Motivo de cancelación: {seleccionado.motivoCancelacion}</p>}
            <div className="flex flex-wrap gap-2 pt-2">
              {accionesDisponibles(seleccionado).puedeEditar && (
                <Button variant="outline" size="sm" onClick={() => iniciarEdicion(seleccionado)}>Editar</Button>
              )}
              {accionesDisponibles(seleccionado).puedeResolver && (
                <>
                  <Button variant="outline" size="sm" disabled={marcarPagado.isPending} onClick={() => marcarPagado.mutate({ id: seleccionado.id }, { onSuccess: () => setSeleccionado(null) })}>Marcar pagado</Button>
                  <Button variant="outline" size="sm" onClick={() => { setReprogramando(seleccionado.id); setVista("lista") }}>Reprogramar</Button>
                  <Button variant="outline" size="sm" onClick={() => { setCancelando(seleccionado.id); setVista("lista") }}>Cancelar</Button>
                </>
              )}
              <Button variant="outline" size="sm" onClick={() => setSeleccionado(null)}>Cerrar</Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

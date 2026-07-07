import { zodResolver } from "@hookform/resolvers/zod"
import { useMemo, useState } from "react"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import {
  useArbolCuentasContables,
  useCambiarEstadoCuentaContable,
  useCrearCuentaContable,
  useEditarCuentaContable,
  useEliminarCuentaContable,
} from "@/hooks/use-cuenta-contable"
import { useProyectos } from "@/hooks/use-proyecto"
import { useRubros } from "@/hooks/use-rubro"
import type { CuentaContable, CuentaContableNodo, Naturaleza, SaldoEsperado } from "@/types/cuenta-contable"

const NATURALEZAS: Naturaleza[] = ["ACTIVO", "PASIVO", "PN", "RPLUS", "RMINUS"]
const SALDOS_ESPERADOS: SaldoEsperado[] = ["DEUDOR", "ACREEDOR"]

const esquema = z.object({
  codigo: z.string().min(1, "El código es obligatorio").max(20),
  nombre: z.string().min(1, "El nombre es obligatorio").max(160),
  padreId: z.string().optional(),
  naturaleza: z.string().min(1),
  rubroId: z.string().optional(),
  imputable: z.boolean(),
  saldoEsperado: z.string().min(1),
  proyectosUsoHabitualIds: z.array(z.string()).optional(),
})

type Valores = z.infer<typeof esquema>

const VALORES_INICIALES: Valores = {
  codigo: "", nombre: "", padreId: "", naturaleza: "ACTIVO", rubroId: "",
  imputable: true, saldoEsperado: "DEUDOR", proyectosUsoHabitualIds: [],
}

type NodoPlano = { id: number; codigo: string; nombre: string }

function aplanar(nodos: CuentaContableNodo[], excluirId?: number, acc: NodoPlano[] = []): NodoPlano[] {
  for (const n of nodos) {
    if (n.id !== excluirId) {
      acc.push({ id: n.id, codigo: n.codigo, nombre: n.nombre })
      aplanar(n.hijos, excluirId, acc)
    }
  }
  return acc
}

function coincide(nodo: CuentaContableNodo, texto: string): boolean {
  const t = texto.toLowerCase()
  if (nodo.codigo.toLowerCase().includes(t) || nodo.nombre.toLowerCase().includes(t)) return true
  return nodo.hijos.some((h) => coincide(h, t))
}

export function PlanDeCuentasPage() {
  const [busqueda, setBusqueda] = useState("")
  const [editando, setEditando] = useState<CuentaContable | number | null>(null)
  const [expandidos, setExpandidos] = useState<Set<number>>(new Set())

  const arbol = useArbolCuentasContables()
  const rubros = useRubros({ page: 0, size: 100 })
  const proyectos = useProyectos({ page: 0, size: 100 })
  const crear = useCrearCuentaContable()
  const editar = useEditarCuentaContable()
  const cambiarEstado = useCambiarEstadoCuentaContable()
  const eliminar = useEliminarCuentaContable()

  const form = useForm<Valores>({ resolver: zodResolver(esquema), defaultValues: VALORES_INICIALES })

  const editandoId = typeof editando === "number" ? editando : editando?.id
  const nodosPlanos = useMemo(() => aplanar(arbol.data ?? [], editandoId), [arbol.data, editandoId])

  const arbolFiltrado = useMemo(() => {
    if (!busqueda.trim()) return arbol.data ?? []
    return (arbol.data ?? []).filter((n) => coincide(n, busqueda))
  }, [arbol.data, busqueda])

  function alternarExpandido(id: number) {
    setExpandidos((actual) => {
      const nuevo = new Set(actual)
      if (nuevo.has(id)) nuevo.delete(id)
      else nuevo.add(id)
      return nuevo
    })
  }

  function iniciarEdicion(id: number) {
    // Se busca la cuenta completa (con rubro/proyectos) en el listado plano vía la API de detalle no hace falta:
    // el árbol ya trae lo necesario para prellenar, salvo proyectosUsoHabitual (no viaja en el nodo del árbol).
    const nodo = buscarNodo(arbol.data ?? [], id)
    if (!nodo) return
    setEditando(id)
    form.reset({
      codigo: nodo.codigo,
      nombre: nodo.nombre,
      padreId: buscarPadreId(arbol.data ?? [], id)?.toString() ?? "",
      naturaleza: nodo.naturaleza,
      rubroId: nodo.rubroId?.toString() ?? "",
      imputable: nodo.imputable,
      saldoEsperado: nodo.saldoEsperado,
      proyectosUsoHabitualIds: [],
    })
  }

  function iniciarNuevaHija(padreId: number) {
    setEditando(null)
    form.reset({ ...VALORES_INICIALES, padreId: padreId.toString() })
  }

  function cancelarEdicion() {
    setEditando(null)
    form.reset(VALORES_INICIALES)
  }

  function onSubmit(valores: Valores) {
    const input = {
      codigo: valores.codigo,
      nombre: valores.nombre,
      padreId: valores.padreId ? Number(valores.padreId) : undefined,
      naturaleza: valores.naturaleza as Naturaleza,
      rubroId: valores.rubroId ? Number(valores.rubroId) : undefined,
      imputable: valores.imputable,
      saldoEsperado: valores.saldoEsperado as SaldoEsperado,
      proyectosUsoHabitualIds: valores.proyectosUsoHabitualIds?.map(Number),
    }
    if (editandoId) {
      editar.mutate({ id: editandoId, valores: input }, { onSuccess: cancelarEdicion })
    } else {
      crear.mutate(input, { onSuccess: () => form.reset(VALORES_INICIALES) })
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-lg font-semibold text-foreground">Plan de cuentas</h1>
        <p className="text-sm text-muted-foreground">Jerarquía madre/imputable (F3.2). Las cuentas madre solo agrupan y no reciben movimientos.</p>
      </div>

      <Card>
        <CardHeader><CardTitle>{editandoId ? "Editar cuenta" : "Nueva cuenta"}</CardTitle></CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="codigo" render={({ field }) => (
                  <FormItem><FormLabel>Código</FormLabel><FormControl><Input {...field} placeholder="1.1.01" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="nombre" render={({ field }) => (
                  <FormItem><FormLabel>Nombre</FormLabel><FormControl><Input {...field} placeholder="Banco Galicia CC" /></FormControl><FormMessage /></FormItem>
                )} />
                <FormField control={form.control} name="padreId" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Cuenta madre</FormLabel>
                    <FormControl>
                      <select {...field} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        <option value="">Sin madre (raíz)</option>
                        {nodosPlanos.map((n) => (
                          <option key={n.id} value={n.id.toString()}>{n.codigo} — {n.nombre}</option>
                        ))}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
              </div>

              <div className="grid gap-4 sm:grid-cols-3">
                <FormField control={form.control} name="naturaleza" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Naturaleza</FormLabel>
                    <FormControl>
                      <select {...field} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        {NATURALEZAS.map((n) => <option key={n} value={n}>{n}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="rubroId" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Rubro {form.watch("imputable") && "(obligatorio si es imputable)"}</FormLabel>
                    <FormControl>
                      <select {...field} disabled={rubros.isLoading} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        <option value="">Sin rubro</option>
                        {rubros.data?.content?.map((r) => (
                          <option key={r.id} value={r.id.toString()}>{r.nombre}</option>
                        ))}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
                <FormField control={form.control} name="saldoEsperado" render={({ field }) => (
                  <FormItem>
                    <FormLabel>Saldo esperado</FormLabel>
                    <FormControl>
                      <select {...field} className="h-8 w-full rounded-lg border border-input bg-background px-3 py-1.5 text-sm">
                        {SALDOS_ESPERADOS.map((s) => <option key={s} value={s}>{s}</option>)}
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )} />
              </div>

              <FormField control={form.control} name="imputable" render={({ field }) => (
                <FormItem className="flex items-center gap-2 space-y-0">
                  <FormControl>
                    <Checkbox checked={field.value} onCheckedChange={(checked) => field.onChange(checked === true)} />
                  </FormControl>
                  <FormLabel className="!mt-0">Imputable (recibe movimientos). Si no, es una cuenta madre que solo agrupa.</FormLabel>
                </FormItem>
              )} />

              <FormItem>
                <FormLabel>Proyectos de uso habitual</FormLabel>
                <div className="grid gap-2 sm:grid-cols-3">
                  {proyectos.data?.content?.map((p) => (
                    <div key={p.id} className="flex items-center space-x-2">
                      <Checkbox
                        id={`proy-${p.id}`}
                        checked={form.watch("proyectosUsoHabitualIds")?.includes(p.id.toString()) || false}
                        onCheckedChange={(checked) => {
                          const actual = form.watch("proyectosUsoHabitualIds") || []
                          form.setValue("proyectosUsoHabitualIds", checked ? [...actual, p.id.toString()] : actual.filter((id) => id !== p.id.toString()))
                        }}
                      />
                      <label htmlFor={`proy-${p.id}`} className="text-sm">{p.nombre}</label>
                    </div>
                  ))}
                </div>
              </FormItem>

              <div className="flex items-center gap-2">
                <Button type="submit" disabled={crear.isPending || editar.isPending}>{editandoId ? "Guardar" : "Crear"}</Button>
                {editandoId && <Button type="button" variant="outline" onClick={cancelarEdicion}>Cancelar</Button>}
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>Árbol de cuentas</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <Input placeholder="Buscar por código o nombre…" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} className="max-w-xs" />
          {arbol.isLoading ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : arbolFiltrado.length === 0 ? (
            <p className="text-sm text-muted-foreground">Sin resultados.</p>
          ) : (
            <div className="space-y-1">
              {arbolFiltrado.map((nodo) => (
                <NodoArbol
                  key={nodo.id}
                  nodo={nodo}
                  nivel={0}
                  busqueda={busqueda}
                  expandidos={expandidos}
                  onToggle={alternarExpandido}
                  onEditar={iniciarEdicion}
                  onNuevaHija={iniciarNuevaHija}
                  onCambiarEstado={(id, activo) => cambiarEstado.mutate({ id, activo })}
                  onEliminar={(id) => eliminar.mutate(id)}
                />
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function buscarNodo(nodos: CuentaContableNodo[], id: number): CuentaContableNodo | undefined {
  for (const n of nodos) {
    if (n.id === id) return n
    const enHijos = buscarNodo(n.hijos, id)
    if (enHijos) return enHijos
  }
  return undefined
}

function buscarPadreId(nodos: CuentaContableNodo[], hijoId: number, padreId: number | null = null): number | null {
  for (const n of nodos) {
    if (n.id === hijoId) return padreId
    const encontrado = buscarPadreId(n.hijos, hijoId, n.id)
    if (encontrado !== null) return encontrado
  }
  return null
}

function NodoArbol({
  nodo, nivel, busqueda, expandidos, onToggle, onEditar, onNuevaHija, onCambiarEstado, onEliminar,
}: {
  nodo: CuentaContableNodo
  nivel: number
  busqueda: string
  expandidos: Set<number>
  onToggle: (id: number) => void
  onEditar: (id: number) => void
  onNuevaHija: (id: number) => void
  onCambiarEstado: (id: number, activo: boolean) => void
  onEliminar: (id: number) => void
}) {
  const tieneHijos = nodo.hijos.length > 0
  const forzarAbierto = busqueda.trim().length > 0
  const abierto = forzarAbierto || expandidos.has(nodo.id)

  return (
    <div>
      <div className="flex items-center gap-2 rounded px-2 py-1 hover:bg-muted/50" style={{ paddingLeft: nivel * 20 }}>
        {tieneHijos ? (
          <button type="button" onClick={() => onToggle(nodo.id)} className="w-4 text-muted-foreground">
            {abierto ? "▾" : "▸"}
          </button>
        ) : (
          <span className="w-4" />
        )}
        <span className="font-mono text-sm text-muted-foreground">{nodo.codigo}</span>
        <span className="text-sm">{nodo.nombre}</span>
        <span className={`rounded px-1.5 py-0.5 text-xs ${nodo.imputable ? "bg-primary/10 text-primary" : "bg-muted text-muted-foreground"}`}>
          {nodo.imputable ? "imputable" : "madre"}
        </span>
        {!nodo.activo && <span className="rounded bg-destructive/10 px-1.5 py-0.5 text-xs text-destructive">inactiva</span>}
        <div className="ml-auto flex gap-2">
          <Button variant="outline" size="sm" onClick={() => onNuevaHija(nodo.id)}>+ hija</Button>
          <Button variant="outline" size="sm" onClick={() => onEditar(nodo.id)}>Editar</Button>
          <Button variant="outline" size="sm" onClick={() => onCambiarEstado(nodo.id, nodo.activo)}>
            {nodo.activo ? "Desactivar" : "Activar"}
          </Button>
          <Button variant="outline" size="sm" onClick={() => onEliminar(nodo.id)}>Eliminar</Button>
        </div>
      </div>
      {tieneHijos && abierto && (
        <div>
          {nodo.hijos
            .filter((h) => !busqueda.trim() || coincide(h, busqueda))
            .map((hijo) => (
              <NodoArbol
                key={hijo.id}
                nodo={hijo}
                nivel={nivel + 1}
                busqueda={busqueda}
                expandidos={expandidos}
                onToggle={onToggle}
                onEditar={onEditar}
                onNuevaHija={onNuevaHija}
                onCambiarEstado={onCambiarEstado}
                onEliminar={onEliminar}
              />
            ))}
        </div>
      )}
    </div>
  )
}

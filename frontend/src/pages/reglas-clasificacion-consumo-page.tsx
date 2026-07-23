import { useMemo, useState } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import {
  useCambiarEstadoReglaClasificacion,
  useCrearReglaClasificacion,
  useReglasClasificacionConsumo,
} from "@/hooks/use-consumo-tarjeta"
import { useCuentasContables } from "@/hooks/use-cuenta-contable"
import { useProveedores } from "@/hooks/use-proveedor"
import { useProyectos } from "@/hooks/use-proyecto"
import { useConceptos } from "@/hooks/use-concepto"

const selectClase = "h-8 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm"
const inputClase = "h-8"

export function ReglasClasificacionConsumoPage() {
  const [patron, setPatron] = useState("")
  const [cuentaContableId, setCuentaContableId] = useState("")
  const [proveedorId, setProveedorId] = useState("")
  const [proyectoId, setProyectoId] = useState("")
  const [conceptoId, setConceptoId] = useState("")

  const reglas = useReglasClasificacionConsumo()
  const cuentasContables = useCuentasContables({ activo: true, page: 0, size: 500 })
  const cuentasImputables = useMemo(() => (cuentasContables.data?.content ?? []).filter((c) => c.imputable), [cuentasContables.data])
  const proveedores = useProveedores({ activo: true, page: 0, size: 200 })
  const proyectos = useProyectos({ activo: true, page: 0, size: 200 })
  const conceptos = useConceptos({ activo: true, page: 0, size: 200 })

  const crear = useCrearReglaClasificacion()
  const cambiarEstado = useCambiarEstadoReglaClasificacion()

  function onCrear() {
    if (!patron || !cuentaContableId) return
    crear.mutate({
      patron,
      cuentaContableId: Number(cuentaContableId),
      proveedorId: proveedorId ? Number(proveedorId) : undefined,
      proyectoId: proyectoId ? Number(proyectoId) : undefined,
      conceptoId: conceptoId ? Number(conceptoId) : undefined,
    }, {
      onSuccess: () => { setPatron(""); setCuentaContableId(""); setProveedorId(""); setProyectoId(""); setConceptoId("") },
    })
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-lg font-semibold text-foreground">Reglas de clasificación de consumos</h1>
        <p className="text-sm text-muted-foreground">
          "Si la descripción de un consumo de tarjeta contiene este texto, clasificarlo así" — usadas por "Clasificar
          masivamente" en la vista de cada tarjeta.
        </p>
      </div>

      <Card>
        <CardHeader><CardTitle>Nueva regla</CardTitle></CardHeader>
        <CardContent className="grid gap-3 sm:grid-cols-5">
          <label className="flex flex-col text-xs text-muted-foreground">
            Patrón (contiene)
            <Input value={patron} onChange={(e) => setPatron(e.target.value)} placeholder="DONWEB" className={inputClase} />
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Cuenta contable
            <select value={cuentaContableId} onChange={(e) => setCuentaContableId(e.target.value)} className={selectClase}>
              <option value="">Seleccionar…</option>
              {cuentasImputables.map((c) => <option key={c.id} value={c.id.toString()}>{c.codigo} — {c.nombre}</option>)}
            </select>
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Proveedor (opcional)
            <select value={proveedorId} onChange={(e) => setProveedorId(e.target.value)} className={selectClase}>
              <option value="">—</option>
              {(proveedores.data?.content ?? []).map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
            </select>
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Proyecto (opcional)
            <select value={proyectoId} onChange={(e) => setProyectoId(e.target.value)} className={selectClase}>
              <option value="">—</option>
              {(proyectos.data?.content ?? []).map((p) => <option key={p.id} value={p.id.toString()}>{p.nombre}</option>)}
            </select>
          </label>
          <label className="flex flex-col text-xs text-muted-foreground">
            Suscripción (opcional)
            <select value={conceptoId} onChange={(e) => setConceptoId(e.target.value)} className={selectClase}>
              <option value="">—</option>
              {(conceptos.data?.content ?? []).map((c) => <option key={c.id} value={c.id.toString()}>{c.nombre}</option>)}
            </select>
          </label>
          <div className="sm:col-span-5">
            <Button onClick={onCrear} disabled={!patron || !cuentaContableId || crear.isPending}>Agregar regla</Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>Reglas ({reglas.data?.totalElements ?? 0})</CardTitle></CardHeader>
        <CardContent>
          <table className="w-full text-left text-sm">
            <thead className="text-muted-foreground">
              <tr className="border-b border-border">
                <th className="py-2 pr-4 font-medium">Patrón</th>
                <th className="py-2 pr-4 font-medium">Cuenta</th>
                <th className="py-2 pr-4 font-medium">Proveedor</th>
                <th className="py-2 pr-4 font-medium">Estado</th>
                <th className="py-2 pr-4 font-medium">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {(reglas.data?.content ?? []).map((r) => (
                <tr key={r.id} className="border-b border-border last:border-0">
                  <td className="py-2 pr-4">{r.patron}</td>
                  <td className="py-2 pr-4">{r.cuentaContableCodigo} — {r.cuentaContableNombre}</td>
                  <td className="py-2 pr-4">{r.proveedorNombre ?? "—"}</td>
                  <td className="py-2 pr-4">{r.activo ? "Activa" : "Inactiva"}</td>
                  <td className="py-2 pr-4">
                    <Button variant="outline" size="sm" disabled={cambiarEstado.isPending} onClick={() => cambiarEstado.mutate({ id: r.id, activo: r.activo })}>
                      {r.activo ? "Desactivar" : "Activar"}
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </CardContent>
      </Card>
    </div>
  )
}

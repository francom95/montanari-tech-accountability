import { useQuery } from "@tanstack/react-query"
import { useState } from "react"

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { RequireAdmin } from "@/components/require-admin"
import { http } from "@/lib/http"
import type { PageResponse } from "@/types/auth"
import type { AccionAuditoria, AuditoriaLog } from "@/types/auditoria"

const ACCIONES: AccionAuditoria[] = [
  "CREAR",
  "EDITAR",
  "ELIMINAR",
  "CONFIRMAR",
  "ANULAR",
  "DUPLICAR",
  "CERRAR_PERIODO",
  "REABRIR_PERIODO",
  "IMPORTAR",
  "LOGIN",
  "CAMBIO_ESTADO",
  "EXPORTAR_SENSIBLE",
]

export function AuditoriaPage() {
  return (
    <RequireAdmin>
      <ConsultaAuditoria />
    </RequireAdmin>
  )
}

function ConsultaAuditoria() {
  const [entidadTipo, setEntidadTipo] = useState("")
  const [usuarioId, setUsuarioId] = useState("")
  const [accion, setAccion] = useState<AccionAuditoria | "">("")

  const auditoriaQuery = useQuery({
    queryKey: ["auditoria", { entidadTipo, usuarioId, accion }],
    queryFn: async () =>
      (
        await http.get<PageResponse<AuditoriaLog>>("/auditoria", {
          params: {
            entidadTipo: entidadTipo || undefined,
            usuarioId: usuarioId || undefined,
            accion: accion || undefined,
            size: 50,
          },
        })
      ).data,
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-lg font-semibold text-foreground">Auditoría</h1>
        <p className="text-sm text-muted-foreground">
          Rastro de operaciones sensibles: quién, cuándo, qué acción y los datos antes/después (F1.1 §4).
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Filtros</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-3">
          <div className="grid gap-2">
            <Label htmlFor="filtro-entidad">Tipo de entidad</Label>
            <Input
              id="filtro-entidad"
              placeholder="Usuario"
              value={entidadTipo}
              onChange={(e) => setEntidadTipo(e.target.value)}
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="filtro-usuario">ID de usuario</Label>
            <Input
              id="filtro-usuario"
              type="number"
              value={usuarioId}
              onChange={(e) => setUsuarioId(e.target.value)}
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="filtro-accion">Acción</Label>
            <select
              id="filtro-accion"
              value={accion}
              onChange={(e) => setAccion(e.target.value as AccionAuditoria | "")}
              className="h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
            >
              <option value="">Todas</option>
              {ACCIONES.map((a) => (
                <option key={a} value={a}>
                  {a}
                </option>
              ))}
            </select>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Resultados</CardTitle>
        </CardHeader>
        <CardContent>
          {auditoriaQuery.isLoading ? (
            <p className="text-sm text-muted-foreground">Cargando…</p>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground">
                <tr className="border-b border-border">
                  <th className="py-2 pr-4 font-medium">Fecha</th>
                  <th className="py-2 pr-4 font-medium">Acción</th>
                  <th className="py-2 pr-4 font-medium">Entidad</th>
                  <th className="py-2 pr-4 font-medium">Usuario</th>
                  <th className="py-2 font-medium">Detalle</th>
                </tr>
              </thead>
              <tbody>
                {auditoriaQuery.data?.content.map((log) => (
                  <tr key={log.id} className="border-b border-border align-top last:border-0">
                    <td className="py-2 pr-4 whitespace-nowrap">
                      {new Date(log.fechaHora).toLocaleString("es-AR")}
                    </td>
                    <td className="py-2 pr-4">{log.accion}</td>
                    <td className="py-2 pr-4">
                      {log.entidadTipo} #{log.entidadId}
                    </td>
                    <td className="py-2 pr-4">{log.usuarioId ?? "—"}</td>
                    <td className="py-2">
                      {log.detalle && <p>{log.detalle}</p>}
                      {log.datosAntes && (
                        <pre className="max-w-md overflow-x-auto whitespace-pre-wrap text-xs text-muted-foreground">
                          antes: {log.datosAntes}
                        </pre>
                      )}
                      {log.datosDespues && (
                        <pre className="max-w-md overflow-x-auto whitespace-pre-wrap text-xs text-muted-foreground">
                          después: {log.datosDespues}
                        </pre>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

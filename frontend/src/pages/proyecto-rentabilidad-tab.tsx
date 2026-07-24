import { useState } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { descargarReporteRentabilidadExcel, descargarReporteRentabilidadPdf, useReporteRentabilidadProyecto } from "@/hooks/use-rentabilidad-proyecto"

function n(v: number | null | undefined) {
  return v === null || v === undefined ? "-" : v.toLocaleString("es-AR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function RentabilidadTab({ proyectoId }: { proyectoId: number }) {
  const reporte = useReporteRentabilidadProyecto(proyectoId)
  const [descargando, setDescargando] = useState<"excel" | "pdf" | null>(null)

  async function descargar(formato: "excel" | "pdf") {
    setDescargando(formato)
    try {
      if (formato === "excel") await descargarReporteRentabilidadExcel(proyectoId)
      else await descargarReporteRentabilidadPdf(proyectoId)
    } finally {
      setDescargando(null)
    }
  }

  if (reporte.isLoading || !reporte.data) {
    return <p className="text-sm text-muted-foreground">Cargando…</p>
  }

  const r = reporte.data

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-base font-semibold">{r.proyectoNombre}</h2>
          <p className="text-sm text-muted-foreground">{r.clienteNombre} · {r.estado}</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" disabled={descargando !== null} onClick={() => descargar("excel")}>
            {descargando === "excel" ? "Generando…" : "Exportar Excel"}
          </Button>
          <Button variant="outline" size="sm" disabled={descargando !== null} onClick={() => descargar("pdf")}>
            {descargando === "pdf" ? "Generando…" : "Exportar PDF"}
          </Button>
        </div>
      </div>

      {r.advertencias.length > 0 && (
        <Card>
          <CardContent className="py-4 text-sm text-amber-700 dark:text-amber-400">
            <ul className="list-disc space-y-1 pl-4">
              {r.advertencias.map((a, i) => <li key={i}>{a}</li>)}
            </ul>
          </CardContent>
        </Card>
      )}

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader><CardTitle>Ingresos</CardTitle></CardHeader>
          <CardContent>
            <table className="w-full text-left text-sm">
              <tbody>
                <Fila etiqueta="Total facturado (ARS)" valor={r.totalFacturadoVentaArs} />
                <Fila etiqueta="Total cobrado (ARS)" valor={r.totalCobradoArs} />
                <Fila etiqueta="Pendiente de cobro (ARS)" valor={r.pendienteCobroArs} destacado />
                <Fila etiqueta="Facturas confirmadas" valor={r.facturasVentaConfirmadas} />
                <Fila etiqueta="Facturas saldadas" valor={r.facturasVentaSaldadas} />
              </tbody>
            </table>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Egresos</CardTitle></CardHeader>
          <CardContent>
            <table className="w-full text-left text-sm">
              <tbody>
                <Fila etiqueta="Total facturado de compra (ARS)" valor={r.totalFacturadoCompraArs} />
                <Fila etiqueta="Total pagado (ARS)" valor={r.totalPagadoArs} />
                <Fila etiqueta="Pendiente de pago (ARS)" valor={r.pendientePagoArs} destacado />
              </tbody>
            </table>
            {r.proveedores.length > 0 && (
              <table className="mt-4 w-full text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr className="border-b border-border">
                    <th className="py-1 pr-2 font-medium">Proveedor</th>
                    <th className="py-1 pr-2 font-medium text-right">Pagado</th>
                    <th className="py-1 pr-2 font-medium text-right">Pendiente</th>
                  </tr>
                </thead>
                <tbody>
                  {r.proveedores.map((p) => (
                    <tr key={p.proveedorId} className="border-b border-border last:border-0">
                      <td className="py-1 pr-2">{p.proveedorNombre}</td>
                      <td className="py-1 pr-2 text-right">{n(p.pagadoArs)}</td>
                      <td className="py-1 pr-2 text-right">{n(p.pendienteArs)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader><CardTitle>Comisiones e impuestos atribuidos</CardTitle></CardHeader>
        <CardContent>
          {r.comisiones.length > 0 && (
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground">
                <tr className="border-b border-border">
                  <th className="py-1 pr-2 font-medium">Comisionista</th>
                  <th className="py-1 pr-2 font-medium">Estado</th>
                  <th className="py-1 pr-2 font-medium text-right">Importe</th>
                </tr>
              </thead>
              <tbody>
                {r.comisiones.map((c) => (
                  <tr key={c.id} className="border-b border-border last:border-0">
                    <td className="py-1 pr-2">{c.comisionistaNombre}</td>
                    <td className="py-1 pr-2">{c.estadoPago}</td>
                    <td className="py-1 pr-2 text-right">{n(c.importeFinal ?? c.importeEstimado)} {c.monedaCodigo}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <table className="mt-4 w-full text-left text-sm">
            <tbody>
              <Fila etiqueta="Total comisiones (ARS)" valor={r.comisionesArs} />
              <Fila etiqueta="Impuestos atribuidos (ARS)" valor={r.impuestosAtribuidosArs} />
            </tbody>
          </table>
        </CardContent>
      </Card>

      {r.presupuesto && (
        <Card>
          <CardHeader><CardTitle>Presupuesto vs. real</CardTitle></CardHeader>
          <CardContent>
            <table className="w-full text-left text-sm">
              <tbody>
                <Fila etiqueta="Precio final presupuestado (USD)" valor={r.presupuesto.calculado.precioFinalCliente} />
                <tr className="border-b border-border last:border-0">
                  <td className="py-2 pr-4">Pagos emparejados con factura real</td>
                  <td className="py-2 pr-4 text-right">{r.presupuesto.pagosEmparejadosConFactura} de {r.presupuesto.cantidadPagosPactados}</td>
                </tr>
                <Fila etiqueta="Presupuesto convertido (ARS, porción emparejada)" valor={r.presupuesto.presupuestoConvertidoArs} />
                <Fila etiqueta="Facturado real (ARS, porción emparejada)" valor={r.presupuesto.facturadoEmparejadoArs} />
                <Fila etiqueta="Diferencia (ARS)" valor={r.presupuesto.diferenciaArs} destacado />
              </tbody>
            </table>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardContent className="py-4">
          <div className="flex items-center justify-between text-base font-semibold">
            <span>Margen real (ARS)</span>
            <span>{n(r.margenRealArs)}</span>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

function Fila({ etiqueta, valor, destacado }: { etiqueta: string; valor: number | null; destacado?: boolean }) {
  return (
    <tr className={`border-b border-border last:border-0 ${destacado ? "font-semibold" : ""}`}>
      <td className="py-2 pr-4">{etiqueta}</td>
      <td className="py-2 pr-4 text-right">{n(valor)}</td>
    </tr>
  )
}

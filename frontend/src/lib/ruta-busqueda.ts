import type { ResultadoBusqueda } from "@/types/busqueda"

/**
 * Mapa de rutas por tipo (F9.2): 4 entidades tienen ruta de detalle real
 * (Proyecto, TarjetaCredito, CuentaContable vía el Mayor, y Etapa via la
 * pestaña del proyecto padre); las otras 12 navegan a su lista existente
 * con ?id= (decisión del usuario: filtro exacto, no texto precargado).
 */
export function rutaParaResultadoBusqueda(r: ResultadoBusqueda): string {
  switch (r.tipo) {
    case "PROYECTO":
      return `/proyectos/${r.id}`
    case "TARJETA_CREDITO":
      return `/tarjetas-credito/${r.id}`
    case "CUENTA_CONTABLE":
      return `/contabilidad/mayor/${r.id}`
    case "ETAPA":
      return `/proyectos/${r.contextoId}?tab=etapas&id=${r.id}`
    case "ASIENTO":
      return `/contabilidad/asientos?id=${r.id}`
    case "FACTURA_VENTA":
      return `/facturacion/ventas?id=${r.id}`
    case "FACTURA_COMPRA":
      return `/facturacion/compras?id=${r.id}`
    case "CLIENTE":
      return `/clientes?id=${r.id}`
    case "PROVEEDOR":
      return `/proveedores?id=${r.id}`
    case "PAGO":
      return `/facturacion/pagos?id=${r.id}`
    case "COBRO":
      return `/facturacion/cobros?id=${r.id}`
    case "MOVIMIENTO_BANCARIO":
      return `/bancos/movimientos?id=${r.id}`
    case "VENCIMIENTO":
      return `/presupuesto/vencimientos?id=${r.id}`
    case "PENDIENTE_ADMINISTRATIVO":
      return `/pendientes?id=${r.id}`
    case "LIQUIDACION_IVA":
      return `/impuestos/iva?id=${r.id}`
    case "LIQUIDACION_IIBB":
      return `/impuestos/iibb?id=${r.id}`
  }
}

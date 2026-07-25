package com.montanaritech.contable.busqueda;

/**
 * Los 16 tipos de entidad que federa la búsqueda global (F9.2) — el plan
 * agrupa "facturas" e "impuestos" como una sola categoría cada uno, pero son
 * tablas distintas (venta/compra, IVA/IIBB), así que se listan por separado.
 */
public enum TipoEntidadBusqueda {
    ASIENTO,
    FACTURA_VENTA,
    FACTURA_COMPRA,
    CLIENTE,
    PROVEEDOR,
    PROYECTO,
    ETAPA,
    CUENTA_CONTABLE,
    PAGO,
    COBRO,
    MOVIMIENTO_BANCARIO,
    TARJETA_CREDITO,
    VENCIMIENTO,
    PENDIENTE_ADMINISTRATIVO,
    LIQUIDACION_IVA,
    LIQUIDACION_IIBB
}

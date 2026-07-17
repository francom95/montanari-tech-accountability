package com.montanaritech.contable.facturacion.facturaventa;

/**
 * Discriminador del concepto {@code INGRESO_VENTA} en {@code mapeo_cuenta}
 * (F4.1 §1.3): resuelve "Ingresos por ventas" vs "Otros ingresos por
 * ventas" cuando la línea no fija su propia {@code cuentaContable}.
 */
public enum TipoIngreso {
    VENTA,
    OTRA_VENTA
}

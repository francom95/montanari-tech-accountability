package com.montanaritech.contable.facturacion;

/**
 * Tipo de comprobante de venta o compra (F1.1 §6.5, ADR-13). Las notas de
 * crédito/débito son el <b>mismo modelo</b> de factura con signo derivado
 * del tipo (no hay entidad aparte): una {@code NOTA_CREDITO_*} invierte los
 * lados debe/haber del asiento generado respecto de una factura normal; una
 * {@code NOTA_DEBITO_*} se comporta igual que una factura (suma al CxC/CxP).
 * Predeclarado completo (F4.2 solo emite {@code FACTURA_*}; F4.3 y las notas
 * de crédito/débito lo reusan tal cual).
 */
public enum TipoComprobante {
    FACTURA_A,
    FACTURA_B,
    FACTURA_C,
    FACTURA_E,
    NOTA_CREDITO_A,
    NOTA_CREDITO_B,
    NOTA_CREDITO_C,
    NOTA_CREDITO_E,
    NOTA_DEBITO_A,
    NOTA_DEBITO_B,
    NOTA_DEBITO_C,
    NOTA_DEBITO_E,
    RECIBO,
    TICKET,
    OTRO
}

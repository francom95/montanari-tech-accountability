package com.montanaritech.contable.maestros.proveedor;

/**
 * Condición frente al IVA del proveedor (F4.1 §5, F4.3): junto con el
 * {@code TipoComprobante} de la factura, determina si el comprobante computa
 * crédito fiscal o si el IVA se absorbe en el costo.
 */
public enum CondicionIva {
    RESPONSABLE_INSCRIPTO,
    MONOTRIBUTISTA,
    EXENTO,
    CONSUMIDOR_FINAL
}

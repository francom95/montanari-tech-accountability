package com.montanaritech.contable.facturacion.comprobantetributo;

/** De qué documento es el tributo (F1.1 §6.5). Predeclarado completo: F4.3/F4.4 lo reusan. */
public enum ComprobanteTipo {
    FACTURA_VENTA,
    FACTURA_COMPRA,
    COBRO,
    PAGO
}

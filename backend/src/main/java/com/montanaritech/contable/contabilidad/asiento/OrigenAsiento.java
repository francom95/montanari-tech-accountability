package com.montanaritech.contable.contabilidad.asiento;

/**
 * Dominio cerrado definido por F3.1 §3.1/§4.4. F3.4 (carga manual, este
 * paso) únicamente produce asientos {@code MANUAL}; el resto de los valores
 * los generan pasos futuros ya diseñados por F3.1: {@code AJUSTE} (F3.5,
 * reversión de anulación en período cerrado y ajustes de F4.x),
 * {@code APERTURA} (F10.3), {@code IMPORTACION} (F4.6/F10.x) y los orígenes
 * de documento ({@code FACTURA_VENTA}, {@code FACTURA_COMPRA}, {@code COBRO},
 * {@code PAGO}, {@code LIQUIDACION_IVA}, {@code LIQUIDACION_IIBB},
 * {@code RESUMEN_TARJETA}, {@code MOVIMIENTO_BANCARIO} — F4.x/F5.x/F6.x).
 * Se predeclara completo (mismo criterio que {@code EstadoDocumento}/
 * {@code AccionAuditoria} en el molde F1.8) porque el dominio ya está
 * cerrado por el diseño, aunque su columna es {@code VARCHAR} y agregar
 * valores más adelante no requiere migración.
 */
public enum OrigenAsiento {
    MANUAL,
    AJUSTE,
    APERTURA,
    IMPORTACION,
    FACTURA_VENTA,
    FACTURA_COMPRA,
    COBRO,
    PAGO,
    LIQUIDACION_IVA,
    LIQUIDACION_IIBB,
    RESUMEN_TARJETA,
    MOVIMIENTO_BANCARIO
}

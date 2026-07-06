package com.montanaritech.contable.common.estado;

/**
 * Molde de referencia de PL-5 (F1.8). Aplica a facturas, cobros, pagos y
 * asientos (F1.1 §5 / F1.8 reglas de negocio): solo {@code CONFIRMADO}
 * impacta contabilidad y reportes; {@code ANULADO} conserva trazabilidad
 * (nunca se borra el registro, se marca anulado).
 */
public enum EstadoDocumento {
    BORRADOR,
    CONFIRMADO,
    ANULADO
}

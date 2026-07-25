package com.montanaritech.contable.alerta;

/**
 * Las 13 reglas del plan (F9.1 §12) se consolidan en 9 tipos: varias reglas
 * apuntan a la misma tabla/query y solo difieren en texto/severidad
 * (VENCIMIENTO_PROXIMO cubre impositivos, tarjetas, planes de pago e
 * impuestos a pagar — los 13 {@code TipoVencimiento} son un único campo de
 * la misma fila; CXC_ATRASADA cubre tanto "facturas sin cobrar" como
 * "cobros atrasados", misma condición).
 */
public enum TipoAlerta {
    VENCIMIENTO_PROXIMO,
    VENCIMIENTO_VENCIDO,
    COMPROMISO_PROXIMO,
    CXP_PROXIMO,
    CXC_ATRASADA,
    SALDO_BAJO,
    MOVIMIENTO_BANCARIO_PENDIENTE,
    CONCILIACION_DIFERENCIA,
    PENDIENTE_ADMINISTRATIVO_PROXIMO
}

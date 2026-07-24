package com.montanaritech.contable.vencimientos;

/** Quién generó el vencimiento (F8.1) — trazabilidad e idempotencia de {@code VencimientoService.generarAutomaticos}. */
public enum OrigenGeneracionVencimiento {
    MANUAL,
    LIQUIDACION_IVA,
    LIQUIDACION_IIBB,
    TARJETA,
    CONCEPTO_RECURRENTE,
    COMPROMISO
}

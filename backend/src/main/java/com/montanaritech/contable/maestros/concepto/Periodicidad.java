package com.montanaritech.contable.maestros.concepto;

/**
 * Periodicidad de un concepto recurrente (F8.1). Antes texto libre sin
 * ninguna lógica que lo leyera; ahora la usa {@code VencimientoService}
 * para decidir si generar un vencimiento del mes/año en curso.
 */
public enum Periodicidad {
    UNICA,
    MENSUAL,
    ANUAL
}

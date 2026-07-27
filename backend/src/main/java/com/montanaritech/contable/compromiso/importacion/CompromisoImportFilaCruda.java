package com.montanaritech.contable.compromiso.importacion;

/**
 * Fila cruda de la hoja "Presupuesto de Pagos", sección "1. Plan de Pagos
 * Impuesto a las Ganancias" (F10.1 §5, filas 7-21), columnas fijas en orden:
 * {@code Concepto/Cuota}, {@code Fecha de Vencimiento}, {@code Totales}.
 * Todas las filas son {@code tipo=CUOTA_PLAN_DE_PAGOS} — las secciones 2-6 de
 * la misma hoja (Sueldos, Honorarios, etc.) no se migran fila por fila
 * (F10.1: se dan de alta como 5 {@code Concepto} recurrentes a mano).
 */
public record CompromisoImportFilaCruda(
        int numeroFila,
        String concepto,
        String fechaVencimiento,
        String total
) {}

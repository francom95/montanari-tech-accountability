package com.montanaritech.contable.contabilidad.mayor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una fila del mayor (F3.1 §5.2). {@code esSaldoAnterior = true} solo en la
 * fila sintética inicial que resume todo lo confirmado antes de
 * {@code fechaDesde} (CP-17); en esa fila el resto de los campos de
 * movimiento (asiento, cuenta, debe/haber, moneda) van en {@code null}.
 */
public record MayorFilaResponse(
        boolean esSaldoAnterior,
        LocalDate fecha,
        Long asientoId,
        Long numeroAsiento,
        String descripcion,
        Long cuentaContableId,
        String cuentaContableCodigo,
        String cuentaContableNombre,
        BigDecimal debe,
        BigDecimal haber,
        BigDecimal saldoAcumulado,
        Long monedaId,
        String monedaCodigo,
        BigDecimal importeOriginal,
        BigDecimal tipoCambio,
        String origen
) {}

package com.montanaritech.contable.common.asiento;

import java.math.BigDecimal;

/**
 * Una línea debe/haber de un asiento generado automáticamente (F3.1 §8.2,
 * forma real de línea — el molde F1.8 solo tenía {@code cuentaCodigo, debe,
 * haber, descripcion}). Exactamente uno de {@code debe}/{@code haber} es
 * distinto de cero. {@code fuenteTc} es {@code "MANUAL"}/{@code "AUTOMATICO"}
 * como String (no el enum de {@code AsientoLinea}, para no crear una
 * dependencia de paquete común→contabilidad); {@code AsientoService} lo
 * convierte al persistir. Las dimensiones analíticas son opcionales (F3.1
 * §3.1, decisión D-1).
 */
public record LineaAsientoGenerada(
        String cuentaCodigo,
        BigDecimal debe,
        BigDecimal haber,
        String descripcion,
        Long monedaId,
        BigDecimal importeOriginal,
        BigDecimal tipoCambio,
        String fuenteTc,
        Long proyectoId,
        Long etapaId,
        Long clienteId,
        Long proveedorId,
        Long cuentaBancariaId
) {
    /** Constructor mínimo para cuando solo interesa validar balance (F3.4). */
    public LineaAsientoGenerada(String cuentaCodigo, BigDecimal debe, BigDecimal haber, String descripcion) {
        this(cuentaCodigo, debe, haber, descripcion, null, null, null, null, null, null, null, null, null);
    }
}

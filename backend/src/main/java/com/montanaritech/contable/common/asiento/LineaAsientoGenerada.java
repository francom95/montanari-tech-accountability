package com.montanaritech.contable.common.asiento;

import java.math.BigDecimal;

/**
 * Una línea debe/haber de un asiento generado automáticamente. Exactamente
 * uno de {@code debe}/{@code haber} es distinto de cero (F1.1: una línea no
 * imputa a ambos lados a la vez).
 */
public record LineaAsientoGenerada(
        String cuentaCodigo,
        BigDecimal debe,
        BigDecimal haber,
        String descripcion
) {
}

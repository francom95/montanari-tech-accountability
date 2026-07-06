package com.montanaritech.contable.common.asiento;

import com.montanaritech.contable.common.error.NegocioException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Σdebe = Σhaber es innegociable (F1.1, regla de negocio de F1.8): si no
 * balancea, el asiento jamás se persiste como confirmado. Se usa desde
 * cualquier {@link AsientoGenerator} y también desde la carga manual de
 * asientos (F3.4).
 */
public final class ValidadorBalanceAsiento {

    private ValidadorBalanceAsiento() {
    }

    public static void validar(List<LineaAsientoGenerada> lineas) {
        BigDecimal totalDebe = lineas.stream().map(LineaAsientoGenerada::debe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalHaber = lineas.stream().map(LineaAsientoGenerada::haber).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebe.compareTo(totalHaber) != 0) {
            throw new NegocioException(
                    "ASIENTO_NO_BALANCEA",
                    "El asiento no balancea: debe=%s, haber=%s".formatted(totalDebe, totalHaber));
        }
    }
}

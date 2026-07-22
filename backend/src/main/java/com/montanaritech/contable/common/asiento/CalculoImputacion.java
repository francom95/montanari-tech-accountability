package com.montanaritech.contable.common.asiento;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Algoritmo de imputación de un cobro/pago contra un comprobante (F3.1 §6.2,
 * §6.3/§6.4 — regla del residuo, F4.4). Compartido por los generadores de
 * Cobro y Pago: la única diferencia entre ambos es a qué cuenta/lado va la
 * diferencia de cambio resultante (signo opuesto, ver F4.1 §6.3), no el
 * cálculo del monto cancelado.
 *
 * <p>Regla del residuo (F3.1 §6.3): cada imputación cancela el comprobante
 * por {@code round2(montoImputadoOriginal × tipoCambioComprobante)}, salvo la
 * imputación que lleva el saldo en moneda original a cero — esa cancela por
 * el saldo ARS contable remanente exacto (no por la fórmula), y la diferencia
 * (incluido cualquier residuo de redondeos previos) se absorbe en la
 * diferencia de cambio.
 */
public final class CalculoImputacion {

    private CalculoImputacion() {
    }

    public record Resultado(BigDecimal montoFondosArs, BigDecimal montoCanceladoArs, BigDecimal diferenciaCambioArs) {
    }

    public static Resultado calcular(
            BigDecimal montoImputadoOriginal,
            BigDecimal tipoCambioOperacion,
            BigDecimal tipoCambioComprobante,
            BigDecimal saldoOriginalAntes,
            BigDecimal totalArsComprobante,
            BigDecimal montoArsCanceladoPrevio) {
        boolean cierraSaldo = montoImputadoOriginal.compareTo(saldoOriginalAntes) == 0;
        BigDecimal montoFondosArs = round2(montoImputadoOriginal.multiply(tipoCambioOperacion));
        BigDecimal montoCanceladoArs = cierraSaldo
                ? totalArsComprobante.subtract(montoArsCanceladoPrevio)
                : round2(montoImputadoOriginal.multiply(tipoCambioComprobante));
        BigDecimal diferencia = montoFondosArs.subtract(montoCanceladoArs);
        return new Resultado(montoFondosArs, montoCanceladoArs, diferencia);
    }

    public static BigDecimal round2(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}

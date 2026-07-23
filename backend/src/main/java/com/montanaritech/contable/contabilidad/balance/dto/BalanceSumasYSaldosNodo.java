package com.montanaritech.contable.contabilidad.balance.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Una fila del balance de sumas y saldos (F7.2). {@code debe}/{@code haber}
 * de una cuenta madre son el roll-up de todas sus descendientes imputables
 * (una madre nunca recibe líneas directas, F3.4 §{@code CUENTA_NO_IMPUTABLE}).
 * {@code saldo} = debe − haber, sin valor absoluto (mismo criterio que
 * {@code MayorFilaResponse.saldoAcumulado}): positivo es deudor, negativo acreedor.
 */
public record BalanceSumasYSaldosNodo(
        Long cuentaId,
        String codigo,
        String nombre,
        boolean imputable,
        BigDecimal debe,
        BigDecimal haber,
        BigDecimal saldo,
        String saldoEtiqueta,
        String saldoEsperado,
        boolean contrarioAlEsperado,
        List<BalanceSumasYSaldosNodo> hijos
) {}

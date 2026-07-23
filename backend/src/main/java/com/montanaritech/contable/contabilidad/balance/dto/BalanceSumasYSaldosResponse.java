package com.montanaritech.contable.contabilidad.balance.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code balancea}/{@code diferencia} se calculan SIEMPRE sobre el total sin
 * filtrar (independiente de {@code incluirSinMovimiento}/{@code nivelMaximo},
 * que solo afectan qué se muestra en {@code raices}) — es una verificación de
 * integridad del período, no un filtro de pantalla. Si no balancea es señal
 * de bug real: nunca se oculta.
 */
public record BalanceSumasYSaldosResponse(
        List<BalanceSumasYSaldosNodo> raices,
        BigDecimal totalDebe,
        BigDecimal totalHaber,
        boolean balancea,
        BigDecimal diferencia
) {}

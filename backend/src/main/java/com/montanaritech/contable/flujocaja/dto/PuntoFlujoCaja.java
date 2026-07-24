package com.montanaritech.contable.flujocaja.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Un bucket (día/semana/mes) de la serie de flujo de caja (F8.3). */
public record PuntoFlujoCaja(
        LocalDate fecha,
        BigDecimal saldoInicial,
        BigDecimal ingresos,
        BigDecimal egresos,
        BigDecimal saldoFinal,
        boolean esReal,
        boolean saldoNegativo
) {}

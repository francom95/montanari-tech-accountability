package com.montanaritech.contable.maestros.cuentabancaria.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuentaBancariaResponse(
        Long id,
        String entidad,
        String alias,
        Long monedaId,
        String monedaCodigo,
        String tipo,
        String estadoConciliacion,
        BigDecimal saldoInicial,
        LocalDate fechaSaldoInicial,
        BigDecimal saldoActual,
        Long cuentaContableId,
        String cuentaContableCodigo,
        boolean activo
) {}

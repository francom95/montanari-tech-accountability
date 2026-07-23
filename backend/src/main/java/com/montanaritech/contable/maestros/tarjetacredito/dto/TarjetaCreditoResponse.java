package com.montanaritech.contable.maestros.tarjetacredito.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TarjetaCreditoResponse(
        Long id,
        String entidad,
        Long monedaId,
        String monedaCodigo,
        int diaCierre,
        int diaVencimiento,
        Long cuentaBancariaDebitoId,
        String cuentaBancariaDebitoAlias,
        Long cuentaContableId,
        String cuentaContableCodigo,
        BigDecimal saldoInicial,
        LocalDate fechaSaldoInicial,
        BigDecimal saldoActual,
        boolean activo
) {}

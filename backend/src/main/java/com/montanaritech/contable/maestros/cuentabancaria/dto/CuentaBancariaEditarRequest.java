package com.montanaritech.contable.maestros.cuentabancaria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CuentaBancariaEditarRequest(
        @NotBlank String entidad,
        @NotBlank String alias,
        @NotNull Long monedaId,
        @NotBlank String tipo,
        String estadoConciliacion,
        @NotNull BigDecimal saldoInicial,
        @NotNull LocalDate fechaSaldoInicial,
        @NotNull Long cuentaContableId
) {}

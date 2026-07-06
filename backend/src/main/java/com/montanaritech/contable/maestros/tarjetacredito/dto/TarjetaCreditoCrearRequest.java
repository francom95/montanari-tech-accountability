package com.montanaritech.contable.maestros.tarjetacredito.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TarjetaCreditoCrearRequest(
        @NotBlank String entidad,
        @NotNull Long monedaId,
        @Min(1) @Max(31) int diaCierre,
        @Min(1) @Max(31) int diaVencimiento,
        @NotNull Long cuentaBancariaDebitoId,
        @NotNull BigDecimal saldoInicial,
        @NotNull LocalDate fechaSaldoInicial
) {}

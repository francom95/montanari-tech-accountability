package com.montanaritech.contable.bancos.tarjetacredito.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Fila devuelta por previsualizar tal como la confirma el usuario (F5.4) — {@code hash} viaja tal cual, no se recalcula. */
public record ConsumoImportacionConfirmarRequest(
        @NotNull LocalDate fecha,
        @NotBlank String descripcion,
        @NotNull BigDecimal importe,
        @NotBlank String monedaCodigo,
        String referencia,
        @NotBlank String hash
) {}

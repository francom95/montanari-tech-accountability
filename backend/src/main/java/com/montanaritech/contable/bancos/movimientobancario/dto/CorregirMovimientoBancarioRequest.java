package com.montanaritech.contable.bancos.movimientobancario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Corrección de un movimiento todavía {@code PENDIENTE} (F5.1) — mismos campos que la carga. */
public record CorregirMovimientoBancarioRequest(
        @NotNull Long cuentaBancariaId,
        @NotNull LocalDate fecha,
        @NotBlank String descripcion,
        @NotNull BigDecimal importe,
        @NotNull Long monedaId,
        @NotNull BigDecimal tipoCambio,
        String referencia,
        Long cuentaContableSugeridaId,
        String observaciones
) {}

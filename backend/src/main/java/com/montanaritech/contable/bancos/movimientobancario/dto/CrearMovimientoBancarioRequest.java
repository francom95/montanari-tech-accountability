package com.montanaritech.contable.bancos.movimientobancario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Carga manual de un movimiento bancario (F5.1; F5.2 agregará parsers que
 * llenan estos mismos campos desde un resumen). {@code importe} lleva signo
 * (positivo = ingreso, negativo = egreso). {@code cuentaContableSugeridaId}
 * es opcional: sin ella, la única acción de resolución disponible es
 * "imputar" (elegir cuenta en el momento) — "confirmar" exige que ya
 * exista una sugerencia.
 */
public record CrearMovimientoBancarioRequest(
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

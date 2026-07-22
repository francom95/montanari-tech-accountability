package com.montanaritech.contable.bancos.movimientobancario.dto;

import com.montanaritech.contable.bancos.movimientobancario.OrigenImportacionMovimiento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Carga manual de un movimiento bancario (F5.1) o vía parsers de resúmenes
 * (F5.2). {@code importe} lleva signo (positivo = ingreso, negativo =
 * egreso). {@code cuentaContableSugeridaId} es opcional: sin ella, la única
 * acción de resolución disponible es "imputar" (elegir cuenta en el
 * momento) — "confirmar" exige que ya exista una sugerencia. {@code fecha}
 * es opcional: algunos orígenes de importación (ej. Galicia ARS) no traen
 * fecha en todas las filas — queda pendiente de completar con "corregir"
 * antes de poder confirmar/imputar.
 */
public record CrearMovimientoBancarioRequest(
        @NotNull Long cuentaBancariaId,
        LocalDate fecha,
        @NotBlank String descripcion,
        @NotNull BigDecimal importe,
        @NotNull Long monedaId,
        @NotNull BigDecimal tipoCambio,
        String referencia,
        Long cuentaContableSugeridaId,
        String observaciones,
        /** Nulo = carga manual (MANUAL). Los parsers de F5.2 setean el origen real. */
        OrigenImportacionMovimiento origenImportacion,
        /** Solo usado por los parsers de F5.2 para detectar duplicados al re-importar; nulo en carga manual. */
        String hashImportacion
) {}

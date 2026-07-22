package com.montanaritech.contable.bancos.importacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fila devuelta por {@code previsualizar} tal como la confirma el usuario
 * (F5.2). {@code fecha} puede seguir nula (ej. Galicia ARS): el movimiento
 * entra igual a la bandeja de F5.1, a completar con "corregir". {@code hash}
 * viaja tal cual vino de la previsualización — no se recalcula acá.
 */
public record FilaImportacionConfirmarRequest(
        LocalDate fecha,
        @NotBlank String descripcion,
        @NotNull BigDecimal importe,
        String monedaCodigo,
        String referencia,
        @NotBlank String hash
) {}

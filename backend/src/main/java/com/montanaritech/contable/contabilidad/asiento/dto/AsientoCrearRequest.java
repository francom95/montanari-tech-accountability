package com.montanaritech.contable.contabilidad.asiento.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * Crea siempre un BORRADOR (F3.1 §3.5): ninguna validación contable corre
 * acá — puede tener 0 líneas, desbalancear, o referenciar cuentas madre;
 * todo eso se valida recién al confirmar.
 */
public record AsientoCrearRequest(
        @NotNull LocalDate fecha,
        @NotBlank String descripcion,
        String observaciones,
        @Valid List<AsientoLineaRequest> lineas
) {}

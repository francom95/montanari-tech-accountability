package com.montanaritech.contable.contabilidad.cuentacontable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CuentaContableEditarRequest(
        @NotBlank @Size(max = 20) String codigo,
        @NotBlank @Size(max = 160) String nombre,
        Long padreId,
        @NotBlank String naturaleza,
        Long rubroId,
        @NotNull Boolean imputable,
        @NotBlank String saldoEsperado,
        Set<Long> proyectosUsoHabitualIds
) {}

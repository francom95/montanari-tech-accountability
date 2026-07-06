package com.montanaritech.contable.maestros.moneda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code codigo} no se edita: es la clave natural, se fija al crear. */
public record MonedaEditarRequest(
        @NotBlank @Size(max = 80)
        String nombre,

        @NotBlank @Size(max = 5)
        String simbolo
) {
}

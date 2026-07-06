package com.montanaritech.contable.maestros.moneda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MonedaCrearRequest(
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "Debe ser un código ISO 4217 de 3 letras mayúsculas (ej. ARS)")
        String codigo,

        @NotBlank @Size(max = 80)
        String nombre,

        @NotBlank @Size(max = 5)
        String simbolo
) {
}

package com.montanaritech.contable.maestros.cliente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClienteEditarRequest(
        @NotBlank String nombre,
        @NotNull Long jurisdiccionId,
        String contacto,
        String email,
        String telefono,
        Long cuentaCxcId
) {}

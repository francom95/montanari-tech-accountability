package com.montanaritech.contable.maestros.cliente.dto;

import com.montanaritech.contable.common.validation.CuitValido;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClienteCrearRequest(
        @NotBlank String nombre,
        @NotBlank @CuitValido String cuit,
        @NotNull Long jurisdiccionId,
        String contacto,
        String email,
        String telefono
) {}

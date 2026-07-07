package com.montanaritech.contable.maestros.comisionista.dto;

import com.montanaritech.contable.common.validation.CuitValido;
import jakarta.validation.constraints.NotBlank;

public record ComisionistaEditarRequest(
        @NotBlank String nombre,
        @CuitValido String cuit,
        String contacto,
        String email,
        String telefono
) {}

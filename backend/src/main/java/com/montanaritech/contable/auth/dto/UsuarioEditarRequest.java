package com.montanaritech.contable.auth.dto;

import com.montanaritech.contable.auth.RolUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioEditarRequest(
        @NotBlank String nombre,
        @NotNull RolUsuario rol
) {
}

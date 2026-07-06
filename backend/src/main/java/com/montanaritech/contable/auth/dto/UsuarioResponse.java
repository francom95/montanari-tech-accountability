package com.montanaritech.contable.auth.dto;

import com.montanaritech.contable.auth.RolUsuario;
import java.time.Instant;

public record UsuarioResponse(
        Long id,
        String email,
        String nombre,
        RolUsuario rol,
        boolean activo,
        Instant ultimoLoginEn
) {
}

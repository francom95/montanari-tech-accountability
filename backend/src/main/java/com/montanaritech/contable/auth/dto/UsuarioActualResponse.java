package com.montanaritech.contable.auth.dto;

import com.montanaritech.contable.auth.RolUsuario;

/** Respuesta de GET /auth/me — lo que el frontend necesita para saber quién está logueado y con qué rol. */
public record UsuarioActualResponse(Long id, String email, String nombre, RolUsuario rol) {
}

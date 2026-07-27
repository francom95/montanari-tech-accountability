package com.montanaritech.contable.common.auth;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Lee el rol del usuario autenticado actual fuera de {@code @PreAuthorize}
 * (F9.3, extraído de {@code AsientoService.esAdmin()} de F3.5): útil cuando
 * la regla depende de un dato que {@code @PreAuthorize} no puede expresar
 * ("admin siempre puede, pero solo con confirmación explícita").
 */
public final class RolActualUtil {

    private RolActualUtil() {
    }

    public static boolean esAdmin() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }
}

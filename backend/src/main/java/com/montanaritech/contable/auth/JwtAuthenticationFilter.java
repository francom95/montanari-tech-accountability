package com.montanaritech.contable.auth;

import com.montanaritech.contable.common.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Valida el access token JWT (si viene) y arma la {@link Authentication} en
 * el contexto de seguridad directamente desde sus claims, sin volver a
 * tocar la base de datos en cada request. También puebla
 * {@link TenantContext} desde el claim {@code tenantId} — es lo que F1.3
 * dejó pendiente para este paso (antes se resolvía siempre al tenant 1).
 *
 * <p>Si no hay token, o es inválido/vencido, sigue la cadena sin autenticar
 * (el resto de la config de seguridad decide si el endpoint requiere auth).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length());
            try {
                Claims claims = jwtService.parsearClaims(token);

                String rol = claims.get("rol", String.class);
                Long tenantId = claims.get("tenantId", Long.class);
                TenantContext.setTenantId(tenantId);

                Authentication autenticacion = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + rol))
                );
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            } catch (JwtException | IllegalArgumentException ignored) {
                // Token ausente/inválido/vencido: se sigue sin autenticar; el
                // entry point de SecurityConfig responde 401 si el endpoint lo exige.
            }
        }

        filterChain.doFilter(request, response);
    }
}

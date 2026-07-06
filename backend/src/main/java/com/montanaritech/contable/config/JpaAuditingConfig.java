package com.montanaritech.contable.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Completa {@code creado_por}/{@code actualizado_por} de {@code EntidadNegocio}.
 * Hasta F1.5 no hay autenticación real, así que cae siempre a "system".
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
            if (autenticacion == null || !autenticacion.isAuthenticated()
                    || "anonymousUser".equals(autenticacion.getPrincipal())) {
                return Optional.of("system");
            }
            return Optional.of(autenticacion.getName());
        };
    }
}

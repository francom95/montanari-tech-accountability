package com.montanaritech.contable.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

/**
 * Cadena de seguridad mínima de scaffolding: solo Actuator health/info y
 * Swagger quedan públicos. NO hay mecanismo de autenticación todavía (JWT
 * llega en F1.5) — por eso {@code anyRequest().authenticated()} sin ningún
 * filtro que autentique significa que, hasta F1.5, cualquier otro endpoint
 * responde 401. Es el comportamiento esperado en este paso, no un bug.
 *
 * <p>El {@link PasswordEncoder} (BCrypt, fijado por el stack en F1.1) se
 * declara acá porque es una decisión ya tomada, no un diseño nuevo; F1.5 lo
 * inyecta para hashear/verificar contraseñas.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                // Sin este entry point explícito, Spring Security responde 403
                // (no 401) ante un request no autenticado cuando no hay ningún
                // mecanismo de challenge (httpBasic/formLogin) configurado.
                // 401 es lo semánticamente correcto acá: "no autenticado", no
                // "autenticado pero sin permiso". F1.5 puede reemplazarlo por
                // uno que devuelva el mismo formato ProblemDetail del resto de errores.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

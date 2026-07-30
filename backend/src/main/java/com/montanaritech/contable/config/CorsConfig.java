package com.montanaritech.contable.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS explícito para /api/**. Sin esto, cualquier navegador bloquea el
 * preflight cuando el frontend corre en un origen distinto al backend — el
 * caso siempre en dev (5173 vs 8081). En prod, si nginx.prod.conf sirve el
 * frontend y proxya /api/ bajo el mismo dominio (el caso normal), el browser
 * nunca hace una llamada cross-origin y esta config no entra en juego; queda
 * disponible igual por si alguna vez el API se llama desde otro origen.
 *
 * app.cors.allowed-origins es una lista separada por comas (dev trae un
 * default de conveniencia; prod queda vacío por default — fail-closed: sin
 * configurar, ningún origen cross-origin es permitido, en vez de fail-open
 * con un valor de relleno que termine autorizando algo no intencional).
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") String allowedOriginsCsv) {
        List<String> allowedOrigins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // Sin cookies de sesión (auth es JWT en el header Authorization) — no hace
        // falta permitir credenciales, y no permitirlas evita tener que restringir
        // más todavía el resto de la configuración por el combo credentials+origen.
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}

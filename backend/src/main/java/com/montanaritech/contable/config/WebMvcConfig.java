package com.montanaritech.contable.config;

import com.montanaritech.contable.common.tenant.TenantFilterInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantFilterInterceptor tenantFilterInterceptor;

    /**
     * F11.2 B2: {@code OpenEntityManagerInViewInterceptor} (auto-configurado por Spring Boot vía
     * {@code JpaBaseConfiguration.JpaWebConfiguration}) abre y liga el {@code EntityManager} real
     * de la request en su propio {@code preHandle}, registrado con orden por defecto (0) — igual
     * que {@link TenantFilterInterceptor} si no se fija un orden acá. Sin un orden explícito
     * mayor, este interceptor corría ANTES que OSIV (el orden entre configurers con el mismo
     * valor depende de en qué secuencia Spring procesa los beans {@code WebMvcConfigurer}, y en la
     * práctica el propio ganaba), habilitando el filtro Hibernate {@code tenantFilter} sobre un
     * {@code EntityManager} efímero que el {@code SharedEntityManagerCreator} descarta de
     * inmediato — el que OSIV liga después para el resto del request nunca tenía el filtro
     * habilitado. Confirmado en vivo: antes de este fix, {@code GET /clientes} devolvía los 19
     * clientes reales de otro tenant pese a que el filtro "estaba habilitado". Un valor positivo
     * cualquiera alcanza para correr después de OSIV.
     */
    private static final int ORDEN_DESPUES_DE_OPEN_ENTITY_MANAGER_IN_VIEW = 10;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantFilterInterceptor).order(ORDEN_DESPUES_DE_OPEN_ENTITY_MANAGER_IN_VIEW);
    }
}

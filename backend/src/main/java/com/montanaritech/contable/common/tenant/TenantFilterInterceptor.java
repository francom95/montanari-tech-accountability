package com.montanaritech.contable.common.tenant;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Habilita el filtro Hibernate {@code tenantFilter} (definido en
 * {@link EntidadNegocio}) para la sesión de persistencia de cada request,
 * usando el tenant resuelto en {@link TenantContext}.
 *
 * <p>Requiere {@code spring.jpa.open-in-view=true} (ver application.yml):
 * necesita que exista una sesión de Hibernate abierta y ligada al hilo antes
 * de que se ejecute cualquier query del controller/service.
 *
 * <p>F1.5 debe poblar {@link TenantContext} desde el claim {@code tenant} del
 * JWT en su propio filtro, ejecutado antes que este interceptor en la cadena.
 * Hasta entonces, todo request opera sobre el tenant por defecto.
 */
@Component
@RequiredArgsConstructor
public class TenantFilterInterceptor implements HandlerInterceptor {

    private final EntityManager entityManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("tenantFilter").setParameter("tenantId", TenantContext.getTenantId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}

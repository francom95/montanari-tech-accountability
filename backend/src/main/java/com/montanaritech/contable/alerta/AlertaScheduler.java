package com.montanaritech.contable.alerta;

import com.montanaritech.contable.common.tenant.Tenant;
import com.montanaritech.contable.common.tenant.TenantContext;
import com.montanaritech.contable.common.tenant.TenantRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dispara {@link MotorAlertasService#sincronizar()} una vez por día para
 * cada tenant (F9.1, primer uso de {@code @Scheduled} del proyecto). A
 * diferencia de un request HTTP, este job no pasa por
 * {@code TenantFilterInterceptor} — hay que habilitar el filtro Hibernate
 * de tenant a mano acá, igual que hace el interceptor por request.
 */
@Component
@RequiredArgsConstructor
public class AlertaScheduler {

    private final TenantRepository tenantRepo;
    private final MotorAlertasService motor;
    private final EntityManager entityManager;

    @Scheduled(cron = "0 0 7 * * *")
    public void sincronizarTodosLosTenants() {
        for (Tenant tenant : tenantRepo.findAll()) {
            sincronizarTenant(tenant.getId());
        }
    }

    @Transactional
    public void sincronizarTenant(Long tenantId) {
        try {
            TenantContext.setTenantId(tenantId);
            entityManager.unwrap(Session.class).enableFilter("tenantFilter").setParameter("tenantId", tenantId);
            motor.sincronizar();
        } finally {
            TenantContext.clear();
        }
    }
}

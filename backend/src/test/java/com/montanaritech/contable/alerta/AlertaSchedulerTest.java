package com.montanaritech.contable.alerta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.tenant.Tenant;
import com.montanaritech.contable.common.tenant.TenantContext;
import com.montanaritech.contable.common.tenant.TenantRepository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** F9.1: el job programado no debe mezclar tenants entre sí (el filtro Hibernate y TenantContext se resetean por tenant). */
@ExtendWith(MockitoExtension.class)
class AlertaSchedulerTest {

    @Mock private TenantRepository tenantRepo;
    @Mock private MotorAlertasService motor;
    @Mock private EntityManager entityManager;

    private AlertaScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AlertaScheduler(tenantRepo, motor, entityManager);
    }

    private Tenant tenant(Long id) {
        Tenant t = new Tenant();
        t.setId(id);
        return t;
    }

    @Test
    void sincronizarTodosLosTenantsNoMezclaElTenantIdEntreCorridas() {
        when(tenantRepo.findAll()).thenReturn(List.of(tenant(1L), tenant(2L)));

        Session session = mock(Session.class);
        Filter filter = mock(Filter.class);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter("tenantFilter")).thenReturn(filter);
        when(filter.setParameter(eq("tenantId"), org.mockito.ArgumentMatchers.any())).thenReturn(filter);

        List<Long> tenantIdVistoPorElMotor = new ArrayList<>();
        doAnswer(inv -> {
            tenantIdVistoPorElMotor.add(TenantContext.getTenantId());
            return null;
        }).when(motor).sincronizar();

        scheduler.sincronizarTodosLosTenants();

        assertThat(tenantIdVistoPorElMotor).containsExactly(1L, 2L);
        verify(filter).setParameter("tenantId", 1L);
        verify(filter).setParameter("tenantId", 2L);
        // Después de cada corrida se limpia: el hilo vuelve al tenant por defecto, no arrastra el último tenant.
        assertThat(TenantContext.getTenantId()).isEqualTo(TenantContext.TENANT_POR_DEFECTO);
    }
}

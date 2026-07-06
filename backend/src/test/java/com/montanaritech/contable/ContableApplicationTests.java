package com.montanaritech.contable;

import static org.assertj.core.api.Assertions.assertThat;

import com.montanaritech.contable.common.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Smoke test de F1.3: el contexto levanta contra MySQL real, Flyway corre
 * V1 sin errores, y el tenant semilla (Montanari Tech) queda disponible.
 * Hibernate en modo {@code validate} además garantiza que las entidades
 * scaffoldeadas (Tenant, Usuario, AuditoriaLog) coinciden con la migración.
 */
class ContableApplicationTests extends AbstractIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void elContextoLevantaYElTenantSemillaExiste() {
        assertThat(tenantRepository.findAll())
                .anySatisfy(tenant -> assertThat(tenant.getNombre()).isEqualTo("Montanari Tech"));
    }
}

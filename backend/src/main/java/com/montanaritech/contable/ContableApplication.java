package com.montanaritech.contable;

import com.montanaritech.contable.common.tenant.TenantScopedRepositoryImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @EnableScheduling} (F9.1): primer job programado del proyecto, el motor de alertas diario.
 *
 * <p>{@code @EnableJpaRepositories(repositoryBaseClass = ...)} (F11.2 B2): reemplaza la
 * implementación base autoconfigurada de todos los repositorios Spring Data por
 * {@link TenantScopedRepositoryImpl}, que cierra el aislamiento de tenant en el acceso
 * por id — ver el javadoc de esa clase.
 */
@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.montanaritech.contable", repositoryBaseClass = TenantScopedRepositoryImpl.class)
public class ContableApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContableApplication.class, args);
    }
}

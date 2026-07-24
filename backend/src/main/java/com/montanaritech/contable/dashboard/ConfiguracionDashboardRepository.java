package com.montanaritech.contable.dashboard;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionDashboardRepository extends JpaRepository<ConfiguracionDashboard, Long> {

    /** Una sola fila por tenant; el seed de V35 la crea. */
    Optional<ConfiguracionDashboard> findFirstByOrderByIdAsc();
}

package com.montanaritech.contable.alerta;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionAlertasRepository extends JpaRepository<ConfiguracionAlertas, Long> {

    /** Una sola fila por tenant; el seed de V40 la crea. */
    Optional<ConfiguracionAlertas> findFirstByOrderByIdAsc();
}

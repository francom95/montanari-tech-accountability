package com.montanaritech.contable.impuestos.atribucion;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionAtribucionRepository extends JpaRepository<ConfiguracionAtribucion, Long> {

    /** Una sola fila por tenant; el seed de V30 la crea. */
    Optional<ConfiguracionAtribucion> findFirstByOrderByIdAsc();
}

package com.montanaritech.contable.maestros.proyecto.presupuesto;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionPresupuestoRepository extends JpaRepository<ConfiguracionPresupuesto, Long> {

    /** Una sola fila por tenant; el seed de V33 la crea. */
    Optional<ConfiguracionPresupuesto> findFirstByOrderByIdAsc();
}

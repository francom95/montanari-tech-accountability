package com.montanaritech.contable.maestros.tipocambio;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionTipoCambioRepository extends JpaRepository<ConfiguracionTipoCambio, Long> {

    /** Una sola fila por tenant; el seed de V34 la crea. */
    Optional<ConfiguracionTipoCambio> findFirstByOrderByIdAsc();
}

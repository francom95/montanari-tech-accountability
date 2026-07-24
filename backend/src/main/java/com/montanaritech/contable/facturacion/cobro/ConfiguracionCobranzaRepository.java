package com.montanaritech.contable.facturacion.cobro;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionCobranzaRepository extends JpaRepository<ConfiguracionCobranza, Long> {

    /** Una sola fila por tenant; el seed de V34 la crea. */
    Optional<ConfiguracionCobranza> findFirstByOrderByIdAsc();
}

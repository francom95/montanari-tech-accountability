package com.montanaritech.contable.contabilidad.asiento;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AsientoLineaRepository extends JpaRepository<AsientoLinea, Long> {

    /** Punto de extensión que F3.1 dejó pendiente en {@code CuentaContableService.tieneMovimientos}. */
    boolean existsByCuentaContableId(Long cuentaContableId);
}

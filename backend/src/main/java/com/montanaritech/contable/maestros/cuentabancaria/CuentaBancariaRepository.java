package com.montanaritech.contable.maestros.cuentabancaria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuentaBancariaRepository extends JpaRepository<CuentaBancaria, Long> {
    @Query("""
            SELECT c FROM CuentaBancaria c
            WHERE (:texto IS NULL OR LOWER(c.entidad) LIKE LOWER(CONCAT('%', :texto, '%'))
                                   OR LOWER(c.alias) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:activo IS NULL OR c.activo = :activo)
            """)
    Page<CuentaBancaria> buscar(@Param("texto") String texto, @Param("activo") Boolean activo, Pageable pageable);
}

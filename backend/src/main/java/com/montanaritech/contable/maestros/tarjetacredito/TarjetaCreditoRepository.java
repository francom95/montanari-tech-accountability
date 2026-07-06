package com.montanaritech.contable.maestros.tarjetacredito;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TarjetaCreditoRepository extends JpaRepository<TarjetaCredito, Long> {
    @Query("""
            SELECT t FROM TarjetaCredito t
            WHERE (:texto IS NULL OR LOWER(t.entidad) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:activo IS NULL OR t.activo = :activo)
            """)
    Page<TarjetaCredito> buscar(@Param("texto") String texto, @Param("activo") Boolean activo, Pageable pageable);
}

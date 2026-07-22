package com.montanaritech.contable.maestros.cliente;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    @Query("""
            SELECT c FROM Cliente c
            WHERE (:texto IS NULL OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
                                   OR LOWER(c.cuit) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:activo IS NULL OR c.activo = :activo)
            """)
    Page<Cliente> buscar(@Param("texto") String texto, @Param("activo") Boolean activo, Pageable pageable);

    /** Resolución por CUIT (F4.6): el importador matchea la contraparte extraída del PDF contra un cliente existente. */
    Optional<Cliente> findByCuit(String cuit);
}

package com.montanaritech.contable.maestros.comisionista;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComisionistaRepository extends JpaRepository<Comisionista, Long> {
    @Query("""
            SELECT c FROM Comisionista c
            WHERE (:texto IS NULL OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
                                   OR LOWER(c.cuit) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:activo IS NULL OR c.activo = :activo)
            """)
    Page<Comisionista> buscar(@Param("texto") String texto, @Param("activo") Boolean activo, Pageable pageable);
}

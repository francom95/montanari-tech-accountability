package com.montanaritech.contable.maestros.moneda;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MonedaRepository extends JpaRepository<Moneda, Long> {

    Optional<Moneda> findByCodigo(String codigo);

    /**
     * Filtros opcionales (patrón "{@code :param IS NULL OR ...}", ya usado
     * en {@code AuditoriaLogRepository} de F1.6): mismo texto busca en
     * código y nombre.
     */
    @Query("""
            SELECT m FROM Moneda m
            WHERE (:texto IS NULL OR LOWER(m.codigo) LIKE LOWER(CONCAT('%', :texto, '%'))
                                   OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:activo IS NULL OR m.activo = :activo)
            """)
    Page<Moneda> buscar(@Param("texto") String texto, @Param("activo") Boolean activo, Pageable pageable);
}

package com.montanaritech.contable.compromiso;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompromisoRepository extends JpaRepository<Compromiso, Long> {

    @Query("""
            SELECT c FROM Compromiso c
            WHERE (:texto IS NULL OR LOWER(c.concepto) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:estado IS NULL OR c.estado = :estado)
              AND (:activo IS NULL OR c.activo = :activo)
            ORDER BY c.fechaPrevista ASC
            """)
    Page<Compromiso> buscar(@Param("texto") String texto, @Param("estado") EstadoCompromiso estado,
            @Param("activo") Boolean activo, Pageable pageable);

    /** F8.2: query service simple para que F8.3 proyecte los compromisos de un período. */
    List<Compromiso> findByFechaPrevistaBetweenOrderByFechaPrevistaAsc(LocalDate desde, LocalDate hasta);
}

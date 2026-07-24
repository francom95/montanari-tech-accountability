package com.montanaritech.contable.maestros.proyecto.etapa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EtapaRepository extends JpaRepository<Etapa, Long> {

    boolean existsByProyectoId(Long proyectoId);

    Optional<Etapa> findByIdAndProyectoId(Long id, Long proyectoId);

    /** Todas las etapas de un proyecto, sin paginar (F7.4, reporte de rentabilidad). */
    List<Etapa> findByProyectoIdOrderByFechaInicioAsc(Long proyectoId);

    @Query("""
            SELECT e FROM Etapa e
            WHERE e.proyecto.id = :proyectoId
              AND (:texto IS NULL OR LOWER(e.nombre) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:activo IS NULL OR e.activo = :activo)
            """)
    Page<Etapa> buscar(
            @Param("proyectoId") Long proyectoId,
            @Param("texto") String texto,
            @Param("activo") Boolean activo,
            Pageable pageable);
}

package com.montanaritech.contable.maestros.proyecto.etapa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EtapaRepository extends JpaRepository<Etapa, Long> {

    boolean existsByProyectoId(Long proyectoId);

    /** Búsqueda global (F9.2, término TEXTO): sin acotar a un proyecto, a diferencia de {@link #buscar}. */
    @Query("""
            SELECT e FROM Etapa e
            WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<Etapa> buscarGlobalPorTexto(@Param("texto") String texto, Pageable pageable);

    /** Búsqueda global (F9.2, término FECHA): inicio o fin estimado. */
    Page<Etapa> findByFechaInicioOrFechaEstimadaFin(LocalDate fecha1, LocalDate fecha2, Pageable pageable);

    /** Búsqueda global (F9.2, término IMPORTE): presupuestado o costo estimado dentro de la tolerancia. */
    @Query("""
            SELECT e FROM Etapa e
            WHERE e.montoPresupuestado BETWEEN :desde AND :hasta OR e.costosEstimados BETWEEN :desde AND :hasta
            """)
    Page<Etapa> buscarGlobalPorImporte(@Param("desde") BigDecimal desde, @Param("hasta") BigDecimal hasta, Pageable pageable);

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

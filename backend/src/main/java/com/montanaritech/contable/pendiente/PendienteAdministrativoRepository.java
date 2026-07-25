package com.montanaritech.contable.pendiente;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PendienteAdministrativoRepository extends JpaRepository<PendienteAdministrativo, Long> {

    @Query("""
            SELECT p FROM PendienteAdministrativo p
            WHERE (:texto IS NULL OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:estado IS NULL OR p.estado = :estado)
              AND (:prioridad IS NULL OR p.prioridad = :prioridad)
              AND (:responsableId IS NULL OR p.responsable.id = :responsableId)
              AND (:categoria IS NULL OR LOWER(p.categoria) = LOWER(:categoria))
              AND (:activo IS NULL OR p.activo = :activo)
            ORDER BY p.fechaEstimadaResolucion ASC, p.id DESC
            """)
    Page<PendienteAdministrativo> buscar(@Param("texto") String texto, @Param("estado") EstadoPendiente estado,
            @Param("prioridad") PrioridadPendiente prioridad, @Param("responsableId") Long responsableId,
            @Param("categoria") String categoria, @Param("activo") Boolean activo, Pageable pageable);

    /** F8.5: query service para que F9.1 (alertas) consuma los pendientes próximos a vencer. */
    List<PendienteAdministrativo> findByEstadoAndFechaEstimadaResolucionLessThanEqualOrderByFechaEstimadaResolucionAsc(
            EstadoPendiente estado, LocalDate limite);

    /** Búsqueda global (F9.2, término FECHA). */
    Page<PendienteAdministrativo> findByFechaEstimadaResolucion(LocalDate fecha, Pageable pageable);
}

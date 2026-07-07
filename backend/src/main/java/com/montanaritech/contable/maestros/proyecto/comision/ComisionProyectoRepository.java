package com.montanaritech.contable.maestros.proyecto.comision;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComisionProyectoRepository extends JpaRepository<ComisionProyecto, Long> {

    boolean existsByComisionistaId(Long comisionistaId);

    Optional<ComisionProyecto> findByIdAndProyectoId(Long id, Long proyectoId);

    List<ComisionProyecto> findByProyectoIdAndActivoTrue(Long proyectoId);

    @Query("""
            SELECT c FROM ComisionProyecto c
            WHERE c.proyecto.id = :proyectoId
              AND (:activo IS NULL OR c.activo = :activo)
            """)
    Page<ComisionProyecto> buscarPorProyecto(
            @Param("proyectoId") Long proyectoId,
            @Param("activo") Boolean activo,
            Pageable pageable);

    @Query("""
            SELECT c FROM ComisionProyecto c
            WHERE (:proyectoId IS NULL OR c.proyecto.id = :proyectoId)
              AND (:comisionistaId IS NULL OR c.comisionista.id = :comisionistaId)
              AND (:estadoPago IS NULL OR c.estadoPago = :estadoPago)
              AND (:desde IS NULL OR c.fechaEstimadaPago >= :desde)
              AND (:hasta IS NULL OR c.fechaEstimadaPago <= :hasta)
            """)
    Page<ComisionProyecto> consultar(
            @Param("proyectoId") Long proyectoId,
            @Param("comisionistaId") Long comisionistaId,
            @Param("estadoPago") ComisionProyecto.EstadoPago estadoPago,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            Pageable pageable);
}

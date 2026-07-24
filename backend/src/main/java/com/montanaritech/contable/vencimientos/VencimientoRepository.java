package com.montanaritech.contable.vencimientos;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VencimientoRepository extends JpaRepository<Vencimiento, Long> {

    /** Búsqueda avanzada (F8.1), mismo molde que {@code AsientoRepository.buscar}. */
    @Query("""
            SELECT v FROM Vencimiento v
            WHERE (:tipo IS NULL OR v.tipo = :tipo)
              AND (:estado IS NULL OR v.estado = :estado)
              AND (:fechaDesde IS NULL OR v.fecha >= :fechaDesde)
              AND (:fechaHasta IS NULL OR v.fecha <= :fechaHasta)
              AND (:proyectoId IS NULL OR v.proyecto.id = :proyectoId)
              AND (:proveedorId IS NULL OR v.proveedor.id = :proveedorId)
              AND (:tarjetaId IS NULL OR v.tarjetaCredito.id = :tarjetaId)
            ORDER BY v.fecha ASC
            """)
    Page<Vencimiento> buscar(
            @Param("tipo") TipoVencimiento tipo,
            @Param("estado") EstadoVencimientoObligacion estado,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("proyectoId") Long proyectoId,
            @Param("proveedorId") Long proveedorId,
            @Param("tarjetaId") Long tarjetaId,
            Pageable pageable);

    /** Idempotencia: liquidaciones de IVA/IIBB generan un único vencimiento por liquidación, para siempre. */
    boolean existsByOrigenGeneracionAndOrigenGeneracionRefId(OrigenGeneracionVencimiento origenGeneracion, Long origenGeneracionRefId);

    /** Idempotencia: tarjetas/conceptos recurrentes generan como máximo un vencimiento por fecha proyectada. */
    boolean existsByOrigenGeneracionAndOrigenGeneracionRefIdAndFecha(
            OrigenGeneracionVencimiento origenGeneracion, Long origenGeneracionRefId, LocalDate fecha);

    /** Idempotencia de conceptos recurrentes: ya existe un vencimiento del origen para ese rango (mes o año en curso). */
    boolean existsByOrigenGeneracionAndOrigenGeneracionRefIdAndFechaBetween(
            OrigenGeneracionVencimiento origenGeneracion, Long origenGeneracionRefId, LocalDate fechaDesde, LocalDate fechaHasta);

    /** Vencimientos manuales recurrentes ya resueltos (para encadenar la próxima ocurrencia en generarAutomaticos). */
    List<Vencimiento> findByOrigenGeneracionAndRecurrenciaNotAndEstadoIn(
            OrigenGeneracionVencimiento origenGeneracion, TipoRecurrencia recurrencia, List<EstadoVencimientoObligacion> estados);

    /** "Próximos vencimientos" (F9.1/F8.3): pendientes con fecha hasta el límite de la ventana, incluye los ya vencidos. */
    List<Vencimiento> findByEstadoAndFechaLessThanEqualOrderByFechaAsc(EstadoVencimientoObligacion estado, LocalDate fechaLimite);
}

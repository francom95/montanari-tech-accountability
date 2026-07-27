package com.montanaritech.contable.maestros.proyecto;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {
    java.util.List<Proyecto> findByActivoTrueOrderByNombreAsc();

    /** F10.2: idempotencia del importador de Proyecto (evita duplicar en una segunda corrida). */
    java.util.Optional<Proyecto> findByNombreIgnoreCaseAndClienteId(String nombre, Long clienteId);

    /** F10.2: resolución de Proyecto por nombre desde el importador de ComisionProyecto (la hoja no trae cliente). */
    java.util.Optional<Proyecto> findByNombreIgnoreCase(String nombre);

    /** Búsqueda global (F9.2, término FECHA): fecha estimada o real de finalización. */
    Page<Proyecto> findByFechaEstimadaFinalizacionOrFechaRealFinalizacion(LocalDate fecha1, LocalDate fecha2, Pageable pageable);

    /** Búsqueda global (F9.2, término IMPORTE): monto total dentro de la tolerancia. */
    Page<Proyecto> findByMontoTotalBetween(BigDecimal desde, BigDecimal hasta, Pageable pageable);

    @Query("""
            SELECT p FROM Proyecto p
            WHERE (:texto IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:activo IS NULL OR p.activo = :activo)
              AND (:clienteId IS NULL OR p.cliente.id = :clienteId)
            """)
    Page<Proyecto> buscar(
            @Param("texto") String texto,
            @Param("activo") Boolean activo,
            @Param("clienteId") Long clienteId,
            Pageable pageable);
}

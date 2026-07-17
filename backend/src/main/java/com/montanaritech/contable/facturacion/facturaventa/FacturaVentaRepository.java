package com.montanaritech.contable.facturacion.facturaventa;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacturaVentaRepository extends JpaRepository<FacturaVenta, Long> {

    @Query("""
            SELECT f FROM FacturaVenta f
            WHERE (:texto IS NULL OR LOWER(f.numero) LIKE LOWER(CONCAT('%', :texto, '%'))
                                   OR LOWER(f.cliente.nombre) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:estado IS NULL OR f.estado = :estado)
              AND (:clienteId IS NULL OR f.cliente.id = :clienteId)
              AND (:proyectoId IS NULL OR f.proyecto.id = :proyectoId)
              AND (:fechaDesde IS NULL OR f.fecha >= :fechaDesde)
              AND (:fechaHasta IS NULL OR f.fecha <= :fechaHasta)
            """)
    Page<FacturaVenta> buscar(
            @Param("texto") String texto,
            @Param("estado") EstadoDocumento estado,
            @Param("clienteId") Long clienteId,
            @Param("proyectoId") Long proyectoId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            Pageable pageable);
}

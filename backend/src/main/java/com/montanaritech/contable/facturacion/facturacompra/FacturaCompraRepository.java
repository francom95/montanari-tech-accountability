package com.montanaritech.contable.facturacion.facturacompra;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacturaCompraRepository extends JpaRepository<FacturaCompra, Long> {

    @Query("""
            SELECT f FROM FacturaCompra f
            WHERE (:texto IS NULL OR LOWER(f.numero) LIKE LOWER(CONCAT('%', :texto, '%'))
                                   OR LOWER(f.proveedor.nombre) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:estado IS NULL OR f.estado = :estado)
              AND (:proveedorId IS NULL OR f.proveedor.id = :proveedorId)
              AND (:proyectoId IS NULL OR f.proyecto.id = :proyectoId)
              AND (:fechaDesde IS NULL OR f.fecha >= :fechaDesde)
              AND (:fechaHasta IS NULL OR f.fecha <= :fechaHasta)
            """)
    Page<FacturaCompra> buscar(
            @Param("texto") String texto,
            @Param("estado") EstadoDocumento estado,
            @Param("proveedorId") Long proveedorId,
            @Param("proyectoId") Long proyectoId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            Pageable pageable);

    /** Facturas confirmadas para el reporte de cuentas por pagar (F4.5) — sin paginar, mismo criterio que el Mayor (F3.6). */
    @Query("""
            SELECT f FROM FacturaCompra f
            WHERE f.estado = com.montanaritech.contable.common.estado.EstadoDocumento.CONFIRMADO
              AND (:proveedorId IS NULL OR f.proveedor.id = :proveedorId)
              AND (:proyectoId IS NULL OR f.proyecto.id = :proyectoId)
              AND (:monedaId IS NULL OR f.moneda.id = :monedaId)
              AND (:fechaDesde IS NULL OR f.fecha >= :fechaDesde)
              AND (:fechaHasta IS NULL OR f.fecha <= :fechaHasta)
            ORDER BY f.fecha ASC, f.id ASC
            """)
    List<FacturaCompra> buscarConfirmadasParaReporte(
            @Param("proveedorId") Long proveedorId,
            @Param("proyectoId") Long proyectoId,
            @Param("monedaId") Long monedaId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta);
}

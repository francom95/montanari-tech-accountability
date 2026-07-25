package com.montanaritech.contable.facturacion.pago;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    /** Búsqueda global (F9.2, término TEXTO): sin campo de descripción propio, matchea proveedor u observaciones. */
    @Query("""
            SELECT p FROM Pago p
            WHERE LOWER(p.proveedor.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(p.observaciones) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<Pago> buscarGlobalPorTexto(@Param("texto") String texto, Pageable pageable);

    /** Búsqueda global (F9.2, término CUIT): vía el CUIT del proveedor. */
    Page<Pago> findByProveedor_Cuit(String cuit, Pageable pageable);

    /** Búsqueda global (F9.2, término FECHA). */
    Page<Pago> findByFecha(LocalDate fecha, Pageable pageable);

    /** Búsqueda global (F9.2, término IMPORTE): total (ARS) dentro de la tolerancia. */
    Page<Pago> findByTotalArsBetween(BigDecimal desde, BigDecimal hasta, Pageable pageable);

    @Query("""
            SELECT p FROM Pago p
            WHERE (:estado IS NULL OR p.estado = :estado)
              AND (:proveedorId IS NULL OR p.proveedor.id = :proveedorId)
              AND (:fechaDesde IS NULL OR p.fecha >= :fechaDesde)
              AND (:fechaHasta IS NULL OR p.fecha <= :fechaHasta)
            """)
    Page<Pago> buscar(
            @Param("estado") EstadoDocumento estado,
            @Param("proveedorId") Long proveedorId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            Pageable pageable);
}

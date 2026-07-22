package com.montanaritech.contable.facturacion.pago;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<Pago, Long> {

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

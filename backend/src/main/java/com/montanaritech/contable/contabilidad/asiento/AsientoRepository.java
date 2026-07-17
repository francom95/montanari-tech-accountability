package com.montanaritech.contable.contabilidad.asiento;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AsientoRepository extends JpaRepository<Asiento, Long> {

    /**
     * Búsqueda avanzada (F3.5): además del texto libre (descripción o leyenda
     * de línea) y el estado, filtra por fecha (rango), número, cuenta,
     * importe (debe o haber de cualquier línea), proyecto, cliente,
     * proveedor y origen. El {@code LEFT JOIN} sobre {@code lineas} es solo
     * para filtrar (no fetch), así que la paginación sigue siendo a nivel
     * SQL; {@code DISTINCT} evita duplicar la cabecera cuando varias líneas
     * matchean el mismo asiento.
     */
    @Query("""
            SELECT DISTINCT a FROM Asiento a
            LEFT JOIN a.lineas l
            WHERE (:texto IS NULL OR LOWER(a.descripcion) LIKE LOWER(CONCAT('%', :texto, '%'))
                                   OR LOWER(l.leyenda) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:estado IS NULL OR a.estado = :estado)
              AND (:origen IS NULL OR a.origen = :origen)
              AND (:numero IS NULL OR a.numero = :numero)
              AND (:fechaDesde IS NULL OR a.fecha >= :fechaDesde)
              AND (:fechaHasta IS NULL OR a.fecha <= :fechaHasta)
              AND (:cuentaContableId IS NULL OR l.cuentaContable.id = :cuentaContableId)
              AND (:importe IS NULL OR l.debe = :importe OR l.haber = :importe)
              AND (:proyectoId IS NULL OR l.proyecto.id = :proyectoId)
              AND (:clienteId IS NULL OR l.cliente.id = :clienteId)
              AND (:proveedorId IS NULL OR l.proveedor.id = :proveedorId)
            """)
    Page<Asiento> buscar(
            @Param("texto") String texto,
            @Param("estado") EstadoDocumento estado,
            @Param("origen") OrigenAsiento origen,
            @Param("numero") Long numero,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("cuentaContableId") Long cuentaContableId,
            @Param("importe") BigDecimal importe,
            @Param("proyectoId") Long proyectoId,
            @Param("clienteId") Long clienteId,
            @Param("proveedorId") Long proveedorId,
            Pageable pageable);
}

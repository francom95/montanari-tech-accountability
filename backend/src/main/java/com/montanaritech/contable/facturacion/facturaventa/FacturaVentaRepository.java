package com.montanaritech.contable.facturacion.facturaventa;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.facturacion.TipoComprobante;
import java.time.LocalDate;
import java.util.List;
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

    /** Facturas confirmadas para el reporte de cuentas por cobrar (F4.5) — sin paginar, mismo criterio que el Mayor (F3.6). */
    @Query("""
            SELECT f FROM FacturaVenta f
            WHERE f.estado = com.montanaritech.contable.common.estado.EstadoDocumento.CONFIRMADO
              AND (:clienteId IS NULL OR f.cliente.id = :clienteId)
              AND (:proyectoId IS NULL OR f.proyecto.id = :proyectoId)
              AND (:monedaId IS NULL OR f.moneda.id = :monedaId)
              AND (:fechaDesde IS NULL OR f.fecha >= :fechaDesde)
              AND (:fechaHasta IS NULL OR f.fecha <= :fechaHasta)
            ORDER BY f.fecha ASC, f.id ASC
            """)
    List<FacturaVenta> buscarConfirmadasParaReporte(
            @Param("clienteId") Long clienteId,
            @Param("proyectoId") Long proyectoId,
            @Param("monedaId") Long monedaId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta);

    /** Idempotencia del importador (F4.6, misma clave que el UK de la tabla: cliente+tipo+puntoVenta+numero). */
    boolean existsByClienteIdAndTipoComprobanteAndPuntoVentaAndNumero(
            Long clienteId, TipoComprobante tipoComprobante, String puntoVenta, String numero);

    /**
     * Ventas confirmadas del período para calcular la base imponible de IIBB
     * (F6.2 §1.2). Trae la jurisdicción de destino con FETCH para agrupar en
     * memoria (volumen acotado por período, mismo criterio que el Mayor). No se
     * lee de asientos como en IVA porque la jurisdicción vive en la factura, no
     * en la línea de asiento. El servicio decide el signo (las notas de crédito
     * restan de la base).
     */
    @Query("""
            SELECT f FROM FacturaVenta f
            LEFT JOIN FETCH f.jurisdiccionDestino j
            WHERE f.estado = com.montanaritech.contable.common.estado.EstadoDocumento.CONFIRMADO
              AND f.fecha BETWEEN :fechaDesde AND :fechaHasta
            ORDER BY f.fecha ASC, f.id ASC
            """)
    List<FacturaVenta> buscarConfirmadasParaBaseIibb(
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta);
}

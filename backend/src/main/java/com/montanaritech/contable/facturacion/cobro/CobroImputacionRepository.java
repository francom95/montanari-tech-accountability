package com.montanaritech.contable.facturacion.cobro;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.facturacion.cobro.dto.ImputadoFacturaVenta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CobroImputacionRepository extends JpaRepository<CobroImputacion, Long> {

    List<CobroImputacion> findByFacturaVenta_IdAndCobro_EstadoOrderByIdAsc(Long facturaVentaId, EstadoDocumento estado);

    /** Agregado en bloque para N facturas (F4.5, evita N+1 en el reporte de cuentas por cobrar). */
    @Query("""
            SELECT new com.montanaritech.contable.facturacion.cobro.dto.ImputadoFacturaVenta(
                ci.facturaVenta.id, SUM(ci.montoImputadoOriginal), SUM(ci.montoArsCancelado))
            FROM CobroImputacion ci
            WHERE ci.facturaVenta.id IN :facturaVentaIds AND ci.cobro.estado = :estado
            GROUP BY ci.facturaVenta.id
            """)
    List<ImputadoFacturaVenta> sumarImputadoPorFactura(
            @Param("facturaVentaIds") List<Long> facturaVentaIds, @Param("estado") EstadoDocumento estado);
}

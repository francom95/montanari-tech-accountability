package com.montanaritech.contable.facturacion.pago;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.facturacion.pago.dto.ImputadoFacturaCompra;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoImputacionRepository extends JpaRepository<PagoImputacion, Long> {

    List<PagoImputacion> findByFacturaCompra_IdAndPago_EstadoOrderByIdAsc(Long facturaCompraId, EstadoDocumento estado);

    /** Agregado en bloque para N facturas (F4.5, evita N+1 en el reporte de cuentas por pagar). */
    @Query("""
            SELECT new com.montanaritech.contable.facturacion.pago.dto.ImputadoFacturaCompra(
                pi.facturaCompra.id, SUM(pi.montoImputadoOriginal), SUM(pi.montoArsCancelado))
            FROM PagoImputacion pi
            WHERE pi.facturaCompra.id IN :facturaCompraIds AND pi.pago.estado = :estado
            GROUP BY pi.facturaCompra.id
            """)
    List<ImputadoFacturaCompra> sumarImputadoPorFactura(
            @Param("facturaCompraIds") List<Long> facturaCompraIds, @Param("estado") EstadoDocumento estado);
}

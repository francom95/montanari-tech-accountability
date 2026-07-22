package com.montanaritech.contable.facturacion.cobro;

import com.montanaritech.contable.facturacion.cobro.dto.ImputadoFacturaVenta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AplicacionAnticipoClienteRepository extends JpaRepository<AplicacionAnticipoCliente, Long> {

    List<AplicacionAnticipoCliente> findByFacturaVenta_IdOrderByIdAsc(Long facturaVentaId);

    List<AplicacionAnticipoCliente> findByCobro_IdOrderByIdAsc(Long cobroId);

    /** Agregado en bloque para N facturas (F4.5, evita N+1 en el reporte de cuentas por cobrar). */
    @Query("""
            SELECT new com.montanaritech.contable.facturacion.cobro.dto.ImputadoFacturaVenta(
                a.facturaVenta.id, SUM(a.montoOriginal), SUM(a.montoArsCancelado))
            FROM AplicacionAnticipoCliente a
            WHERE a.facturaVenta.id IN :facturaVentaIds
            GROUP BY a.facturaVenta.id
            """)
    List<ImputadoFacturaVenta> sumarAplicacionesPorFactura(@Param("facturaVentaIds") List<Long> facturaVentaIds);
}

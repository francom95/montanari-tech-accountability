package com.montanaritech.contable.facturacion.pago;

import com.montanaritech.contable.facturacion.pago.dto.ImputadoFacturaCompra;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AplicacionAnticipoProveedorRepository extends JpaRepository<AplicacionAnticipoProveedor, Long> {

    List<AplicacionAnticipoProveedor> findByFacturaCompra_IdOrderByIdAsc(Long facturaCompraId);

    List<AplicacionAnticipoProveedor> findByPago_IdOrderByIdAsc(Long pagoId);

    /** Agregado en bloque para N facturas (F4.5, evita N+1 en el reporte de cuentas por pagar). */
    @Query("""
            SELECT new com.montanaritech.contable.facturacion.pago.dto.ImputadoFacturaCompra(
                a.facturaCompra.id, SUM(a.montoOriginal), SUM(a.montoArsCancelado))
            FROM AplicacionAnticipoProveedor a
            WHERE a.facturaCompra.id IN :facturaCompraIds
            GROUP BY a.facturaCompra.id
            """)
    List<ImputadoFacturaCompra> sumarAplicacionesPorFactura(@Param("facturaCompraIds") List<Long> facturaCompraIds);
}

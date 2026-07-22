package com.montanaritech.contable.facturacion.pago;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AplicacionAnticipoProveedorRepository extends JpaRepository<AplicacionAnticipoProveedor, Long> {

    List<AplicacionAnticipoProveedor> findByFacturaCompra_IdOrderByIdAsc(Long facturaCompraId);

    List<AplicacionAnticipoProveedor> findByPago_IdOrderByIdAsc(Long pagoId);
}

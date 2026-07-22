package com.montanaritech.contable.facturacion.pago;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoImputacionRepository extends JpaRepository<PagoImputacion, Long> {

    List<PagoImputacion> findByFacturaCompra_IdAndPago_EstadoOrderByIdAsc(Long facturaCompraId, EstadoDocumento estado);
}

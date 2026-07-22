package com.montanaritech.contable.facturacion.cobro;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CobroImputacionRepository extends JpaRepository<CobroImputacion, Long> {

    List<CobroImputacion> findByFacturaVenta_IdAndCobro_EstadoOrderByIdAsc(Long facturaVentaId, EstadoDocumento estado);
}

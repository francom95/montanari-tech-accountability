package com.montanaritech.contable.facturacion.cobro;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AplicacionAnticipoClienteRepository extends JpaRepository<AplicacionAnticipoCliente, Long> {

    List<AplicacionAnticipoCliente> findByFacturaVenta_IdOrderByIdAsc(Long facturaVentaId);

    List<AplicacionAnticipoCliente> findByCobro_IdOrderByIdAsc(Long cobroId);
}

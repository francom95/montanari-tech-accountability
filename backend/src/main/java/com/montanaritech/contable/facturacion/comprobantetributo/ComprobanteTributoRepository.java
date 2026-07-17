package com.montanaritech.contable.facturacion.comprobantetributo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComprobanteTributoRepository extends JpaRepository<ComprobanteTributo, Long> {

    List<ComprobanteTributo> findByComprobanteTipoAndComprobanteIdOrderByIdAsc(ComprobanteTipo comprobanteTipo, Long comprobanteId);

    void deleteByComprobanteTipoAndComprobanteId(ComprobanteTipo comprobanteTipo, Long comprobanteId);
}

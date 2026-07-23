package com.montanaritech.contable.impuestos.atribucion;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtribucionImpuestoRepository extends JpaRepository<AtribucionImpuesto, Long> {

    Optional<AtribucionImpuesto> findByLiquidacionTipoAndLiquidacionId(TipoLiquidacion tipo, Long liquidacionId);

    /** Atribuciones de un período, para el reporte de rentabilidad por proyecto (F7.4). */
    List<AtribucionImpuesto> findByAnioAndMes(Integer anio, Integer mes);
}

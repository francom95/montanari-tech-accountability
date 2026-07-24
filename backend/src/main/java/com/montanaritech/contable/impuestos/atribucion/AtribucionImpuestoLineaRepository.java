package com.montanaritech.contable.impuestos.atribucion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtribucionImpuestoLineaRepository extends JpaRepository<AtribucionImpuestoLinea, Long> {

    /** Total histórico atribuido a un proyecto (F7.4): no se filtra por período, la rentabilidad es de vida del proyecto. */
    List<AtribucionImpuestoLinea> findByProyectoId(Long proyectoId);
}

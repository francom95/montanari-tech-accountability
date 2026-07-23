package com.montanaritech.contable.impuestos.iibb;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiquidacionIibbRepository extends JpaRepository<LiquidacionIibb, Long> {

    @Query("""
            SELECT l FROM LiquidacionIibb l
            WHERE (:anio IS NULL OR l.anio = :anio)
              AND (:estado IS NULL OR l.estado = :estado)
            ORDER BY l.anio DESC, l.mes DESC
            """)
    Page<LiquidacionIibb> buscar(@Param("anio") Integer anio,
                                 @Param("estado") EstadoDocumento estado,
                                 Pageable pageable);

    /** Liquidaciones "vivas" de un período: las que bloquean crear otra (F6.2 §1.6). */
    List<LiquidacionIibb> findByAnioAndMesAndEstadoIn(Integer anio, Integer mes, Collection<EstadoDocumento> estados);

    /** Arrastre por jurisdicción: el saldo a favor de esta es el anterior del mes siguiente. */
    Optional<LiquidacionIibb> findFirstByAnioAndMesAndEstado(Integer anio, Integer mes, EstadoDocumento estado);
}

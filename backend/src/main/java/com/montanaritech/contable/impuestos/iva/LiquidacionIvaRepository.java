package com.montanaritech.contable.impuestos.iva;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiquidacionIvaRepository extends JpaRepository<LiquidacionIva, Long> {

    @Query("""
            SELECT l FROM LiquidacionIva l
            WHERE (:anio IS NULL OR l.anio = :anio)
              AND (:estado IS NULL OR l.estado = :estado)
            ORDER BY l.anio DESC, l.mes DESC
            """)
    Page<LiquidacionIva> buscar(@Param("anio") Integer anio,
                                @Param("estado") EstadoDocumento estado,
                                Pageable pageable);

    /**
     * Liquidaciones "vivas" de un período: las que bloquean crear otra. Se
     * consulta por estados en vez de por unique constraint porque una
     * liquidación anulada debe poder rehacerse (F6.1 §1.7).
     */
    List<LiquidacionIva> findByAnioAndMesAndEstadoIn(Integer anio, Integer mes, Collection<EstadoDocumento> estados);

    /** Arrastre: el saldo a favor de esta es el saldo técnico anterior del mes siguiente. */
    Optional<LiquidacionIva> findFirstByAnioAndMesAndEstado(Integer anio, Integer mes, EstadoDocumento estado);
}

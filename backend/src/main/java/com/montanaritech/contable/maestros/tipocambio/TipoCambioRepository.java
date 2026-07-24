package com.montanaritech.contable.maestros.tipocambio;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TipoCambioRepository extends JpaRepository<TipoCambio, Long> {

    @Query("""
            SELECT tc FROM TipoCambio tc
            WHERE (:texto IS NULL OR LOWER(tc.criterio) LIKE LOWER(CONCAT('%', :texto, '%'))
                                   OR LOWER(tc.fuente) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:activo IS NULL OR tc.activo = :activo)
            """)
    Page<TipoCambio> buscar(@Param("texto") String texto, @Param("activo") Boolean activo, Pageable pageable);

    /**
     * Resolución automática de TC (F3.1 §3.4 ítem 3, CP-19): cuando una línea
     * en moneda extranjera no trae tipo de cambio manual, se busca una
     * cotización cargada para (moneda, fecha). No distingue por criterio
     * (BNA venta/compra, oficial, manual...) — toma la primera activa, de
     * forma determinística. Usado como fallback cuando no hay
     * {@code ConfiguracionTipoCambio.criterioPorDefecto} configurado, o
     * cuando ese criterio no tiene cotización cargada para la fecha (F7.4).
     */
    Optional<TipoCambio> findFirstByMonedaIdAndFechaAndActivoTrueOrderByIdAsc(Long monedaId, LocalDate fecha);

    /** Resolución automática de TC respetando el criterio por defecto del sistema (F7.4). */
    Optional<TipoCambio> findFirstByMonedaIdAndFechaAndCriterioAndActivoTrueOrderByIdAsc(Long monedaId, LocalDate fecha, String criterio);
}

package com.montanaritech.contable.maestros.tipocambio;

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
}

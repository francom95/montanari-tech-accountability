package com.montanaritech.contable.inversion;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InversionRepository extends JpaRepository<Inversion, Long> {

    @Query("""
            SELECT i FROM Inversion i
            WHERE (:texto IS NULL OR LOWER(i.instrumento) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:estado IS NULL OR i.estado = :estado)
              AND (:activo IS NULL OR i.activo = :activo)
            ORDER BY i.instrumento ASC
            """)
    Page<Inversion> buscar(@Param("texto") String texto, @Param("estado") EstadoInversion estado,
            @Param("activo") Boolean activo, Pageable pageable);

    /** F8.4: fuente para que F8.3 proyecte el rescate planificado de las inversiones vinculadas. */
    List<Inversion> findByActivoTrueAndEstadoAndVinculoTipoIsNotNull(EstadoInversion estado);

    /** F10.2: resuelve-o-reusa la única Inversion compartida por todos los movimientos de un instrumento (ej. "Fima Premium"). */
    Optional<Inversion> findByInstrumentoIgnoreCase(String instrumento);
}

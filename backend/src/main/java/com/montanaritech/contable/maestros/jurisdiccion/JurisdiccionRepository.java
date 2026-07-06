package com.montanaritech.contable.maestros.jurisdiccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface JurisdiccionRepository extends JpaRepository<Jurisdiccion, Long> {
    @Query("""
            SELECT j FROM Jurisdiccion j
            WHERE (:texto IS NULL OR LOWER(j.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
                                   OR LOWER(j.codigo) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:activo IS NULL OR j.activo = :activo)
            """)
    Page<Jurisdiccion> buscar(@Param("texto") String texto, @Param("activo") Boolean activo, Pageable pageable);
}

package com.montanaritech.contable.maestros.concepto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface ConceptoRepository extends JpaRepository<Concepto, Long> {
    @Query("""
            SELECT c FROM Concepto c
            WHERE (:texto IS NULL OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:activo IS NULL OR c.activo = :activo)
            """)
    Page<Concepto> buscar(@Param("texto") String texto, @Param("activo") Boolean activo, Pageable pageable);

    /** F8.1: todos los activos, sin paginar, para generar sus vencimientos. */
    List<Concepto> findByActivoTrue();
}

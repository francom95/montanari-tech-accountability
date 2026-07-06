package com.montanaritech.contable.maestros.rubro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface RubroRepository extends JpaRepository<Rubro, Long> {
    @Query("""
            SELECT r FROM Rubro r
            WHERE (:texto IS NULL OR LOWER(r.nombre) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:activo IS NULL OR r.activo = :activo)
            """)
    Page<Rubro> buscar(@Param("texto") String texto, @Param("activo") Boolean activo, Pageable pageable);
}

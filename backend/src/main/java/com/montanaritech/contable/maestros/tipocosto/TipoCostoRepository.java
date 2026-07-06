package com.montanaritech.contable.maestros.tipocosto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface TipoCostoRepository extends JpaRepository<TipoCosto, Long> {
    @Query("""
            SELECT tc FROM TipoCosto tc
            WHERE (:texto IS NULL OR LOWER(tc.nombre) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:activo IS NULL OR tc.activo = :activo)
            """)
    Page<TipoCosto> buscar(@Param("texto") String texto, @Param("activo") Boolean activo, Pageable pageable);
}

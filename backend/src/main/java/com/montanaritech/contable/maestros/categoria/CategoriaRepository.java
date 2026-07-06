package com.montanaritech.contable.maestros.categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    @Query("""
            SELECT c FROM Categoria c
            WHERE (:texto IS NULL OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:activo IS NULL OR c.activo = :activo)
            """)
    Page<Categoria> buscar(@Param("texto") String texto, @Param("activo") Boolean activo, Pageable pageable);
}

package com.montanaritech.contable.maestros.proyecto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {
    @Query("""
            SELECT p FROM Proyecto p
            WHERE (:texto IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:activo IS NULL OR p.activo = :activo)
              AND (:clienteId IS NULL OR p.cliente.id = :clienteId)
            """)
    Page<Proyecto> buscar(
            @Param("texto") String texto,
            @Param("activo") Boolean activo,
            @Param("clienteId") Long clienteId,
            Pageable pageable);
}

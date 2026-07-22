package com.montanaritech.contable.maestros.proveedor;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    @Query("""
            SELECT p FROM Proveedor p
            WHERE (:texto IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
                                   OR LOWER(p.cuit) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:activo IS NULL OR p.activo = :activo)
            """)
    Page<Proveedor> buscar(@Param("texto") String texto, @Param("activo") Boolean activo, Pageable pageable);

    Optional<Proveedor> findByNombreIgnoreCase(String nombre);

    /** Resolución por CUIT (F4.6): el importador matchea la contraparte extraída del PDF contra un proveedor existente. */
    Optional<Proveedor> findByCuit(String cuit);
}

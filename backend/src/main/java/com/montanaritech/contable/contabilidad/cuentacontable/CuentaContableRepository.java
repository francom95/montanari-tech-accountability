package com.montanaritech.contable.contabilidad.cuentacontable;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuentaContableRepository extends JpaRepository<CuentaContable, Long> {

    Optional<CuentaContable> findByCodigo(String codigo);

    boolean existsByPadreId(Long padreId);

    boolean existsByPadreIdAndActivoTrue(Long padreId);

    List<CuentaContable> findByPadreId(Long padreId);

    List<CuentaContable> findAllByOrderByCodigoAsc();

    @Query("""
            SELECT c FROM CuentaContable c
            WHERE (:texto IS NULL OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
                                   OR LOWER(c.codigo) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:activo IS NULL OR c.activo = :activo)
            """)
    Page<CuentaContable> buscar(@Param("texto") String texto, @Param("activo") Boolean activo, Pageable pageable);
}

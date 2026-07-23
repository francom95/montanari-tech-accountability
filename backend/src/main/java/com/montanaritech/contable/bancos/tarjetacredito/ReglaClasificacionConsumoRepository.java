package com.montanaritech.contable.bancos.tarjetacredito;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReglaClasificacionConsumoRepository extends JpaRepository<ReglaClasificacionConsumo, Long> {

    @Query("""
            SELECT r FROM ReglaClasificacionConsumo r
            WHERE (:activo IS NULL OR r.activo = :activo)
            ORDER BY r.patron ASC
            """)
    Page<ReglaClasificacionConsumo> buscar(@Param("activo") Boolean activo, Pageable pageable);

    List<ReglaClasificacionConsumo> findByActivoTrue();
}

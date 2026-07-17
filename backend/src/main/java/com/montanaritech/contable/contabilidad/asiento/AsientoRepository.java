package com.montanaritech.contable.contabilidad.asiento;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AsientoRepository extends JpaRepository<Asiento, Long> {

    @Query("""
            SELECT a FROM Asiento a
            WHERE (:texto IS NULL OR LOWER(a.descripcion) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:estado IS NULL OR a.estado = :estado)
            """)
    Page<Asiento> buscar(@Param("texto") String texto, @Param("estado") EstadoDocumento estado, Pageable pageable);
}

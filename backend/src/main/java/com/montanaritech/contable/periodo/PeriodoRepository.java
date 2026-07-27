package com.montanaritech.contable.periodo;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PeriodoRepository extends JpaRepository<Periodo, Long> {

    Optional<Periodo> findByAnioAndMes(Integer anio, Integer mes);

    boolean existsByAnioAndMes(Integer anio, Integer mes);

    @Query("""
            SELECT p FROM Periodo p
            WHERE (:estado IS NULL OR p.estado = :estado)
            ORDER BY p.anio DESC, p.mes DESC
            """)
    Page<Periodo> buscar(@Param("estado") EstadoPeriodo estado, Pageable pageable);
}

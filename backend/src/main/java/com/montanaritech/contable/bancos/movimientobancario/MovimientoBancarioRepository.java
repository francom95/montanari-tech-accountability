package com.montanaritech.contable.bancos.movimientobancario;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoBancarioRepository extends JpaRepository<MovimientoBancario, Long> {

    @Query("""
            SELECT m FROM MovimientoBancario m
            WHERE (:cuentaBancariaId IS NULL OR m.cuentaBancaria.id = :cuentaBancariaId)
              AND (:estado IS NULL OR m.estado = :estado)
              AND (:fechaDesde IS NULL OR m.fecha >= :fechaDesde)
              AND (:fechaHasta IS NULL OR m.fecha <= :fechaHasta)
            """)
    Page<MovimientoBancario> buscar(
            @Param("cuentaBancariaId") Long cuentaBancariaId,
            @Param("estado") EstadoMovimientoBancario estado,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            Pageable pageable);

    long countByEstado(EstadoMovimientoBancario estado);

    long countByEstadoAndCuentaBancaria_Id(EstadoMovimientoBancario estado, Long cuentaBancariaId);

    /** Guarda de "asociar" (F5.1): un asiento ya vinculado a otro movimiento no puede reusarse. */
    boolean existsByAsiento_Id(Long asientoId);
}

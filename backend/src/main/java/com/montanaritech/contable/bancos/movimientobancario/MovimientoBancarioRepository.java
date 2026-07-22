package com.montanaritech.contable.bancos.movimientobancario;

import java.time.LocalDate;
import java.util.List;
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

    /**
     * Conciliación bancaria (F5.3): todos los movimientos con fecha del
     * período, sin paginar (el matching se calcula una vez en memoria,
     * mismo criterio que {@code MayorService}). Los movimientos sin fecha
     * (F5.2, ej. Galicia ARS) quedan afuera a propósito — no tienen
     * período hasta que el usuario los completa en la bandeja de F5.1.
     */
    @Query("""
            SELECT m FROM MovimientoBancario m
            WHERE m.cuentaBancaria.id = :cuentaBancariaId
              AND m.fecha BETWEEN :fechaDesde AND :fechaHasta
            ORDER BY m.fecha ASC
            """)
    List<MovimientoBancario> buscarParaConciliacion(
            @Param("cuentaBancariaId") Long cuentaBancariaId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta);

    long countByEstado(EstadoMovimientoBancario estado);

    long countByEstadoAndCuentaBancaria_Id(EstadoMovimientoBancario estado, Long cuentaBancariaId);

    /** Guarda de "asociar" (F5.1): un asiento ya vinculado a otro movimiento no puede reusarse. */
    boolean existsByAsiento_Id(Long asientoId);

    /** Detección de duplicados al re-importar un resumen (F5.2): mismo hash ya cargado en esta cuenta. */
    boolean existsByCuentaBancaria_IdAndHashImportacion(Long cuentaBancariaId, String hashImportacion);
}

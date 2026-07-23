package com.montanaritech.contable.contabilidad.asiento;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AsientoLineaRepository extends JpaRepository<AsientoLinea, Long> {

    /** Punto de extensión que F3.1 dejó pendiente en {@code CuentaContableService.tieneMovimientos}. */
    boolean existsByCuentaContableId(Long cuentaContableId);

    /**
     * Movimientos para el mayor (F3.1 §5.2/§5.3/§5.4): solo confirmados,
     * orden {@code (fecha, número, orden de línea)} — la fecha manda, el
     * número desempata dentro del día (funcional §4.3). {@code cuentaIds}
     * es un conjunto porque el mayor de una cuenta madre agrega todas sus
     * imputables descendientes (CTE recursivo resuelto en Java, ver
     * {@code MayorService}, dado el tamaño acotado del árbol de cuentas).
     */
    @Query("""
            SELECT l FROM AsientoLinea l
            JOIN FETCH l.asiento a
            JOIN FETCH l.cuentaContable c
            JOIN FETCH l.moneda m
            WHERE a.estado = :estado
              AND l.cuentaContable.id IN :cuentaIds
              AND (:rubroId IS NULL OR c.rubro.id = :rubroId)
              AND (:proyectoId IS NULL OR l.proyecto.id = :proyectoId)
              AND (:clienteId IS NULL OR l.cliente.id = :clienteId)
              AND (:proveedorId IS NULL OR l.proveedor.id = :proveedorId)
              AND (:origen IS NULL OR a.origen = :origen)
              AND (:monedaId IS NULL OR l.moneda.id = :monedaId)
              AND (:fechaDesde IS NULL OR a.fecha >= :fechaDesde)
              AND (:fechaHasta IS NULL OR a.fecha <= :fechaHasta)
            ORDER BY a.fecha ASC, a.numero ASC, l.orden ASC
            """)
    List<AsientoLinea> buscarParaMayor(
            @Param("cuentaIds") Set<Long> cuentaIds,
            @Param("rubroId") Long rubroId,
            @Param("proyectoId") Long proyectoId,
            @Param("clienteId") Long clienteId,
            @Param("proveedorId") Long proveedorId,
            @Param("origen") OrigenAsiento origen,
            @Param("monedaId") Long monedaId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("estado") EstadoDocumento estado);

    /**
     * Saldo anterior (F3.1 §5.2, CP-17): acumulado de todo lo confirmado
     * antes de {@code fechaDesde}, con los mismos filtros analíticos que el
     * resto del mayor. Dos consultas escalares (debe/haber) en vez de una
     * tupla: Spring Data JPA devuelve las proyecciones multi-columna como
     * {@code List<Object[]>}, no como {@code Object[]} directo, así que
     * separarlas es más simple que lidiar con ese envoltorio.
     */
    @Query("""
            SELECT COALESCE(SUM(l.debe), 0) FROM AsientoLinea l
            JOIN l.asiento a
            WHERE a.estado = :estado
              AND l.cuentaContable.id IN :cuentaIds
              AND (:rubroId IS NULL OR l.cuentaContable.rubro.id = :rubroId)
              AND (:proyectoId IS NULL OR l.proyecto.id = :proyectoId)
              AND (:clienteId IS NULL OR l.cliente.id = :clienteId)
              AND (:proveedorId IS NULL OR l.proveedor.id = :proveedorId)
              AND (:origen IS NULL OR a.origen = :origen)
              AND (:monedaId IS NULL OR l.moneda.id = :monedaId)
              AND a.fecha < :fechaDesde
            """)
    BigDecimal sumarDebeAntesDeFecha(
            @Param("cuentaIds") Set<Long> cuentaIds,
            @Param("rubroId") Long rubroId,
            @Param("proyectoId") Long proyectoId,
            @Param("clienteId") Long clienteId,
            @Param("proveedorId") Long proveedorId,
            @Param("origen") OrigenAsiento origen,
            @Param("monedaId") Long monedaId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("estado") EstadoDocumento estado);

    @Query("""
            SELECT COALESCE(SUM(l.haber), 0) FROM AsientoLinea l
            JOIN l.asiento a
            WHERE a.estado = :estado
              AND l.cuentaContable.id IN :cuentaIds
              AND (:rubroId IS NULL OR l.cuentaContable.rubro.id = :rubroId)
              AND (:proyectoId IS NULL OR l.proyecto.id = :proyectoId)
              AND (:clienteId IS NULL OR l.cliente.id = :clienteId)
              AND (:proveedorId IS NULL OR l.proveedor.id = :proveedorId)
              AND (:origen IS NULL OR a.origen = :origen)
              AND (:monedaId IS NULL OR l.moneda.id = :monedaId)
              AND a.fecha < :fechaDesde
            """)
    BigDecimal sumarHaberAntesDeFecha(
            @Param("cuentaIds") Set<Long> cuentaIds,
            @Param("rubroId") Long rubroId,
            @Param("proyectoId") Long proyectoId,
            @Param("clienteId") Long clienteId,
            @Param("proveedorId") Long proveedorId,
            @Param("origen") OrigenAsiento origen,
            @Param("monedaId") Long monedaId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("estado") EstadoDocumento estado);

    /**
     * Candidatos de match para la conciliación bancaria (F5.3): líneas de
     * fondos (cuenta bancaria destino/origen, seteada por Cobro/Pago/
     * MovimientoBancario al confirmar — F4.4/F5.1) de asientos confirmados
     * en el rango de fechas, que todavía NO están asociadas a ningún
     * {@code MovimientoBancario} (esa asociación es 1:1, F5.1). El
     * servicio empareja en memoria contra los movimientos pendientes por
     * fecha±tolerancia e importe exacto — volumen acotado por período,
     * mismo criterio que {@code MayorService}.
     */
    @Query("""
            SELECT l FROM AsientoLinea l
            JOIN FETCH l.asiento a
            WHERE a.estado = :estado
              AND l.cuentaBancaria.id = :cuentaBancariaId
              AND a.fecha BETWEEN :fechaDesde AND :fechaHasta
              AND NOT EXISTS (SELECT 1 FROM MovimientoBancario m WHERE m.asiento = a)
            ORDER BY a.fecha ASC
            """)
    List<AsientoLinea> buscarCandidatosConciliacion(
            @Param("cuentaBancariaId") Long cuentaBancariaId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("estado") EstadoDocumento estado);

    /**
     * Movimientos de una cuenta impositiva en un período, para la liquidación
     * de IVA (F6.1 §1.1). Devuelve las líneas en vez de un escalar porque la
     * pantalla necesita además el detalle trazable (qué documento originó cada
     * importe); el servicio suma en memoria, con el mismo criterio de volumen
     * acotado que {@code MayorService}.
     *
     * <p>{@code origenExcluido} existe para dejar afuera los asientos de
     * {@code LIQUIDACION_IVA}: esos mueven las mismas tres cuentas de IVA para
     * cancelarlas, así que sin la exclusión una liquidación se contaría a sí
     * misma en el período siguiente.
     */
    @Query("""
            SELECT l FROM AsientoLinea l
            JOIN FETCH l.asiento a
            JOIN FETCH l.cuentaContable c
            WHERE a.estado = :estado
              AND l.cuentaContable.id IN :cuentaIds
              AND a.fecha BETWEEN :fechaDesde AND :fechaHasta
              AND a.origen <> :origenExcluido
            ORDER BY a.fecha ASC, a.numero ASC, l.orden ASC
            """)
    List<AsientoLinea> buscarParaLiquidacionImpositiva(
            @Param("cuentaIds") Set<Long> cuentaIds,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("estado") EstadoDocumento estado,
            @Param("origenExcluido") OrigenAsiento origenExcluido);

    /**
     * Debe/haber acumulado por cuenta en el período (F7.2, balance de sumas y
     * saldos; F7.3, estado de resultados con {@code proyectoId} para la vista
     * por proyecto): a diferencia de {@code sumarDebeAntesDeFecha}/{@code
     * sumarHaberAntesDeFecha} (un escalar para un set de cuentas), acá es lo
     * inverso — una fila por cada cuenta imputable con movimientos, para
     * armar el reporte de todas las cuentas de una sola consulta en vez de
     * una por cuenta. Solo cuentas imputables aparecen (F3.4 exige {@code
     * CUENTA_NO_IMPUTABLE} al confirmar), así que el rollup de las cuentas
     * madre (F7.2) o el bucketing por rubro (F7.3) lo hace cada service.
     */
    @Query("""
            SELECT l.cuentaContable.id, COALESCE(SUM(l.debe), 0), COALESCE(SUM(l.haber), 0)
            FROM AsientoLinea l
            JOIN l.asiento a
            WHERE a.estado = :estado
              AND (:fechaDesde IS NULL OR a.fecha >= :fechaDesde)
              AND (:fechaHasta IS NULL OR a.fecha <= :fechaHasta)
              AND (:proyectoId IS NULL OR l.proyecto.id = :proyectoId)
            GROUP BY l.cuentaContable.id
            """)
    List<Object[]> sumarDebeHaberPorCuenta(
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("proyectoId") Long proyectoId,
            @Param("estado") EstadoDocumento estado);

    /**
     * Variante "sin proyecto" de {@link #sumarDebeHaberPorCuenta} (F7.3, vista
     * por proyecto del estado de resultados): {@code proyectoId = null} en el
     * método de arriba significa "sin filtrar" (todas las líneas), no "solo
     * las que no tienen proyecto" — para esa partición exacta hace falta esta
     * consulta aparte con {@code l.proyecto IS NULL} explícito, de forma que
     * Σ(por cada proyecto) + Σ(sin proyecto) = el total sin filtrar.
     */
    @Query("""
            SELECT l.cuentaContable.id, COALESCE(SUM(l.debe), 0), COALESCE(SUM(l.haber), 0)
            FROM AsientoLinea l
            JOIN l.asiento a
            WHERE a.estado = :estado
              AND (:fechaDesde IS NULL OR a.fecha >= :fechaDesde)
              AND (:fechaHasta IS NULL OR a.fecha <= :fechaHasta)
              AND l.proyecto IS NULL
            GROUP BY l.cuentaContable.id
            """)
    List<Object[]> sumarDebeHaberPorCuentaSinProyecto(
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("estado") EstadoDocumento estado);
}

package com.montanaritech.contable.contabilidad.asiento;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AsientoRepository extends JpaRepository<Asiento, Long> {

    /** Búsqueda global (F9.2, término TEXTO): FULLTEXT sobre descripcion, tenant explícito porque es SQL nativo. */
    @Query(value = """
            SELECT * FROM asiento
            WHERE tenant_id = :tenantId AND MATCH(descripcion) AGAINST (:texto IN NATURAL LANGUAGE MODE)
            """,
            countQuery = """
            SELECT COUNT(*) FROM asiento
            WHERE tenant_id = :tenantId AND MATCH(descripcion) AGAINST (:texto IN NATURAL LANGUAGE MODE)
            """,
            nativeQuery = true)
    Page<Asiento> buscarGlobalPorTexto(@Param("tenantId") Long tenantId, @Param("texto") String texto, Pageable pageable);

    /** Búsqueda global (F9.2, término FECHA). */
    Page<Asiento> findByFecha(LocalDate fecha, Pageable pageable);

    /** Búsqueda global (F9.2, término IMPORTE): cualquier línea con debe/haber dentro de la tolerancia. */
    @Query("""
            SELECT DISTINCT a FROM Asiento a JOIN a.lineas l
            WHERE l.debe BETWEEN :desde AND :hasta OR l.haber BETWEEN :desde AND :hasta
            """)
    Page<Asiento> buscarGlobalPorImporte(@Param("desde") BigDecimal desde, @Param("hasta") BigDecimal hasta, Pageable pageable);

    /** Resolución por número visible para el usuario (F5.1, acción "asociar" de un movimiento bancario). */
    Optional<Asiento> findByNumero(Long numero);

    /**
     * F10.2: idempotencia del importador de Libro Diario (descripcion embebe
     * el N° de asiento original del Excel). Filtra por {@code CONFIRMADO}
     * explícitamente: un BORRADOR huérfano (crearBorrador exitoso, confirmar
     * fallido por checklist de negocio en un intento previo) no debe contarse
     * como "ya migrado" — si no, el reintento lo saltea en silencio para
     * siempre en vez de volver a intentarlo o reportarlo como rechazado.
     */
    boolean existsByFechaAndDescripcionAndEstado(LocalDate fecha, String descripcion, EstadoDocumento estado);

    /**
     * Fuente del motor on-demand de {@code Periodo} (F9.3, molde de
     * {@code VencimientoService.generarAutomaticos()} de F8.1): cada (año,
     * mes) con al menos un asiento. JPQL (no nativa), así que el filtro de
     * tenant de Hibernate se aplica solo.
     */
    @Query("SELECT DISTINCT FUNCTION('YEAR', a.fecha), FUNCTION('MONTH', a.fecha) FROM Asiento a")
    List<Object[]> findDistinctAnioMesConAsientos();

    /**
     * Búsqueda avanzada (F3.5): además del texto libre (descripción o leyenda
     * de línea) y el estado, filtra por fecha (rango), número, cuenta,
     * importe (debe o haber de cualquier línea), proyecto, cliente,
     * proveedor y origen. El {@code LEFT JOIN} sobre {@code lineas} es solo
     * para filtrar (no fetch), así que la paginación sigue siendo a nivel
     * SQL; {@code DISTINCT} evita duplicar la cabecera cuando varias líneas
     * matchean el mismo asiento.
     */
    @Query("""
            SELECT DISTINCT a FROM Asiento a
            LEFT JOIN a.lineas l
            WHERE (:texto IS NULL OR LOWER(a.descripcion) LIKE LOWER(CONCAT('%', :texto, '%'))
                                   OR LOWER(l.leyenda) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND (:estado IS NULL OR a.estado = :estado)
              AND (:origen IS NULL OR a.origen = :origen)
              AND (:numero IS NULL OR a.numero = :numero)
              AND (:fechaDesde IS NULL OR a.fecha >= :fechaDesde)
              AND (:fechaHasta IS NULL OR a.fecha <= :fechaHasta)
              AND (:cuentaContableId IS NULL OR l.cuentaContable.id = :cuentaContableId)
              AND (:importe IS NULL OR l.debe = :importe OR l.haber = :importe)
              AND (:proyectoId IS NULL OR l.proyecto.id = :proyectoId)
              AND (:clienteId IS NULL OR l.cliente.id = :clienteId)
              AND (:proveedorId IS NULL OR l.proveedor.id = :proveedorId)
            """)
    Page<Asiento> buscar(
            @Param("texto") String texto,
            @Param("estado") EstadoDocumento estado,
            @Param("origen") OrigenAsiento origen,
            @Param("numero") Long numero,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("cuentaContableId") Long cuentaContableId,
            @Param("importe") BigDecimal importe,
            @Param("proyectoId") Long proyectoId,
            @Param("clienteId") Long clienteId,
            @Param("proveedorId") Long proveedorId,
            Pageable pageable);
}

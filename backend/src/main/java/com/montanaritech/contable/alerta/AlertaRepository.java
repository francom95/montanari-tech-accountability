package com.montanaritech.contable.alerta;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    /** Clave de sincronización: a lo sumo una fila por condición vigente. */
    Optional<Alerta> findByTipoAndEntidadTipoAndEntidadRefId(TipoAlerta tipo, TipoEntidadAlerta entidadTipo, Long entidadRefId);

    /** Universo de alertas ACTIVAS de un tipo, para el diff crear/reactivar/actualizar/auto-resolver del motor. */
    List<Alerta> findByTipoAndEstado(TipoAlerta tipo, EstadoAlerta estado);

    @Query("""
            SELECT a FROM Alerta a
            WHERE (:estado IS NULL OR a.estado = :estado)
              AND (:tipo IS NULL OR a.tipo = :tipo)
            ORDER BY a.severidad DESC, a.fecha ASC
            """)
    Page<Alerta> buscar(@Param("estado") EstadoAlerta estado, @Param("tipo") TipoAlerta tipo, Pageable pageable);

    /** Badge del header (F9.1): activas que el usuario todavía no marcó como leídas. */
    @Query("""
            SELECT COUNT(a) FROM Alerta a
            WHERE a.estado = com.montanaritech.contable.alerta.EstadoAlerta.ACTIVA
              AND NOT EXISTS (
                  SELECT 1 FROM AlertaLectura l WHERE l.alerta = a AND l.usuario.id = :usuarioId
              )
            """)
    long contarActivasNoLeidasPorUsuario(@Param("usuarioId") Long usuarioId);
}

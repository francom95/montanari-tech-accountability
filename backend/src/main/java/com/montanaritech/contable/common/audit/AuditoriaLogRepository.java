package com.montanaritech.contable.common.audit;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditoriaLogRepository extends JpaRepository<AuditoriaLog, Long> {

    @Query("""
            SELECT a FROM AuditoriaLog a
            WHERE (:entidadTipo IS NULL OR a.entidadTipo = :entidadTipo)
              AND (:usuarioId IS NULL OR a.usuarioId = :usuarioId)
              AND (:accion IS NULL OR a.accion = :accion)
              AND (:desde IS NULL OR a.fechaHora >= :desde)
              AND (:hasta IS NULL OR a.fechaHora <= :hasta)
            """)
    Page<AuditoriaLog> buscar(
            @Param("entidadTipo") String entidadTipo,
            @Param("usuarioId") Long usuarioId,
            @Param("accion") AccionAuditoria accion,
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta,
            Pageable pageable);
}

package com.montanaritech.contable.alerta;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertaLecturaRepository extends JpaRepository<AlertaLectura, Long> {

    Optional<AlertaLectura> findByAlerta_IdAndUsuario_Id(Long alertaId, Long usuarioId);

    /** IDs de alerta que el usuario ya leyó, para resolver "leída" en el listado sin N+1. */
    @Query("SELECT l.alerta.id FROM AlertaLectura l WHERE l.usuario.id = :usuarioId AND l.alerta.id IN :alertaIds")
    List<Long> findAlertaIdsLeidasPorUsuario(@Param("alertaIds") Collection<Long> alertaIds, @Param("usuarioId") Long usuarioId);
}

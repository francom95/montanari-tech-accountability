package com.montanaritech.contable.common.adjunto;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdjuntoRepository extends JpaRepository<Adjunto, Long> {

    List<Adjunto> findByEntidadTipoAndEntidadIdOrderByIdAsc(String entidadTipo, Long entidadId);
}

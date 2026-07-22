package com.montanaritech.contable.bancos.conciliacion.dto;

import java.time.LocalDate;

/** Candidato de asiento existente que matchea un movimiento PENDIENTE por importe exacto + fecha±tolerancia (F5.3). */
public record MatchSugeridoResponse(
        Long asientoId,
        Long asientoNumero,
        LocalDate fecha,
        String origenTipo,
        Long origenId,
        String descripcion
) {}

package com.montanaritech.contable.alerta.dto;

import java.time.LocalDate;

public record AlertaResponse(
        Long id,
        String tipo,
        String severidad,
        String mensaje,
        String entidadTipo,
        Long entidadRefId,
        LocalDate fecha,
        String estado,
        boolean leida
) {}

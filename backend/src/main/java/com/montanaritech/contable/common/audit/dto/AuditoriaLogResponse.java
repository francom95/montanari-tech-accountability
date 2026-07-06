package com.montanaritech.contable.common.audit.dto;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import java.time.Instant;

public record AuditoriaLogResponse(
        Long id,
        String entidadTipo,
        Long entidadId,
        AccionAuditoria accion,
        Long usuarioId,
        Instant fechaHora,
        String datosAntes,
        String datosDespues,
        boolean sobrePeriodoCerrado,
        String detalle
) {
}

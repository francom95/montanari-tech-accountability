package com.montanaritech.contable.pendiente.importacion;

import com.montanaritech.contable.pendiente.dto.PendienteAdministrativoResponse;
import java.util.List;

/** {@code yaExistian}: filas cuyo PendienteAdministrativo ya existía por título exacto (idempotencia). */
public record PendienteAdministrativoImportResultado(
        List<PendienteAdministrativoResponse> creadas,
        List<PendienteAdministrativoImportFilaDto> rechazadas,
        int yaExistian
) {}

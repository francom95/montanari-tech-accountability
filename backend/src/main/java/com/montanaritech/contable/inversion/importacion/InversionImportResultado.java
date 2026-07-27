package com.montanaritech.contable.inversion.importacion;

import com.montanaritech.contable.inversion.dto.MovimientoInversionResponse;
import java.util.List;

/** {@code yaExistian}: filas cuyo movimiento ya existía (idempotencia) — ni error, ni duplicado. */
public record InversionImportResultado(
        List<MovimientoInversionResponse> creadas,
        List<InversionImportFilaDto> rechazadas,
        int yaExistian
) {}

package com.montanaritech.contable.compromiso.importacion;

import com.montanaritech.contable.compromiso.dto.CompromisoResponse;
import java.util.List;

/** {@code yaExistian}: filas cuyo Compromiso ya existía por (concepto, fechaPrevista) (idempotencia). */
public record CompromisoImportResultado(
        List<CompromisoResponse> creadas,
        List<CompromisoImportFilaDto> rechazadas,
        int yaExistian
) {}

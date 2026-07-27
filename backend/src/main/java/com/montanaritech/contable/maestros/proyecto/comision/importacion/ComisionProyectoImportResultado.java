package com.montanaritech.contable.maestros.proyecto.comision.importacion;

import com.montanaritech.contable.maestros.proyecto.comision.dto.ComisionProyectoResponse;
import java.util.List;

/** {@code yaExistian}: filas cuyo vínculo proyecto+comisionista ya existía (idempotencia) — ni error, ni duplicado. */
public record ComisionProyectoImportResultado(
        List<ComisionProyectoResponse> creadas,
        List<ComisionProyectoImportFilaDto> rechazadas,
        int yaExistian
) {}

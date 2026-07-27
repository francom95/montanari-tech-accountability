package com.montanaritech.contable.maestros.proyecto.importacion;

import com.montanaritech.contable.maestros.proyecto.dto.ProyectoResponse;
import java.util.List;

/** {@code yaExistian}: filas cuyo Proyecto ya existía por (nombre, cliente) (idempotencia) — ni error, ni duplicado. */
public record ProyectoImportResultado(
        List<ProyectoResponse> creadas,
        List<ProyectoImportFilaDto> rechazadas,
        int yaExistian
) {}

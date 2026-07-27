package com.montanaritech.contable.maestros.cliente.importacion;

import com.montanaritech.contable.maestros.cliente.dto.ClienteResponse;
import java.util.List;

/** {@code yaExistian}: filas cuyo Cliente ya existía por nombre (idempotencia) — ni error, ni duplicado. */
public record ClienteImportResultado(
        List<ClienteResponse> creadas,
        List<ClienteImportFilaDto> rechazadas,
        int yaExistian
) {}

package com.montanaritech.contable.maestros.proveedor.importacion;

import com.montanaritech.contable.maestros.proveedor.dto.ProveedorResponse;
import java.util.List;

/** {@code yaExistian}: filas cuyo Proveedor ya existía por nombre (idempotencia) — ni error, ni duplicado. */
public record ProveedorImportResultado(
        List<ProveedorResponse> creadas,
        List<ProveedorImportFilaDto> rechazadas,
        int yaExistian
) {}

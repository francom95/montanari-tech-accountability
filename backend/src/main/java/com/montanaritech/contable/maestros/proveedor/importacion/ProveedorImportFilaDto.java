package com.montanaritech.contable.maestros.proveedor.importacion;

import java.util.List;

public record ProveedorImportFilaDto(
        int fila,
        String nombre,
        String cuit,
        List<String> errores
) {}

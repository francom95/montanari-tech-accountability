package com.montanaritech.contable.maestros.cliente.importacion;

import java.util.List;

public record ClienteImportFilaDto(
        int fila,
        String nombre,
        String cuit,
        List<String> errores
) {}

package com.montanaritech.contable.pendiente.importacion;

import java.util.List;

public record PendienteAdministrativoImportFilaDto(
        int fila,
        String titulo,
        List<String> errores
) {}

package com.montanaritech.contable.bancos.importacion.dto;

/** Resultado de confirmar una fila de importación (F5.2): IMPORTADO, DUPLICADO o ERROR. */
public record FilaImportacionResultadoResponse(
        String descripcion,
        String resultado,
        String motivoError,
        Long movimientoBancarioId
) {}

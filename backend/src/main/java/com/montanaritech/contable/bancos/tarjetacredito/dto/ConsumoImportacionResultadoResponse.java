package com.montanaritech.contable.bancos.tarjetacredito.dto;

/** Resultado de confirmar una fila de importación de resumen de tarjeta (F5.4): IMPORTADO, DUPLICADO o ERROR. */
public record ConsumoImportacionResultadoResponse(
        String descripcion,
        String resultado,
        String motivoError,
        Long consumoTarjetaId
) {}

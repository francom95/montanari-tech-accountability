package com.montanaritech.contable.bancos.tarjetacredito.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fila del resumen de tarjeta parseada (F5.4, reusa {@code ParserTarjeta} de F5.2), previa a persistirse como {@code ConsumoTarjeta}. */
public record ConsumoImportacionPreviewResponse(
        LocalDate fecha,
        String descripcion,
        BigDecimal importe,
        String monedaCodigo,
        String referencia,
        boolean duplicado,
        String hash
) {}

package com.montanaritech.contable.bancos.importacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fila parseada de un resumen (F5.2), previa a persistirse — el usuario la
 * revisa (y puede completar la fecha faltante, ver {@code fecha} nula) antes
 * de confirmar la importación. {@code duplicado} indica que ya existe un
 * {@code MovimientoBancario} con el mismo hash en la cuenta bancaria elegida.
 */
public record FilaImportacionPreviewResponse(
        LocalDate fecha,
        String descripcion,
        BigDecimal importe,
        String monedaCodigo,
        String referencia,
        boolean duplicado,
        String hash
) {}

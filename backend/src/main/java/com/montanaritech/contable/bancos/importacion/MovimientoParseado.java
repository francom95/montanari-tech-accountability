package com.montanaritech.contable.bancos.importacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fila normalizada por un {@link ResumenParser} (F5.2), previa a convertirse
 * en {@code MovimientoBancario} (F5.1). {@code fecha} puede ser nula cuando
 * el origen no la trae en la fila (ej. Galicia ARS.xlsx) — el usuario la
 * completa a mano en la bandeja antes de confirmar/imputar. {@code importe}
 * lleva signo (positivo = ingreso, negativo = egreso). {@code monedaCodigo}
 * (ISO 4217: "ARS"/"USD") puede ser nulo cuando el archivo no declara
 * moneda por fila (ej. Galicia): el servicio de importación usa entonces
 * la moneda de la cuenta bancaria destino para todas las filas.
 */
public record MovimientoParseado(
        LocalDate fecha,
        String descripcion,
        BigDecimal importe,
        String monedaCodigo,
        String referencia
) {}

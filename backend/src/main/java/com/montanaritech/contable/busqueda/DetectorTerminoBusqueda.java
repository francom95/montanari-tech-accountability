package com.montanaritech.contable.busqueda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Heurísticas fijas de detección de tipo de término (F9.2 §"Términos
 * soportados"): CUIT → importe → fecha → texto libre, en ese orden de
 * prioridad. Sin configuración de usuario — son reglas del motor, no
 * parámetros expuestos.
 */
public final class DetectorTerminoBusqueda {

    private static final DateTimeFormatter FORMATO_DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private DetectorTerminoBusqueda() {
    }

    /** Normaliza a formato {@code XX-XXXXXXXX-X} (el que usa {@code Cliente.cuit}/{@code Proveedor.cuit}) si el término parece un CUIT. */
    public static String esCuit(String termino) {
        String limpio = termino.replace("-", "").replace(" ", "");
        if (!limpio.matches("^\\d{11}$")) {
            return null;
        }
        return limpio.substring(0, 2) + "-" + limpio.substring(2, 10) + "-" + limpio.substring(10);
    }

    /** Parsea el término como importe si es enteramente numérico (con coma o punto decimal). */
    public static BigDecimal comoImporte(String termino) {
        String normalizado = termino.trim().replace(",", ".");
        if (!normalizado.matches("^-?\\d+(\\.\\d+)?$")) {
            return null;
        }
        try {
            return new BigDecimal(normalizado);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Tolerancia de búsqueda por importe: ±1%, con piso de 1.00 para montos chicos. */
    public static BigDecimal toleranciaImporte(BigDecimal importe) {
        BigDecimal porcentaje = importe.abs().multiply(new BigDecimal("0.01"));
        return porcentaje.max(BigDecimal.ONE);
    }

    /** Parsea el término como fecha en formato dd/MM/yyyy o yyyy-MM-dd. */
    public static LocalDate comoFecha(String termino) {
        String t = termino.trim();
        try {
            return LocalDate.parse(t, FORMATO_DD_MM_YYYY);
        } catch (DateTimeParseException ignored) {
            // sigue probando el otro formato
        }
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}

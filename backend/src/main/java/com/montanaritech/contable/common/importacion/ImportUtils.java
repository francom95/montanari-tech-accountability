package com.montanaritech.contable.common.importacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Utilidades de parseo/normalización compartidas por los importadores de F10.2,
 * extraídas del molde de {@code EtapaImportService} (F2.5).
 */
public final class ImportUtils {

    private ImportUtils() {}

    public static String vacioANull(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    /** Acepta tanto "1.234,56" (es-AR) como "1234.56" (en-US) o "1234,56". */
    public static String normalizarImporte(String valor) {
        String limpio = valor.replace(" ", "");
        boolean tieneComa = limpio.contains(",");
        boolean tienePunto = limpio.contains(".");
        if (tieneComa && tienePunto) {
            return limpio.replace(".", "").replace(",", ".");
        }
        if (tieneComa) {
            return limpio.replace(",", ".");
        }
        return limpio;
    }

    public static BigDecimal parsearImporte(String crudo, String etiqueta, List<String> errores) {
        String limpio = vacioANull(crudo);
        if (limpio == null) {
            return null;
        }
        try {
            BigDecimal valor = new BigDecimal(normalizarImporte(limpio));
            if (valor.signum() < 0) {
                errores.add("El " + etiqueta + " no puede ser negativo");
            }
            return valor;
        } catch (NumberFormatException e) {
            errores.add("Formato de " + etiqueta + " inválido: " + crudo);
            return null;
        }
    }

    public static LocalDate parsearFecha(String crudo, DateTimeFormatter formato, String etiqueta, List<String> errores) {
        String limpio = vacioANull(crudo);
        if (limpio == null) {
            return null;
        }
        try {
            return LocalDate.parse(limpio, formato);
        } catch (DateTimeParseException e) {
            errores.add("Formato de " + etiqueta + " inválido: " + crudo);
            return null;
        }
    }
}

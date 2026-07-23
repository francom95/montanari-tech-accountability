package com.montanaritech.contable.common.reporte;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formato AR centralizado para todos los reportes exportables (F7.1, PL-3):
 * moneda {@code $ 1.234.567,89} y fechas {@code dd/mm/yyyy}, sin depender del
 * locale de la máquina donde se abra el archivo.
 */
public final class FormatoReporte {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat MONEDA = new DecimalFormat(
            "#,##0.00", new DecimalFormatSymbols(Locale.of("es", "AR")));

    private FormatoReporte() {
    }

    public static String formatearFecha(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(FECHA);
    }

    public static String formatearFechaHora(LocalDateTime fechaHora) {
        return fechaHora.format(FECHA_HORA);
    }

    public static String formatearMoneda(BigDecimal monto) {
        if (monto == null) {
            return "";
        }
        boolean negativo = monto.signum() < 0;
        String cuerpo = MONEDA.format(monto.abs());
        return (negativo ? "-$ " : "$ ") + cuerpo;
    }
}

package com.montanaritech.contable.bancos.importacion;

import com.montanaritech.contable.bancos.movimientobancario.OrigenImportacionMovimiento;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * Extracción básica de texto (PDFBox, {@code setSortByPosition(false)}, igual
 * que {@code ExtractorFacturaPdf} de F4.6) del resumen de tarjeta Banco
 * Galicia VISA Business. NO es un parser estructural de layout — son
 * patrones regex sobre el orden lineal real que devuelve PDFBox, verificado
 * contra un resumen real (no reconstruido a ojo, misma lección de F4.6).
 *
 * <p>Todas las filas de movimiento (bloque CONSOLIDADO y DETALLE DEL CONSUMO
 * + impuestos/comisiones) empiezan con una fecha "dd-MM-yy" — esto alcanza
 * para filtrar el ruido: encabezados/pies de página repetidos, el subtotal
 * "TARJETA ... Total Consumos de ..." y el "TOTAL A PAGAR" final NUNCA
 * empiezan con fecha, así que quedan afuera sin necesidad de excluirlos a
 * mano. Un importe SIN signo explícito en el texto es un consumo/impuesto
 * (egreso); uno CON signo "-" ya viene expresado como movimiento de cuenta
 * (pagos/devoluciones del bloque CONSOLIDADO) y se respeta tal cual —
 * supuesto a confirmar en el checkpoint humano de F5.2 con la contadora.
 */
@Component
public class ParserTarjeta implements ResumenParser {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd-MM-yy");

    private static final Pattern LINEA_MOVIMIENTO = Pattern.compile("^(\\d{2}-\\d{2}-\\d{2})\\s+(.+)$");
    private static final Pattern MARCADOR_INICIAL = Pattern.compile("^([*A-Z])\\s+(.+)$");
    /** Filas de DETALLE con consumo en dólares: referencia+"USD" pegados, importe duplicado, comprobante, importe. */
    private static final Pattern FILA_DOLARES = Pattern.compile("^(.+?)USD\\s+(-?[\\d.,]+)\\s+(\\d{4,8})\\s+(-?[\\d.,]+)\\s*$");
    private static final Pattern FILA_GENERAL = Pattern.compile("^(.+?)\\s+(-?[\\d.,]+)\\s*$");
    private static final Pattern COMPROBANTE_FINAL = Pattern.compile("^(.*\\D)\\s*(\\d{4,8})$");

    @Override
    public OrigenImportacionMovimiento origen() {
        return OrigenImportacionMovimiento.TARJETA_CREDITO;
    }

    @Override
    public List<MovimientoParseado> parsear(byte[] contenido) {
        try (PDDocument documento = Loader.loadPDF(contenido)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(false);
            return parsearTexto(stripper.getText(documento));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el resumen de tarjeta", e);
        }
    }

    /**
     * Parsing propiamente dicho, separado de la extracción PDFBox para poder
     * testear contra texto real ya capturado (mismo patrón que
     * {@code ExtractorFacturaPdf#extraer} de F4.6).
     */
    List<MovimientoParseado> parsearTexto(String textoOriginal) {
        String texto = textoOriginal.replace(' ', ' ');
        List<MovimientoParseado> movimientos = new ArrayList<>();
        for (String linea : texto.split("\\r?\\n")) {
            MovimientoParseado movimiento = parsearLinea(linea);
            if (movimiento != null) {
                movimientos.add(movimiento);
            }
        }
        return movimientos;
    }

    private MovimientoParseado parsearLinea(String linea) {
        Matcher matchFecha = LINEA_MOVIMIENTO.matcher(linea.trim());
        if (!matchFecha.matches()) {
            return null;
        }
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(matchFecha.group(1), FORMATO_FECHA);
        } catch (java.time.format.DateTimeParseException e) {
            return null; // "28-May-26 ..." (rango de vencimientos): no es fecha numérica real, no es un movimiento
        }

        String resto = matchFecha.group(2).trim();
        Matcher matchMarcador = MARCADOR_INICIAL.matcher(resto);
        if (matchMarcador.matches()) {
            resto = matchMarcador.group(2).trim();
        }

        Matcher matchDolares = FILA_DOLARES.matcher(resto);
        if (matchDolares.matches()) {
            String descripcion = normalizarEspacios(matchDolares.group(1));
            String comprobante = matchDolares.group(3);
            BigDecimal monto = parsearImporte(matchDolares.group(4)).abs().negate();
            return new MovimientoParseado(fecha, descripcion, monto, "USD", comprobante);
        }

        Matcher matchGeneral = FILA_GENERAL.matcher(resto);
        if (!matchGeneral.matches()) {
            return null;
        }
        String descripcionCruda = normalizarEspacios(matchGeneral.group(1));
        String montoTexto = matchGeneral.group(2);
        BigDecimal monto = parsearImporte(montoTexto);
        if (!montoTexto.trim().startsWith("-")) {
            monto = monto.negate(); // consumo/impuesto sin signo explícito = egreso
        }

        String descripcion = descripcionCruda;
        String referencia = null;
        Matcher matchComprobante = COMPROBANTE_FINAL.matcher(descripcionCruda);
        if (matchComprobante.matches()) {
            descripcion = normalizarEspacios(matchComprobante.group(1));
            referencia = matchComprobante.group(2);
        }

        String monedaCodigo = descripcion.toUpperCase(Locale.ROOT).contains("USD")
                || descripcion.toUpperCase(Locale.ROOT).contains("DÓLAR") ? "USD" : "ARS";

        return new MovimientoParseado(fecha, descripcion, monto, monedaCodigo, referencia);
    }

    private String normalizarEspacios(String texto) {
        return texto.trim().replaceAll("\\s+", " ");
    }

    /** "125.969,64" (miles con punto, decimales con coma) → 125969.64; conserva el signo "-" si está presente. */
    private BigDecimal parsearImporte(String texto) {
        String normalizado = texto.trim();
        boolean negativo = normalizado.startsWith("-");
        normalizado = normalizado.replace("-", "").replace(".", "").replace(",", ".");
        BigDecimal valor = new BigDecimal(normalizado);
        return negativo ? valor.negate() : valor;
    }
}

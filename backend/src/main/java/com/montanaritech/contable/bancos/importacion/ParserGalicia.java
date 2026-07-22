package com.montanaritech.contable.bancos.importacion;

import com.montanaritech.contable.bancos.movimientobancario.OrigenImportacionMovimiento;
import com.montanaritech.contable.common.error.NegocioException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

/**
 * Parser del export de Banco Galicia home banking (hoja "Movimientos",
 * Excel) o del resumen "Movimientos de CC" (PDF, "Office Banking") — se
 * detecta el formato por los bytes iniciales del archivo. La moneda del
 * archivo entero (ARS o USD) no está declarada adentro — por eso
 * {@code monedaCodigo} sale siempre {@code null}: la resuelve el servicio de
 * importación usando la moneda de la cuenta bancaria destino elegida por el
 * usuario, igual para todas las filas del archivo.
 *
 * <p>El Excel real puede no traer fecha en NINGUNA fila (caso confirmado:
 * Galicia ARS.xlsx) — se deja nula, el usuario la completa en la bandeja de
 * F5.1 antes de confirmar/imputar. El PDF sí trae fecha siempre, pero a
 * cambio no separa Débitos/Créditos en columnas (todo es un único
 * "$ importe" por fila) — la dirección se infiere por palabras clave en la
 * descripción, ver {@link #PALABRAS_CREDITO}.
 */
@Component
public class ParserGalicia implements ResumenParser {

    private static final String COL_FECHA = "fecha";
    private static final String COL_DESCRIPCION = "descripci"; // matchea "Descripción" con o sin mojibake de encoding
    private static final String COL_DEBITOS = "débitos";
    private static final String COL_DEBITOS_ALT = "debitos";
    private static final String COL_CREDITOS = "créditos";
    private static final String COL_CREDITOS_ALT = "creditos";
    private static final String COL_COMPROBANTE = "comprobante";

    @Override
    public OrigenImportacionMovimiento origen() {
        return OrigenImportacionMovimiento.GALICIA;
    }

    @Override
    public List<MovimientoParseado> parsear(byte[] contenido) {
        if (esPdf(contenido)) {
            return parsearPdf(contenido);
        }
        return parsearExcel(contenido);
    }

    private boolean esPdf(byte[] contenido) {
        return contenido.length >= 4 && contenido[0] == '%' && contenido[1] == 'P' && contenido[2] == 'D' && contenido[3] == 'F';
    }

    private List<MovimientoParseado> parsearExcel(byte[] contenido) {
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            Sheet hoja = wb.getSheetAt(0);
            Row encabezado = hoja.getRow(hoja.getFirstRowNum());
            if (encabezado == null) {
                throw new NegocioException("RESUMEN_GALICIA_INVALIDO", "El archivo no tiene encabezado reconocible");
            }

            int colFecha = buscarColumna(encabezado, COL_FECHA);
            int colDescripcion = buscarColumna(encabezado, COL_DESCRIPCION);
            int colDebitos = buscarColumnaAlt(encabezado, COL_DEBITOS, COL_DEBITOS_ALT);
            int colCreditos = buscarColumnaAlt(encabezado, COL_CREDITOS, COL_CREDITOS_ALT);
            int colComprobante = buscarColumna(encabezado, COL_COMPROBANTE);
            if (colFecha < 0 || colDescripcion < 0 || colDebitos < 0 || colCreditos < 0) {
                throw new NegocioException("RESUMEN_GALICIA_INVALIDO",
                        "No se reconocen las columnas esperadas (Fecha/Descripción/Débitos/Créditos)");
            }

            List<MovimientoParseado> movimientos = new ArrayList<>();
            for (int i = hoja.getFirstRowNum() + 1; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null) {
                    continue;
                }
                String descripcion = textoDeCelda(fila.getCell(colDescripcion));
                BigDecimal debitos = numeroDeCelda(fila.getCell(colDebitos));
                BigDecimal creditos = numeroDeCelda(fila.getCell(colCreditos));
                if ((descripcion == null || descripcion.isBlank())
                        && debitos.compareTo(BigDecimal.ZERO) == 0
                        && creditos.compareTo(BigDecimal.ZERO) == 0) {
                    continue; // fila basura / de cierre
                }

                BigDecimal importe = creditos.compareTo(BigDecimal.ZERO) > 0 ? creditos : debitos.negate();
                if (importe.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                LocalDate fecha = fechaDeCelda(fila.getCell(colFecha));
                String referencia = colComprobante >= 0 ? textoDeCelda(fila.getCell(colComprobante)) : null;

                movimientos.add(new MovimientoParseado(fecha, descripcion, importe, null, referencia));
            }
            return movimientos;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el resumen de Galicia", e);
        }
    }

    private int buscarColumna(Row encabezado, String texto) {
        for (Cell celda : encabezado) {
            String valor = textoDeCelda(celda);
            if (valor != null && valor.toLowerCase(Locale.ROOT).contains(texto)) {
                return celda.getColumnIndex();
            }
        }
        return -1;
    }

    private int buscarColumnaAlt(Row encabezado, String texto, String alternativo) {
        int col = buscarColumna(encabezado, texto);
        return col >= 0 ? col : buscarColumna(encabezado, alternativo);
    }

    private String textoDeCelda(Cell celda) {
        if (celda == null) {
            return null;
        }
        return switch (celda.getCellType()) {
            case STRING -> celda.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(celda.getNumericCellValue());
            default -> null;
        };
    }

    /**
     * {@code BigDecimal.valueOf(double)} pasa por {@code Double.toString}, que
     * para valores ≥ 10.000.000 devuelve notación científica (ej.
     * "1.462E7") — el {@code BigDecimal} resultante queda con escala
     * negativa y su serialización JSON hereda esa notación. Se normaliza
     * acá a 2 decimales para que el importe siempre salga como un número
     * de cuenta normal.
     */
    private BigDecimal numeroDeCelda(Cell celda) {
        if (celda == null || celda.getCellType() != CellType.NUMERIC) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(celda.getNumericCellValue()).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Galicia USD.xlsx real trae la columna Fecha con celdas {@code t="d"}
     * (fecha ISO 8601, ej. {@code <v>2026-06-02T00:00:00.000Z</v>}) en vez
     * del serial numérico tradicional de Excel. Ese tipo de celda no está
     * en el enum {@code ST_CellType} que trae POI/xmlbeans — cualquier
     * llamada a {@code getCellType()} sobre esa celda específica revienta
     * con {@code XmlValueOutOfRangeException} ("string value 'd' is not a
     * valid enumeration value"), sea cual sea el método de POI que la
     * invoque por detrás. {@code getRawValue()} sí funciona: devuelve el
     * texto crudo de {@code <v>} sin pasar por esa validación de tipo.
     */
    private LocalDate fechaDeCelda(Cell celda) {
        if (celda == null) {
            return null;
        }
        CellType tipo;
        try {
            tipo = celda.getCellType();
        } catch (RuntimeException e) {
            String valorCrudo = celda instanceof org.apache.poi.xssf.usermodel.XSSFCell xssfCelda ? xssfCelda.getRawValue() : null;
            return fechaIso8601DeValorCrudo(valorCrudo);
        }
        if (tipo != CellType.NUMERIC || !DateUtil.isCellDateFormatted(celda)) {
            return null;
        }
        Date fecha = celda.getDateCellValue();
        return fecha == null ? null : fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalDate fechaIso8601DeValorCrudo(String valorCrudo) {
        if (valorCrudo == null || valorCrudo.isBlank()) {
            return null;
        }
        return java.time.Instant.parse(valorCrudo).atZone(ZoneId.of("UTC")).toLocalDate();
    }

    // ---- Formato PDF ("Movimientos de CC") ----

    private static final DateTimeFormatter FORMATO_FECHA_PDF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Cada fila real es "FECHA DESCRIPCION $ SALDO(+/-) $ IMPORTE" en una
     * sola línea (el PDF no separa Débitos/Créditos como el Excel — son la
     * misma columna "$ IMPORTE" para ambos casos). Ej. real:
     * {@code 01/06/2026 COMISION SERVICIO DE CUENTA $ 6.727.063,22- $ 60.000,00}.
     */
    private static final Pattern LINEA_MOVIMIENTO_PDF = Pattern.compile(
            "^(\\d{2}/\\d{2}/\\d{4})\\s+(.+?)\\s+\\$\\s*([\\d.,]+)([+-])\\s+\\$\\s*([\\d.,]+)\\s*$");

    /**
     * Palabras que indican un ingreso. El PDF no separa Débitos/Créditos en
     * columnas como el Excel — cada fila trae un único "$ importe" sin
     * signo propio, así que la dirección se infiere por descripción. Todo
     * lo que no matchea acá cae en egreso (la gran mayoría real:
     * comisiones, impuestos, transferencias salientes, pagos).
     *
     * <p><b>El saldo acumulado con signo ("+"/"-") que trae cada fila NO
     * sirve para esto</b> — se probó comparar el saldo de una fila contra
     * el de la anterior (si "sube", ingreso; si "baja", egreso) y el
     * resultado contradice lo que ya sabíamos por Galicia ARS.xlsx real:
     * "Iva" es un débito confirmado (columna Débitos del Excel) pero el
     * saldo del PDF "sube" en esa fila — no hay una relación aritmética
     * simple entre saldo y dirección en este formato, al menos no una que
     * se pueda derivar de dos filas consecutivas.
     *
     * <p>Heurística, no una regla cerrada — verificado que clasifica
     * correctamente las 32 filas reales de "Galicia ARS (1).pdf"
     * comparando contra "Galicia ARS.xlsx" (que sí separa Débitos/
     * Créditos). Ojo con agregar palabras ambiguas: "ACREDITAMIENTO" se
     * probó y quedó afuera a propósito porque "Servicio Acreditamiento De
     * Haberes" (el banco pagando sueldos por este medio) es un DÉBITO real
     * para la cuenta de la empresa, no un ingreso.
     */
    private static final Set<String> PALABRAS_CREDITO = Set.of(
            "DEPOSITO", "RESCATE", "TRANSFERENCIA RECIBIDA", "CREDITO", "DEVOLUCION", "INTERES GANADO");

    private List<MovimientoParseado> parsearPdf(byte[] contenido) {
        try (PDDocument documento = Loader.loadPDF(contenido)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(false);
            return parsearTextoPdf(stripper.getText(documento));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el resumen de Galicia (PDF)", e);
        }
    }

    /**
     * Parsing propiamente dicho, separado de la extracción PDFBox para poder
     * testear contra texto real ya capturado (mismo patrón que
     * {@code ParserTarjeta#parsearTexto}).
     */
    List<MovimientoParseado> parsearTextoPdf(String textoOriginal) {
        String texto = textoOriginal.replace(' ', ' ');
        String[] lineas = texto.split("\\r?\\n");
        List<MovimientoParseado> movimientos = new ArrayList<>();
        for (int i = 0; i < lineas.length; i++) {
            Matcher m = LINEA_MOVIMIENTO_PDF.matcher(lineas[i].trim());
            if (!m.matches()) {
                continue;
            }

            LocalDate fecha = LocalDate.parse(m.group(1), FORMATO_FECHA_PDF);
            String descripcion = normalizarEspaciosPdf(m.group(2));
            BigDecimal importeMagnitud = parsearNumeroAr(m.group(5));

            boolean credito = esDescripcionDeCredito(descripcion);
            BigDecimal importe = credito ? importeMagnitud : importeMagnitud.negate();

            String referencia = primeraLineaAuxiliar(lineas, i + 1);

            movimientos.add(new MovimientoParseado(fecha, descripcion, importe, null, referencia));
        }
        return movimientos;
    }

    /** La línea siguiente a la del movimiento (referencia, período, nombre de contraparte, etc.) si existe y no es otro movimiento. */
    private String primeraLineaAuxiliar(String[] lineas, int desde) {
        if (desde >= lineas.length) {
            return null;
        }
        String siguiente = lineas[desde].trim();
        if (siguiente.isBlank() || LINEA_MOVIMIENTO_PDF.matcher(siguiente).matches()) {
            return null;
        }
        return siguiente;
    }

    private boolean esDescripcionDeCredito(String descripcion) {
        String normalizada = descripcion.toUpperCase(Locale.ROOT);
        return PALABRAS_CREDITO.stream().anyMatch(normalizada::contains);
    }

    private String normalizarEspaciosPdf(String texto) {
        return texto.trim().replaceAll("\\s+", " ");
    }

    /** "6.727.063,22" (miles con punto, decimales con coma) → 6727063.22. */
    private BigDecimal parsearNumeroAr(String texto) {
        return new BigDecimal(texto.replace(".", "").replace(",", "."));
    }
}

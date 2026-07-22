package com.montanaritech.contable.bancos.importacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Los valores de las filas replican EXACTAMENTE los capturados con openpyxl
 * contra los archivos reales "Galicia ARS.xlsx" y "Galicia USD.xlsx" (Banco
 * Galicia home banking, hoja "Movimientos", 16 columnas) — no se
 * reconstruyen a ojo, siguiendo la lección de F4.6 sobre no confiar en
 * texto idealizado. Los binarios no se versionan (son resúmenes reales del
 * cliente); ver outputs/F5_2_parsers_de_resumenes.md.
 */
class ParserGaliciaTest {

    private static final String[] ENCABEZADO = {
            "Fecha", "Descripción", "Origen", "Débitos", "Créditos", "Grupo de Conceptos", "Concepto",
            "Número de Terminal", "Observaciones Cliente", "Número de Comprobante", "Leyendas Adicionales 1",
            "Leyendas Adicionales 2", "Leyendas Adicionales 3", "Leyendas Adicionales 4", "Tipo de Movimiento", "Saldo"
    };

    private final ParserGalicia parser = new ParserGalicia();

    @Test
    void galiciaArsSinFechaEnNingunaFilaDejaFechaNula() {
        byte[] archivo = construirArchivo(false);

        List<MovimientoParseado> movimientos = parser.parsear(archivo);

        assertThat(movimientos).hasSize(3);
        assertThat(movimientos).allMatch(m -> m.fecha() == null);
        assertThat(movimientos).allMatch(m -> m.monedaCodigo() == null);

        MovimientoParseado comisionServicio = movimientos.get(0);
        assertThat(comisionServicio.descripcion()).isEqualTo("Comision Servicio De Cuenta");
        assertThat(comisionServicio.importe()).isEqualByComparingTo("-60000.0");
        assertThat(comisionServicio.referencia()).isNull();

        MovimientoParseado transferenciaAfip = movimientos.get(1);
        assertThat(transferenciaAfip.descripcion()).isEqualTo("Transf. Afip");
        assertThat(transferenciaAfip.importe()).isEqualByComparingTo("-13262.18");
        assertThat(transferenciaAfip.referencia()).isEqualTo("346143332");

        MovimientoParseado pagoTarjeta = movimientos.get(2);
        assertThat(pagoTarjeta.descripcion()).isEqualTo("Pago Tarjeta Visa");
        assertThat(pagoTarjeta.importe()).isEqualByComparingTo("-125969.64");
        assertThat(pagoTarjeta.referencia()).isEqualTo("163937936");
    }

    @Test
    void galiciaUsdConFechaPoblada() {
        byte[] archivo = construirArchivo(true);

        List<MovimientoParseado> movimientos = parser.parsear(archivo);

        assertThat(movimientos).hasSize(3);
        assertThat(movimientos).allMatch(m -> m.monedaCodigo() == null);

        MovimientoParseado comision = movimientos.get(0);
        assertThat(comision.fecha()).isEqualTo(LocalDate.of(2026, 6, 2));
        assertThat(comision.descripcion()).isEqualTo("Comision Mantenimiento Cta. Cte/cce");
        assertThat(comision.importe()).isEqualByComparingTo("-10.0");

        MovimientoParseado pagoVisa = movimientos.get(2);
        assertThat(pagoVisa.fecha()).isEqualTo(LocalDate.of(2026, 6, 5));
        assertThat(pagoVisa.descripcion()).isEqualTo("Pago Tarjeta Visa");
        assertThat(pagoVisa.importe()).isEqualByComparingTo("-102.22");
        assertThat(pagoVisa.referencia()).isEqualTo("163839379");
    }

    /**
     * Galicia USD.xlsx real usa celdas {@code t="d"} (fecha ISO 8601) en la
     * columna Fecha en vez del serial numérico tradicional — un tipo de
     * celda que el enum {@code ST_CellType} de POI/xmlbeans no reconoce:
     * cualquier llamada a {@code getCellType()} sobre esa celda específica
     * revienta con {@code XmlValueOutOfRangeException}, confirmado
     * únicamente al probar contra el archivo real (no lo reproduce un
     * workbook armado con el writer de POI, que nunca emite {@code t="d"}).
     * Este test arma a mano el XML mínimo de un .xlsx con esa celda exacta
     * para blindar el fallback de {@code fechaDeCelda} contra una regresión.
     */
    @Test
    void fechaEnCeldaTipoDIso8601NoReconocidaPorPoiSeParseaIgual() throws IOException {
        byte[] archivo = construirArchivoConCeldaTipoD();

        List<MovimientoParseado> movimientos = parser.parsear(archivo);

        assertThat(movimientos).hasSize(1);
        MovimientoParseado m = movimientos.get(0);
        assertThat(m.fecha()).isEqualTo(LocalDate.of(2026, 6, 2));
        assertThat(m.descripcion()).isEqualTo("Comision Mantenimiento");
        assertThat(m.importe()).isEqualByComparingTo("-10.00");
        assertThat(m.referencia()).isEqualTo("163839379");
    }

    @Test
    void unCreditoDaImporteEnPositivo() {
        byte[] archivo = construirArchivoConCredito();

        List<MovimientoParseado> movimientos = parser.parsear(archivo);

        assertThat(movimientos).hasSize(1);
        assertThat(movimientos.get(0).importe()).isEqualByComparingTo("50000.00");
    }

    /** Filas basura (todo en blanco) no generan movimientos. */
    private byte[] construirArchivo(boolean conFecha) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet hoja = wb.createSheet("Movimientos");
            escribirEncabezado(hoja);

            if (conFecha) {
                CreationHelper helper = wb.getCreationHelper();
                CellStyle estiloFecha = wb.createCellStyle();
                estiloFecha.setDataFormat(helper.createDataFormat().getFormat("dd/mm/yyyy"));

                escribirFilaConFecha(hoja, 1, 2026, Calendar.JUNE, 2, "Comision Mantenimiento Cta. Cte/cce", 10, 0, null, estiloFecha);
                escribirFilaConFecha(hoja, 2, 2026, Calendar.JUNE, 2, "Iva", 2.1, 0, null, estiloFecha);
                escribirFilaConFecha(hoja, 3, 2026, Calendar.JUNE, 5, "Pago Tarjeta Visa", 102.22, 0, "163839379", estiloFecha);
            } else {
                escribirFilaSinFecha(hoja, 1, "Comision Servicio De Cuenta", 60000.0, 0, null);
                escribirFilaSinFecha(hoja, 2, "Transf. Afip", 13262.18, 0, "346143332");
                escribirFilaSinFecha(hoja, 3, "Pago Tarjeta Visa", 125969.64, 0, "163937936");
                hoja.createRow(4); // fila basura: todas las celdas vacías
            }

            return aBytes(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private byte[] construirArchivoConCredito() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet hoja = wb.createSheet("Movimientos");
            escribirEncabezado(hoja);
            escribirFilaSinFecha(hoja, 1, "Transferencia recibida", 0, 50000.00, "999111");
            return aBytes(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void escribirEncabezado(Sheet hoja) {
        Row fila = hoja.createRow(0);
        for (int i = 0; i < ENCABEZADO.length; i++) {
            fila.createCell(i).setCellValue(ENCABEZADO[i]);
        }
    }

    private void escribirFilaSinFecha(Sheet hoja, int indice, String descripcion, double debitos, double creditos, String comprobante) {
        Row fila = hoja.createRow(indice);
        // celda 0 (Fecha) deliberadamente sin escribir: replica Galicia ARS.xlsx real, donde la columna está vacía en todas las filas
        fila.createCell(1).setCellValue(descripcion);
        fila.createCell(3).setCellValue(debitos);
        fila.createCell(4).setCellValue(creditos);
        if (comprobante != null) {
            fila.createCell(9).setCellValue(comprobante);
        }
    }

    private void escribirFilaConFecha(Sheet hoja, int indice, int anio, int mes, int dia, String descripcion,
            double debitos, double creditos, String comprobante, CellStyle estiloFecha) {
        Row fila = hoja.createRow(indice);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(anio, mes, dia);
        Cell celdaFecha = fila.createCell(0);
        celdaFecha.setCellValue(cal);
        celdaFecha.setCellStyle(estiloFecha);
        fila.createCell(1).setCellValue(descripcion);
        fila.createCell(3).setCellValue(debitos);
        fila.createCell(4).setCellValue(creditos);
        if (comprobante != null) {
            fila.createCell(9).setCellValue(comprobante);
        }
    }

    private byte[] aBytes(XSSFWorkbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    /** Arma a mano el .xlsx mínimo (POI no puede generar celdas {@code t="d"}) con el XML real observado. */
    private byte[] construirArchivoConCeldaTipoD() throws IOException {
        String contentTypes = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                </Types>""";
        String rootRels = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>""";
        String workbookXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                <sheets><sheet name="Movimientos" sheetId="1" r:id="rId1"/></sheets>
                </workbook>""";
        String workbookRels = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                </Relationships>""";
        // Fila 1: encabezado (inlineStr). Fila 2: la celda A2 real observada en Galicia USD.xlsx (t="d", fecha ISO 8601).
        String sheetXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <sheetData>
                <row r="1">
                <c r="A1" t="inlineStr"><is><t>Fecha</t></is></c>
                <c r="B1" t="inlineStr"><is><t>Descripcion</t></is></c>
                <c r="D1" t="inlineStr"><is><t>Debitos</t></is></c>
                <c r="E1" t="inlineStr"><is><t>Creditos</t></is></c>
                <c r="J1" t="inlineStr"><is><t>Numero de Comprobante</t></is></c>
                </row>
                <row r="2">
                <c r="A2" t="d"><v>2026-06-02T00:00:00.000Z</v></c>
                <c r="B2" t="inlineStr"><is><t>Comision Mantenimiento</t></is></c>
                <c r="D2"><v>10</v></c>
                <c r="E2"><v>0</v></c>
                <c r="J2" t="inlineStr"><is><t>163839379</t></is></c>
                </row>
                </sheetData>
                </worksheet>""";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            escribirEntrada(zip, "[Content_Types].xml", contentTypes);
            escribirEntrada(zip, "_rels/.rels", rootRels);
            escribirEntrada(zip, "xl/workbook.xml", workbookXml);
            escribirEntrada(zip, "xl/_rels/workbook.xml.rels", workbookRels);
            escribirEntrada(zip, "xl/worksheets/sheet1.xml", sheetXml);
        }
        return out.toByteArray();
    }

    private void escribirEntrada(ZipOutputStream zip, String nombre, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}

package com.montanaritech.contable.common.reporte;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * Molde de referencia de PL-3 (F1.8). Punto único de exportación: cualquier
 * reporte nuevo arma su título + columnas + filas y le pasa el trabajo de
 * escribir el archivo a este service, sin reimplementar POI/OpenPDF cada
 * vez. Usa {@code SXSSFWorkbook} (streaming) para no cargar todas las filas
 * en memoria en reportes grandes (F1.1: exportaciones deben soportar
 * volumen).
 *
 * <p>Placeholder de logo/estilos corporativos: pendiente hasta que el
 * equipo lo entregue (ver F1.1 §"Pendiente del equipo"); por ahora los
 * reportes solo llevan título y tabla.
 */
@Service
public class ReportExportService {

    public void exportarExcel(String titulo, List<String> columnas, List<List<Object>> filas, OutputStream out)
            throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            SXSSFSheet hoja = workbook.createSheet(titulo);

            CellStyle estiloEncabezado = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font fuenteEncabezado = workbook.createFont();
            fuenteEncabezado.setBold(true);
            fuenteEncabezado.setFontHeightInPoints((short) 11);
            estiloEncabezado.setFont(fuenteEncabezado);

            Row filaEncabezado = hoja.createRow(0);
            for (int col = 0; col < columnas.size(); col++) {
                Cell celda = filaEncabezado.createCell(col);
                celda.setCellValue(columnas.get(col));
                celda.setCellStyle(estiloEncabezado);
            }

            for (int fila = 0; fila < filas.size(); fila++) {
                Row filaExcel = hoja.createRow(fila + 1);
                List<Object> valores = filas.get(fila);
                for (int col = 0; col < valores.size(); col++) {
                    escribirCelda(filaExcel.createCell(col), valores.get(col));
                }
            }

            workbook.write(out);
            workbook.dispose();
        }
    }

    public void exportarPdf(String titulo, List<String> columnas, List<List<Object>> filas, OutputStream out)
            throws DocumentException {
        Document documento = new Document(PageSize.A4.rotate(), 24, 24, 36, 36);
        PdfWriter.getInstance(documento, out);
        documento.open();

        Font fuenteTitulo = new Font(Font.HELVETICA, 14, Font.BOLD);
        Paragraph tituloParrafo = new Paragraph(titulo, fuenteTitulo);
        tituloParrafo.setAlignment(Element.ALIGN_LEFT);
        tituloParrafo.setSpacingAfter(12);
        documento.add(tituloParrafo);

        PdfPTable tabla = new PdfPTable(columnas.size());
        tabla.setWidthPercentage(100);

        Font fuenteEncabezado = new Font(Font.HELVETICA, 10, Font.BOLD);
        for (String columna : columnas) {
            PdfPCell celda = new PdfPCell(new Paragraph(columna, fuenteEncabezado));
            tabla.addCell(celda);
        }

        Font fuenteCelda = new Font(Font.HELVETICA, 9);
        for (List<Object> fila : filas) {
            for (Object valor : fila) {
                tabla.addCell(new Paragraph(valor == null ? "" : valor.toString(), fuenteCelda));
            }
        }

        documento.add(tabla);
        documento.close();
    }

    private void escribirCelda(Cell celda, Object valor) {
        switch (valor) {
            case null -> celda.setBlank();
            case Number numero -> celda.setCellValue(numero.doubleValue());
            case Boolean booleano -> celda.setCellValue(booleano);
            default -> celda.setCellValue(valor.toString());
        }
    }
}

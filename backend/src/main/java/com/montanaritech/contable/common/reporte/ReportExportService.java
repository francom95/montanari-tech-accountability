package com.montanaritech.contable.common.reporte;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.montanaritech.contable.common.tenant.Tenant;
import com.montanaritech.contable.common.tenant.TenantContext;
import com.montanaritech.contable.common.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportExportService {

    private static final int FILAS_POR_LOTE_PDF = 500;

    private final TenantRepository tenantRepository;

    public void exportarExcel(ContextoReporte contexto, List<String> columnas, List<List<Object>> filas, OutputStream out)
            throws IOException {
        Tenant tenant = tenantActual();
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            SXSSFSheet hoja = workbook.createSheet(nombreHoja(contexto.titulo()));

            CellStyle estiloEmpresa = estiloTexto(workbook, 14, true);
            CellStyle estiloTitulo = estiloTexto(workbook, 12, true);
            CellStyle estiloMeta = estiloTexto(workbook, 9, false);
            CellStyle estiloEncabezadoColumnas = estiloTexto(workbook, 11, true);
            CellStyle estiloFecha = workbook.createCellStyle();
            estiloFecha.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));

            int fila = 0;
            fila = escribirTexto(hoja, fila, tenant.getNombre(), estiloEmpresa);
            fila = escribirTexto(hoja, fila, contexto.titulo(), estiloTitulo);
            fila = escribirTexto(hoja, fila, String.join("  |  ", contexto.filtrosAplicados()), estiloMeta);
            fila = escribirTexto(hoja, fila, "Emitido: " + FormatoReporte.formatearFechaHora(LocalDateTime.now()), estiloMeta);
            fila++;

            Row filaEncabezado = hoja.createRow(fila++);
            for (int col = 0; col < columnas.size(); col++) {
                Cell celda = filaEncabezado.createCell(col);
                celda.setCellValue(columnas.get(col));
                celda.setCellStyle(estiloEncabezadoColumnas);
            }

            for (List<Object> valores : filas) {
                Row filaExcel = hoja.createRow(fila++);
                for (int col = 0; col < valores.size(); col++) {
                    escribirCelda(filaExcel.createCell(col), valores.get(col), estiloFecha);
                }
            }

            insertarLogoSiCorresponde(workbook, hoja, tenant, columnas.size());

            workbook.write(out);
            workbook.dispose();
        }
    }

    public void exportarPdf(ContextoReporte contexto, List<String> columnas, List<List<Object>> filas, OutputStream out)
            throws DocumentException, IOException {
        Tenant tenant = tenantActual();
        Document documento = new Document(PageSize.A4.rotate(), 24, 24, 40, 40);
        PdfWriter writer = PdfWriter.getInstance(documento, out);
        writer.setPageEvent(new NumeracionPaginasPdf());
        documento.open();

        escribirCabeceraPdf(documento, tenant, contexto);

        PdfPTable tabla = new PdfPTable(columnas.size());
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);

        Font fuenteEncabezado = new Font(Font.HELVETICA, 9, Font.BOLD);
        for (String columna : columnas) {
            tabla.addCell(new PdfPCell(new Paragraph(columna, fuenteEncabezado)));
        }

        Font fuenteCelda = new Font(Font.HELVETICA, 8);
        int contador = 0;
        for (List<Object> fila : filas) {
            for (Object valor : fila) {
                tabla.addCell(new PdfPCell(new Paragraph(formatearValorTexto(valor), fuenteCelda)));
            }
            contador++;
            if (contador % FILAS_POR_LOTE_PDF == 0) {
                documento.add(tabla);
                tabla.deleteBodyRows();
            }
        }
        documento.add(tabla);

        documento.close();
    }

    private Tenant tenantActual() {
        return tenantRepository.findById(TenantContext.getTenantId())
                .orElseThrow(() -> new IllegalStateException("Tenant no encontrado: " + TenantContext.getTenantId()));
    }

    private void escribirCabeceraPdf(Document documento, Tenant tenant, ContextoReporte contexto) throws DocumentException, IOException {
        Font fuenteEmpresa = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font fuenteTitulo = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font fuenteMeta = new Font(Font.HELVETICA, 9, Font.NORMAL, java.awt.Color.DARK_GRAY);

        Paragraph empresa = new Paragraph(tenant.getNombre(), fuenteEmpresa);
        Paragraph titulo = new Paragraph(contexto.titulo(), fuenteTitulo);
        titulo.setSpacingBefore(2);
        Paragraph filtros = new Paragraph(
                contexto.filtrosAplicados().isEmpty() ? "" : String.join("  |  ", contexto.filtrosAplicados()), fuenteMeta);
        filtros.setSpacingBefore(4);
        Paragraph emitido = new Paragraph("Emitido: " + FormatoReporte.formatearFechaHora(LocalDateTime.now()), fuenteMeta);
        emitido.setSpacingAfter(10);

        Image logo = cargarLogo(tenant);
        if (logo != null) {
            logo.scaleToFit(90, 40);
            PdfPTable encabezado = new PdfPTable(2);
            encabezado.setWidthPercentage(100);
            encabezado.setWidths(new float[] {4, 1});

            PdfPCell celdaTexto = new PdfPCell();
            celdaTexto.setBorder(0);
            celdaTexto.addElement(empresa);
            celdaTexto.addElement(titulo);
            celdaTexto.addElement(filtros);
            celdaTexto.addElement(emitido);

            PdfPCell celdaLogo = new PdfPCell(logo, false);
            celdaLogo.setBorder(0);
            celdaLogo.setHorizontalAlignment(Element.ALIGN_RIGHT);
            celdaLogo.setVerticalAlignment(Element.ALIGN_TOP);

            encabezado.addCell(celdaTexto);
            encabezado.addCell(celdaLogo);
            documento.add(encabezado);
        } else {
            documento.add(empresa);
            documento.add(titulo);
            documento.add(filtros);
            documento.add(emitido);
        }
    }

    private Image cargarLogo(Tenant tenant) throws DocumentException, IOException {
        if (tenant.getLogoClasspath() == null) {
            return null;
        }
        ClassPathResource recurso = new ClassPathResource(tenant.getLogoClasspath());
        if (!recurso.exists()) {
            return null;
        }
        return Image.getInstance(recurso.getInputStream().readAllBytes());
    }

    private void insertarLogoSiCorresponde(Workbook workbook, SXSSFSheet hoja, Tenant tenant, int columnasDatos) throws IOException {
        if (tenant.getLogoClasspath() == null) {
            return;
        }
        ClassPathResource recurso = new ClassPathResource(tenant.getLogoClasspath());
        if (!recurso.exists()) {
            return;
        }
        byte[] bytes = recurso.getInputStream().readAllBytes();
        int indice = workbook.addPicture(bytes, tipoImagen(tenant.getLogoClasspath()));
        CreationHelper helper = workbook.getCreationHelper();
        Drawing<?> dibujo = hoja.createDrawingPatriarch();
        ClientAnchor ancla = helper.createClientAnchor();
        ancla.setCol1(columnasDatos + 1);
        ancla.setRow1(0);
        ancla.setCol2(columnasDatos + 3);
        ancla.setRow2(4);
        Picture imagen = dibujo.createPicture(ancla, indice);
        imagen.resize();
    }

    private int tipoImagen(String logoClasspath) {
        return logoClasspath.toLowerCase().endsWith(".jpg") || logoClasspath.toLowerCase().endsWith(".jpeg")
                ? Workbook.PICTURE_TYPE_JPEG
                : Workbook.PICTURE_TYPE_PNG;
    }

    private CellStyle estiloTexto(Workbook workbook, int puntos, boolean negrita) {
        CellStyle estilo = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font fuente = workbook.createFont();
        fuente.setBold(negrita);
        fuente.setFontHeightInPoints((short) puntos);
        estilo.setFont(fuente);
        return estilo;
    }

    private int escribirTexto(SXSSFSheet hoja, int fila, String texto, CellStyle estilo) {
        Row row = hoja.createRow(fila);
        Cell celda = row.createCell(0);
        celda.setCellValue(texto == null ? "" : texto);
        celda.setCellStyle(estilo);
        return fila + 1;
    }

    private String nombreHoja(String titulo) {
        String limpio = titulo.replaceAll("[\\\\/*?\\[\\]:]", " ");
        return limpio.length() > 31 ? limpio.substring(0, 31) : limpio;
    }

    private void escribirCelda(Cell celda, Object valor, CellStyle estiloFecha) {
        switch (valor) {
            case null -> celda.setBlank();
            case BigDecimal monto -> celda.setCellValue(FormatoReporte.formatearMoneda(monto));
            case LocalDate fecha -> {
                celda.setCellValue(fecha);
                celda.setCellStyle(estiloFecha);
            }
            case Number numero -> celda.setCellValue(numero.doubleValue());
            case Boolean booleano -> celda.setCellValue(booleano);
            default -> celda.setCellValue(valor.toString());
        }
    }

    private String formatearValorTexto(Object valor) {
        return switch (valor) {
            case null -> "";
            case BigDecimal monto -> FormatoReporte.formatearMoneda(monto);
            case LocalDate fecha -> FormatoReporte.formatearFecha(fecha);
            case Boolean booleano -> booleano ? "Sí" : "No";
            default -> valor.toString();
        };
    }
}

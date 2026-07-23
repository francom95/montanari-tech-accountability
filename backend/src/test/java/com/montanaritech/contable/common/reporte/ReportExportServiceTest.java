package com.montanaritech.contable.common.reporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.montanaritech.contable.common.tenant.Tenant;
import com.montanaritech.contable.common.tenant.TenantRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Servicio único de exportación (F7.1): estilos corporativos, formato AR y
 * streaming real para volúmenes grandes sin OOM.
 */
@ExtendWith(MockitoExtension.class)
class ReportExportServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    private ReportExportService service;

    @BeforeEach
    void setUp() {
        service = new ReportExportService(tenantRepository);
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setNombre("Montanari Tech");
        when(tenantRepository.findById(anyLong())).thenReturn(Optional.of(tenant));
    }

    @Test
    void exportarExcel_incluyeEncabezadoCorporativoYFormatoAr() throws Exception {
        List<String> columnas = List.of("Fecha", "Descripción", "Monto");
        List<List<Object>> filas = List.of(List.of(LocalDate.of(2026, 3, 5), "Factura A", new BigDecimal("123456.78")));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.exportarExcel(ContextoReporte.de("Reporte de prueba", "Desde: 01/03/2026"), columnas, filas, out);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet hoja = workbook.getSheetAt(0);
            assertThat(hoja.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Montanari Tech");
            assertThat(hoja.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Reporte de prueba");
            assertThat(hoja.getRow(2).getCell(0).getStringCellValue()).contains("Desde: 01/03/2026");
            assertThat(hoja.getRow(3).getCell(0).getStringCellValue()).startsWith("Emitido: ");

            Row encabezadoColumnas = hoja.getRow(5);
            assertThat(encabezadoColumnas.getCell(0).getStringCellValue()).isEqualTo("Fecha");
            assertThat(encabezadoColumnas.getCell(2).getStringCellValue()).isEqualTo("Monto");

            Row dato = hoja.getRow(6);
            assertThat(dato.getCell(0).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 5));
            assertThat(dato.getCell(1).getStringCellValue()).isEqualTo("Factura A");
            assertThat(dato.getCell(2).getStringCellValue()).isEqualTo("$ 123.456,78");
        }
    }

    @Test
    void exportarPdf_generaArchivoValidoConNumeracionDePaginas() throws Exception {
        List<String> columnas = List.of("Columna");
        List<List<Object>> filas = List.of(List.of("valor"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.exportarPdf(ContextoReporte.de("Reporte PDF"), columnas, filas, out);

        byte[] bytes = out.toByteArray();
        assertThat(new String(bytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");

        try (PdfReader reader = new PdfReader(bytes)) {
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
            // El total de páginas se dibuja en un PdfTemplate aparte (recién se conoce al
            // cerrar el documento), así que el extractor de texto separa "de " y "1" con un
            // salto de línea aunque en el PDF renderizado queden en el mismo renglón; se
            // normalizan espacios/saltos para comparar el contenido lógico.
            String texto = new PdfTextExtractor(reader).getTextFromPage(1).replaceAll("\\s+", " ");
            assertThat(texto).contains("Página 1 de 1");
        }
    }

    @Test
    void exportarExcel_50milFilasSinOom() throws Exception {
        List<String> columnas = List.of("Fecha", "Descripción", "Monto");
        List<List<Object>> filas = generarFilas(50_000);
        Path archivo = Files.createTempFile("f7-1-excel", ".xlsx");
        try {
            try (OutputStream out = Files.newOutputStream(archivo)) {
                service.exportarExcel(ContextoReporte.de("Volumen"), columnas, filas, out);
            }
            assertThat(Files.size(archivo)).isGreaterThan(0);
        } finally {
            Files.deleteIfExists(archivo);
        }
    }

    @Test
    void exportarPdf_50milFilasSinOom() throws Exception {
        List<String> columnas = List.of("Fecha", "Descripción", "Monto");
        List<List<Object>> filas = generarFilas(50_000);
        Path archivo = Files.createTempFile("f7-1-pdf", ".pdf");
        try {
            try (OutputStream out = Files.newOutputStream(archivo)) {
                service.exportarPdf(ContextoReporte.de("Volumen"), columnas, filas, out);
            }
            assertThat(Files.size(archivo)).isGreaterThan(0);
            try (PdfReader reader = new PdfReader(archivo.toString())) {
                assertThat(reader.getNumberOfPages()).isGreaterThan(100);
            }
        } finally {
            Files.deleteIfExists(archivo);
        }
    }

    private List<List<Object>> generarFilas(int cantidad) {
        List<List<Object>> filas = new ArrayList<>(cantidad);
        LocalDate base = LocalDate.of(2026, 1, 1);
        for (int i = 0; i < cantidad; i++) {
            filas.add(List.of(base.plusDays(i % 365), "Fila " + i, BigDecimal.valueOf(i * 1.11d)));
        }
        return filas;
    }
}

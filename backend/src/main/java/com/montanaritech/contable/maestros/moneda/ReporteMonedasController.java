package com.montanaritech.contable.maestros.moneda;

import com.montanaritech.contable.common.reporte.ContextoReporte;
import com.montanaritech.contable.common.reporte.ReportExportService;
import com.montanaritech.contable.maestros.moneda.dto.MonedaResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Molde de referencia de PL-3 (F1.8): reporte dummy sobre Moneda (el
 * dominio real no importa acá, lo que importa es la forma). Un reporte
 * real reemplaza {@code datos()} por su propia consulta/agregación y
 * ajusta filtros (período, proyecto, cuenta, etc. según F1.1 §"filtros
 * estándar") y columnas; el resto —endpoint JSON + 2 endpoints de
 * exportación reusando {@link ReportExportService}— se copia igual.
 */
@RestController
@RequestMapping("/api/v1/reportes/monedas")
@RequiredArgsConstructor
public class ReporteMonedasController {

    private static final List<String> COLUMNAS = List.of("Código", "Nombre", "Símbolo", "Activo");

    private final MonedaRepository monedaRepository;
    private final MonedaMapper monedaMapper;
    private final ReportExportService reportExportService;

    @GetMapping
    public List<MonedaResponse> datos(@RequestParam(required = false) Boolean activo) {
        return monedaRepository.buscar(null, activo, PageRequest.of(0, 1000))
                .map(monedaMapper::aResponse)
                .getContent();
    }

    @GetMapping("/exportar/excel")
    public ResponseEntity<StreamingResponseBody> exportarExcel(@RequestParam(required = false) Boolean activo) {
        List<List<Object>> filas = aFilas(todasLasFilas(activo));
        ContextoReporte contexto = contexto(activo);
        StreamingResponseBody cuerpo = out -> reportExportService.exportarExcel(contexto, COLUMNAS, filas, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-monedas.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(cuerpo);
    }

    @GetMapping("/exportar/pdf")
    public ResponseEntity<StreamingResponseBody> exportarPdf(@RequestParam(required = false) Boolean activo) {
        List<List<Object>> filas = aFilas(todasLasFilas(activo));
        ContextoReporte contexto = contexto(activo);
        StreamingResponseBody cuerpo = out -> {
            try {
                reportExportService.exportarPdf(contexto, COLUMNAS, filas, out);
            } catch (Exception e) {
                throw new IOException("No se pudo generar el PDF", e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-monedas.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(cuerpo);
    }

    /**
     * A diferencia de {@link #datos(Boolean)} (pantalla, acotada a 1000 filas),
     * el export no debe truncar el volumen (F7.1): trae todo con {@code Pageable.unpaged()}.
     */
    private List<MonedaResponse> todasLasFilas(Boolean activo) {
        return monedaRepository.buscar(null, activo, Pageable.unpaged())
                .map(monedaMapper::aResponse)
                .getContent();
    }

    private List<List<Object>> aFilas(List<MonedaResponse> monedas) {
        return monedas.stream()
                .<List<Object>>map(m -> List.of(m.codigo(), m.nombre(), m.simbolo(), m.activo()))
                .toList();
    }

    private ContextoReporte contexto(Boolean activo) {
        return ContextoReporte.de("Reporte de Monedas", activo == null ? null : "Activo: " + (activo ? "Sí" : "No"));
    }
}

package com.montanaritech.contable.contabilidad.mayor;

import com.montanaritech.contable.common.reporte.ReportExportService;
import com.montanaritech.contable.contabilidad.asiento.OrigenAsiento;
import com.montanaritech.contable.contabilidad.mayor.dto.MayorFilaResponse;
import com.montanaritech.contable.contabilidad.mayor.dto.MayorResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Mayor de una cuenta (F3.4 §5, F3.6, molde PL-3 de F1.8). Igual criterio
 * que {@code ReporteMonedasController}: un endpoint JSON paginado para
 * pantalla + dos de exportación (Excel/PDF) que reusan
 * {@link ReportExportService} sobre el cálculo completo sin paginar.
 */
@RestController
@RequestMapping("/api/v1/cuentas-contables/{cuentaId}/mayor")
@RequiredArgsConstructor
@Tag(name = "Mayor")
public class MayorController {

    private static final List<String> COLUMNAS = List.of(
            "Fecha", "N° Asiento", "Descripción", "Debe", "Haber", "Saldo acumulado",
            "Moneda", "Importe original", "TC", "Origen");

    private final MayorService service;
    private final ReportExportService reportExportService;

    @GetMapping
    public MayorResponse consultar(
            @PathVariable Long cuentaId,
            @RequestParam(required = false) Long rubroId,
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) OrigenAsiento origen,
            @RequestParam(required = false) Long monedaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var completo = service.calcular(cuentaId, rubroId, proyectoId, clienteId, proveedorId, origen, monedaId,
                fechaDesde, fechaHasta);
        return service.paginar(completo, page, size);
    }

    @GetMapping("/exportar/excel")
    public ResponseEntity<StreamingResponseBody> exportarExcel(
            @PathVariable Long cuentaId,
            @RequestParam(required = false) Long rubroId,
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) OrigenAsiento origen,
            @RequestParam(required = false) Long monedaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        var completo = service.calcular(cuentaId, rubroId, proyectoId, clienteId, proveedorId, origen, monedaId,
                fechaDesde, fechaHasta);
        List<List<Object>> filas = aFilas(completo.filas());
        StreamingResponseBody cuerpo = out -> reportExportService.exportarExcel(
                "Mayor - " + completo.cuenta().getCodigo() + " " + completo.cuenta().getNombre(), COLUMNAS, filas, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mayor-" + completo.cuenta().getCodigo() + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(cuerpo);
    }

    @GetMapping("/exportar/pdf")
    public ResponseEntity<StreamingResponseBody> exportarPdf(
            @PathVariable Long cuentaId,
            @RequestParam(required = false) Long rubroId,
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) OrigenAsiento origen,
            @RequestParam(required = false) Long monedaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        var completo = service.calcular(cuentaId, rubroId, proyectoId, clienteId, proveedorId, origen, monedaId,
                fechaDesde, fechaHasta);
        List<List<Object>> filas = aFilas(completo.filas());
        String titulo = "Mayor - " + completo.cuenta().getCodigo() + " " + completo.cuenta().getNombre();
        StreamingResponseBody cuerpo = out -> {
            try {
                reportExportService.exportarPdf(titulo, COLUMNAS, filas, out);
            } catch (Exception e) {
                throw new IOException("No se pudo generar el PDF", e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mayor-" + completo.cuenta().getCodigo() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(cuerpo);
    }

    private List<List<Object>> aFilas(List<MayorFilaResponse> filas) {
        return filas.stream()
                .<List<Object>>map(f -> List.of(
                        f.esSaldoAnterior() ? "" : f.fecha().toString(),
                        f.esSaldoAnterior() || f.numeroAsiento() == null ? "" : f.numeroAsiento(),
                        f.descripcion(),
                        f.debe() == null ? "" : f.debe(),
                        f.haber() == null ? "" : f.haber(),
                        f.saldoAcumulado(),
                        f.monedaCodigo() == null ? "" : f.monedaCodigo(),
                        f.importeOriginal() == null ? "" : f.importeOriginal(),
                        f.tipoCambio() == null ? "" : f.tipoCambio(),
                        f.origen() == null ? "" : f.origen()))
                .toList();
    }
}

package com.montanaritech.contable.facturacion.cuentasporcobrar;

import com.montanaritech.contable.common.reporte.EstadoVencimiento;
import com.montanaritech.contable.common.reporte.ReportExportService;
import com.montanaritech.contable.facturacion.cuentasporcobrar.dto.CuentaPorCobrarFilaResponse;
import com.montanaritech.contable.facturacion.cuentasporcobrar.dto.CuentaPorCobrarResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Cuentas por cobrar (F4.5, molde PL-3 — mismo criterio que {@code
 * MayorController} de F3.6): un endpoint JSON para pantalla + dos de
 * exportación (Excel/PDF) que reusan {@link ReportExportService}.
 */
@RestController
@RequestMapping("/api/v1/reportes/cuentas-por-cobrar")
@RequiredArgsConstructor
@Tag(name = "CuentasPorCobrar")
public class CuentaPorCobrarController {

    private static final List<String> COLUMNAS = List.of(
            "Cliente", "Proyecto", "Factura", "Fecha", "Vencimiento", "Moneda", "Total", "Total ARS", "Saldo", "Saldo ARS", "Estado");

    private final CuentaPorCobrarService service;
    private final ReportExportService reportExportService;

    @GetMapping
    public CuentaPorCobrarResponse consultar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) Long monedaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoVencimiento estadoVencimiento) {
        return service.calcular(clienteId, proyectoId, monedaId, fechaDesde, fechaHasta, estadoVencimiento);
    }

    @GetMapping("/exportar/excel")
    public ResponseEntity<StreamingResponseBody> exportarExcel(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) Long monedaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoVencimiento estadoVencimiento) {
        var resultado = service.calcular(clienteId, proyectoId, monedaId, fechaDesde, fechaHasta, estadoVencimiento);
        List<List<Object>> filas = aFilas(resultado.filas());
        StreamingResponseBody cuerpo = out -> reportExportService.exportarExcel("Cuentas por cobrar", COLUMNAS, filas, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cuentas-por-cobrar.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(cuerpo);
    }

    @GetMapping("/exportar/pdf")
    public ResponseEntity<StreamingResponseBody> exportarPdf(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) Long monedaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoVencimiento estadoVencimiento) {
        var resultado = service.calcular(clienteId, proyectoId, monedaId, fechaDesde, fechaHasta, estadoVencimiento);
        List<List<Object>> filas = aFilas(resultado.filas());
        StreamingResponseBody cuerpo = out -> {
            try {
                reportExportService.exportarPdf("Cuentas por cobrar", COLUMNAS, filas, out);
            } catch (Exception e) {
                throw new IOException("No se pudo generar el PDF", e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cuentas-por-cobrar.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(cuerpo);
    }

    private List<List<Object>> aFilas(List<CuentaPorCobrarFilaResponse> filas) {
        return filas.stream()
                .<List<Object>>map(f -> List.of(
                        f.clienteNombre(),
                        f.proyectoNombre() == null ? "" : f.proyectoNombre(),
                        f.numero(),
                        f.fecha().toString(),
                        f.fechaVencimiento() == null ? "" : f.fechaVencimiento().toString(),
                        f.monedaCodigo(),
                        f.total(),
                        f.totalArs(),
                        f.saldo(),
                        f.saldoArs(),
                        f.estadoVencimiento()))
                .toList();
    }
}

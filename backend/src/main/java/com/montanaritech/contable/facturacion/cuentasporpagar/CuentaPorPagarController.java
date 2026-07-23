package com.montanaritech.contable.facturacion.cuentasporpagar;

import com.montanaritech.contable.common.reporte.ContextoReporte;
import com.montanaritech.contable.common.reporte.EstadoVencimiento;
import com.montanaritech.contable.common.reporte.FormatoReporte;
import com.montanaritech.contable.common.reporte.ReportExportService;
import com.montanaritech.contable.facturacion.cuentasporpagar.dto.CuentaPorPagarFilaResponse;
import com.montanaritech.contable.facturacion.cuentasporpagar.dto.CuentaPorPagarResponse;
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

/** Cuentas por pagar (F4.5), simétrico a {@code CuentaPorCobrarController}. */
@RestController
@RequestMapping("/api/v1/reportes/cuentas-por-pagar")
@RequiredArgsConstructor
@Tag(name = "CuentasPorPagar")
public class CuentaPorPagarController {

    private static final List<String> COLUMNAS = List.of(
            "Proveedor", "Proyecto", "Factura", "Fecha", "Vencimiento", "Moneda", "Total", "Total ARS", "Saldo", "Saldo ARS", "Estado");

    private final CuentaPorPagarService service;
    private final ReportExportService reportExportService;

    @GetMapping
    public CuentaPorPagarResponse consultar(
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) Long monedaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoVencimiento estadoVencimiento) {
        return service.calcular(proveedorId, proyectoId, monedaId, fechaDesde, fechaHasta, estadoVencimiento);
    }

    @GetMapping("/exportar/excel")
    public ResponseEntity<StreamingResponseBody> exportarExcel(
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) Long monedaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoVencimiento estadoVencimiento) {
        var resultado = service.calcular(proveedorId, proyectoId, monedaId, fechaDesde, fechaHasta, estadoVencimiento);
        List<List<Object>> filas = aFilas(resultado.filas());
        ContextoReporte contexto = contexto(proveedorId, proyectoId, monedaId, fechaDesde, fechaHasta, estadoVencimiento);
        StreamingResponseBody cuerpo = out -> reportExportService.exportarExcel(contexto, COLUMNAS, filas, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cuentas-por-pagar.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(cuerpo);
    }

    @GetMapping("/exportar/pdf")
    public ResponseEntity<StreamingResponseBody> exportarPdf(
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) Long monedaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) EstadoVencimiento estadoVencimiento) {
        var resultado = service.calcular(proveedorId, proyectoId, monedaId, fechaDesde, fechaHasta, estadoVencimiento);
        List<List<Object>> filas = aFilas(resultado.filas());
        ContextoReporte contexto = contexto(proveedorId, proyectoId, monedaId, fechaDesde, fechaHasta, estadoVencimiento);
        StreamingResponseBody cuerpo = out -> {
            try {
                reportExportService.exportarPdf(contexto, COLUMNAS, filas, out);
            } catch (Exception e) {
                throw new IOException("No se pudo generar el PDF", e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cuentas-por-pagar.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(cuerpo);
    }

    private List<List<Object>> aFilas(List<CuentaPorPagarFilaResponse> filas) {
        return filas.stream()
                .<List<Object>>map(f -> List.of(
                        f.proveedorNombre(),
                        f.proyectoNombre() == null ? "" : f.proyectoNombre(),
                        f.numero(),
                        f.fecha(),
                        f.fechaVencimiento() == null ? "" : f.fechaVencimiento(),
                        f.monedaCodigo(),
                        f.total(),
                        f.totalArs(),
                        f.saldo(),
                        f.saldoArs(),
                        f.estadoVencimiento()))
                .toList();
    }

    private ContextoReporte contexto(Long proveedorId, Long proyectoId, Long monedaId, LocalDate fechaDesde,
            LocalDate fechaHasta, EstadoVencimiento estadoVencimiento) {
        return ContextoReporte.de("Cuentas por pagar",
                proveedorId == null ? null : "Proveedor ID: " + proveedorId,
                proyectoId == null ? null : "Proyecto ID: " + proyectoId,
                monedaId == null ? null : "Moneda ID: " + monedaId,
                fechaDesde == null ? null : "Desde: " + FormatoReporte.formatearFecha(fechaDesde),
                fechaHasta == null ? null : "Hasta: " + FormatoReporte.formatearFecha(fechaHasta),
                estadoVencimiento == null ? null : "Estado: " + estadoVencimiento);
    }
}

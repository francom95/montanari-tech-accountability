package com.montanaritech.contable.contabilidad.balance;

import com.montanaritech.contable.common.reporte.ContextoReporte;
import com.montanaritech.contable.common.reporte.FormatoReporte;
import com.montanaritech.contable.common.reporte.ReportExportService;
import com.montanaritech.contable.contabilidad.balance.dto.BalanceSumasYSaldosNodo;
import com.montanaritech.contable.contabilidad.balance.dto.BalanceSumasYSaldosResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
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

/** Balance de sumas y saldos (F7.2, molde PL-3 — mismo criterio que {@code MayorController} de F3.6). */
@RestController
@RequestMapping("/api/v1/reportes/balance-sumas-y-saldos")
@RequiredArgsConstructor
@Tag(name = "BalanceSumasYSaldos")
public class BalanceSumasYSaldosController {

    private static final List<String> COLUMNAS = List.of("Código", "Cuenta", "Debe", "Haber", "Saldo", "Naturaleza del saldo");

    private final BalanceSumasYSaldosService service;
    private final ReportExportService reportExportService;

    @GetMapping
    public BalanceSumasYSaldosResponse consultar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "false") boolean incluirSinMovimiento,
            @RequestParam(required = false) Integer nivelMaximo) {
        return service.calcular(fechaDesde, fechaHasta, incluirSinMovimiento, nivelMaximo);
    }

    @GetMapping("/exportar/excel")
    public ResponseEntity<StreamingResponseBody> exportarExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "false") boolean incluirSinMovimiento,
            @RequestParam(required = false) Integer nivelMaximo) {
        var completo = service.calcular(fechaDesde, fechaHasta, incluirSinMovimiento, nivelMaximo);
        List<List<Object>> filas = aFilas(completo);
        ContextoReporte contexto = contexto(fechaDesde, fechaHasta, incluirSinMovimiento, nivelMaximo);
        StreamingResponseBody cuerpo = out -> reportExportService.exportarExcel(contexto, COLUMNAS, filas, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"balance-sumas-y-saldos.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(cuerpo);
    }

    @GetMapping("/exportar/pdf")
    public ResponseEntity<StreamingResponseBody> exportarPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "false") boolean incluirSinMovimiento,
            @RequestParam(required = false) Integer nivelMaximo) {
        var completo = service.calcular(fechaDesde, fechaHasta, incluirSinMovimiento, nivelMaximo);
        List<List<Object>> filas = aFilas(completo);
        ContextoReporte contexto = contexto(fechaDesde, fechaHasta, incluirSinMovimiento, nivelMaximo);
        StreamingResponseBody cuerpo = out -> {
            try {
                reportExportService.exportarPdf(contexto, COLUMNAS, filas, out);
            } catch (Exception e) {
                throw new IOException("No se pudo generar el PDF", e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"balance-sumas-y-saldos.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(cuerpo);
    }

    private List<List<Object>> aFilas(BalanceSumasYSaldosResponse completo) {
        List<List<Object>> filas = new ArrayList<>();
        for (BalanceSumasYSaldosNodo raiz : completo.raices()) {
            agregarFilas(raiz, 0, filas);
        }
        filas.add(List.of("", "TOTALES", completo.totalDebe(), completo.totalHaber(), completo.diferencia(),
                completo.balancea() ? "BALANCEA" : "NO BALANCEA - revisar, es señal de bug"));
        return filas;
    }

    private void agregarFilas(BalanceSumasYSaldosNodo nodo, int nivel, List<List<Object>> filas) {
        String indentacion = "    ".repeat(nivel);
        filas.add(List.of(nodo.codigo(), indentacion + nodo.nombre(), nodo.debe(), nodo.haber(), nodo.saldo(), nodo.saldoEtiqueta()));
        for (BalanceSumasYSaldosNodo hijo : nodo.hijos()) {
            agregarFilas(hijo, nivel + 1, filas);
        }
    }

    private ContextoReporte contexto(LocalDate fechaDesde, LocalDate fechaHasta, boolean incluirSinMovimiento, Integer nivelMaximo) {
        return ContextoReporte.de("Balance de sumas y saldos",
                fechaDesde == null ? null : "Desde: " + FormatoReporte.formatearFecha(fechaDesde),
                fechaHasta == null ? null : "Hasta: " + FormatoReporte.formatearFecha(fechaHasta),
                incluirSinMovimiento ? "Incluye cuentas sin movimiento" : null,
                nivelMaximo == null ? null : "Nivel máximo: " + nivelMaximo);
    }
}

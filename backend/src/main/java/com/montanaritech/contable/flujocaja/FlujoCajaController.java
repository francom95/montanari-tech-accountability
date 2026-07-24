package com.montanaritech.contable.flujocaja;

import com.montanaritech.contable.common.reporte.ContextoReporte;
import com.montanaritech.contable.common.reporte.ReportExportService;
import com.montanaritech.contable.flujocaja.dto.FlujoCajaResponse;
import com.montanaritech.contable.flujocaja.dto.PuntoFlujoCaja;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Flujo de caja real y proyectado (F8.3). */
@RestController
@RequestMapping("/api/v1/flujo-caja")
@RequiredArgsConstructor
@Tag(name = "FlujoCaja")
public class FlujoCajaController {

    private static final List<String> COLUMNAS = List.of(
            "Fecha", "Tipo", "Saldo inicial", "Ingresos", "Egresos", "Saldo final", "Alerta");

    private final FlujoCajaService service;
    private final ReportExportService reportExportService;

    @GetMapping("/real")
    public FlujoCajaResponse real(@RequestParam LocalDate desde, @RequestParam LocalDate hasta,
            @RequestParam(defaultValue = "DIARIO") Granularidad granularidad) {
        return service.flujoReal(desde, hasta, granularidad);
    }

    @GetMapping("/proyectado")
    public FlujoCajaResponse proyectado(@RequestParam(defaultValue = "30") int dias) {
        return service.flujoProyectado(dias);
    }

    @GetMapping("/combinado")
    public FlujoCajaResponse combinado(
            @RequestParam(defaultValue = "30") int diasAtras, @RequestParam(defaultValue = "30") int diasAdelante) {
        return service.combinado(diasAtras, diasAdelante);
    }

    @GetMapping("/exportar/excel")
    public ResponseEntity<StreamingResponseBody> exportarExcel(
            @RequestParam(defaultValue = "30") int diasAtras, @RequestParam(defaultValue = "30") int diasAdelante) {
        FlujoCajaResponse respuesta = service.combinado(diasAtras, diasAdelante);
        List<List<Object>> filas = aFilas(respuesta.consolidado());
        ContextoReporte contexto = contexto(diasAtras, diasAdelante);
        StreamingResponseBody cuerpo = out -> reportExportService.exportarExcel(contexto, COLUMNAS, filas, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"flujo-de-caja.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(cuerpo);
    }

    @GetMapping("/exportar/pdf")
    public ResponseEntity<StreamingResponseBody> exportarPdf(
            @RequestParam(defaultValue = "30") int diasAtras, @RequestParam(defaultValue = "30") int diasAdelante) {
        FlujoCajaResponse respuesta = service.combinado(diasAtras, diasAdelante);
        List<List<Object>> filas = aFilas(respuesta.consolidado());
        ContextoReporte contexto = contexto(diasAtras, diasAdelante);
        StreamingResponseBody cuerpo = out -> {
            try {
                reportExportService.exportarPdf(contexto, COLUMNAS, filas, out);
            } catch (Exception e) {
                throw new IOException("No se pudo generar el PDF", e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"flujo-de-caja.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(cuerpo);
    }

    private List<List<Object>> aFilas(List<PuntoFlujoCaja> puntos) {
        return puntos.stream()
                .<List<Object>>map(p -> List.of(
                        p.fecha().toString(),
                        p.esReal() ? "Real" : "Proyectado",
                        p.saldoInicial(),
                        p.ingresos(),
                        p.egresos(),
                        p.saldoFinal(),
                        p.saldoNegativo() ? "Saldo negativo" : ""))
                .toList();
    }

    private ContextoReporte contexto(int diasAtras, int diasAdelante) {
        return ContextoReporte.de("Flujo de caja",
                "Real: últimos " + diasAtras + " día(s)", "Proyectado: próximos " + diasAdelante + " día(s)");
    }
}

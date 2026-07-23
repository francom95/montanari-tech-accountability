package com.montanaritech.contable.contabilidad.estadoresultados;

import com.montanaritech.contable.common.reporte.ContextoReporte;
import com.montanaritech.contable.common.reporte.ReportExportService;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.EstadoResultadosDtos.CuentaMonto;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.EstadoResultadosDtos.EstadoResultadosCalculado;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.EstadoResultadosDtos.EstadoResultadosPorProyectoResponse;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.EstadoResultadosDtos.EstadoResultadosResponse;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.EstadoResultadosDtos.LineaCalculada;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Estado de resultados (F7.3, molde PL-3): 4 vistas, comparativo mes vs anterior, drill-down y export (F7.1). */
@RestController
@RequestMapping("/api/v1/reportes/estado-resultados")
@RequiredArgsConstructor
@Tag(name = "EstadoResultados")
public class EstadoResultadosController {

    private static final List<String> COLUMNAS = List.of("Línea / Cuenta", "Monto");
    private static final Map<LineaEstadoResultados, String> ETIQUETA = Map.ofEntries(
            Map.entry(LineaEstadoResultados.INGRESOS_POR_VENTAS, "Ingresos por ventas"),
            Map.entry(LineaEstadoResultados.OTROS_INGRESOS_POR_VENTAS, "Otros ingresos por ventas"),
            Map.entry(LineaEstadoResultados.COSTOS_DE_PRESTACION_DE_SERVICIOS, "Costos de prestación de servicios"),
            Map.entry(LineaEstadoResultados.GASTOS_DE_COMERCIALIZACION, "Gastos de comercialización"),
            Map.entry(LineaEstadoResultados.GASTOS_DE_ADMINISTRACION, "Gastos de administración"),
            Map.entry(LineaEstadoResultados.GASTOS_FINANCIEROS, "Gastos financieros"),
            Map.entry(LineaEstadoResultados.IMPUESTOS, "Impuestos"),
            Map.entry(LineaEstadoResultados.OTROS_INGRESOS, "Otros ingresos"),
            Map.entry(LineaEstadoResultados.OTROS_EGRESOS, "Otros egresos"));

    private final EstadoResultadosService service;
    private final ReportExportService reportExportService;

    @GetMapping("/mes")
    public EstadoResultadosResponse porMes(@RequestParam int anio, @RequestParam int mes) {
        return service.porMes(anio, mes);
    }

    @GetMapping("/anio")
    public EstadoResultadosResponse porAnio(@RequestParam int anio) {
        return service.porAnio(anio);
    }

    @GetMapping("/acumulado")
    public EstadoResultadosResponse acumulado(@RequestParam int anio, @RequestParam int mes) {
        return service.acumulado(anio, mes);
    }

    @GetMapping("/por-proyecto")
    public EstadoResultadosPorProyectoResponse porProyecto(@RequestParam int anio, @RequestParam int mes) {
        return service.porProyecto(anio, mes);
    }

    @GetMapping("/mes/exportar/excel")
    public ResponseEntity<StreamingResponseBody> exportarMesExcel(@RequestParam int anio, @RequestParam int mes) {
        return exportarExcel(service.porMes(anio, mes), "Estado de resultados - " + mes + "/" + anio, "estado-resultados-mes");
    }

    @GetMapping("/mes/exportar/pdf")
    public ResponseEntity<StreamingResponseBody> exportarMesPdf(@RequestParam int anio, @RequestParam int mes) {
        return exportarPdf(service.porMes(anio, mes), "Estado de resultados - " + mes + "/" + anio, "estado-resultados-mes");
    }

    @GetMapping("/anio/exportar/excel")
    public ResponseEntity<StreamingResponseBody> exportarAnioExcel(@RequestParam int anio) {
        return exportarExcel(service.porAnio(anio), "Estado de resultados - " + anio, "estado-resultados-anio");
    }

    @GetMapping("/anio/exportar/pdf")
    public ResponseEntity<StreamingResponseBody> exportarAnioPdf(@RequestParam int anio) {
        return exportarPdf(service.porAnio(anio), "Estado de resultados - " + anio, "estado-resultados-anio");
    }

    @GetMapping("/acumulado/exportar/excel")
    public ResponseEntity<StreamingResponseBody> exportarAcumuladoExcel(@RequestParam int anio, @RequestParam int mes) {
        return exportarExcel(service.acumulado(anio, mes), "Estado de resultados acumulado - " + mes + "/" + anio, "estado-resultados-acumulado");
    }

    @GetMapping("/acumulado/exportar/pdf")
    public ResponseEntity<StreamingResponseBody> exportarAcumuladoPdf(@RequestParam int anio, @RequestParam int mes) {
        return exportarPdf(service.acumulado(anio, mes), "Estado de resultados acumulado - " + mes + "/" + anio, "estado-resultados-acumulado");
    }

    @GetMapping("/por-proyecto/exportar/excel")
    public ResponseEntity<StreamingResponseBody> exportarPorProyectoExcel(@RequestParam int anio, @RequestParam int mes) {
        List<List<Object>> filas = aFilasPorProyecto(service.porProyecto(anio, mes));
        ContextoReporte contexto = ContextoReporte.de("Estado de resultados por proyecto - " + mes + "/" + anio);
        StreamingResponseBody cuerpo = out -> reportExportService.exportarExcel(contexto, COLUMNAS, filas, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"estado-resultados-por-proyecto.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(cuerpo);
    }

    @GetMapping("/por-proyecto/exportar/pdf")
    public ResponseEntity<StreamingResponseBody> exportarPorProyectoPdf(@RequestParam int anio, @RequestParam int mes) {
        List<List<Object>> filas = aFilasPorProyecto(service.porProyecto(anio, mes));
        ContextoReporte contexto = ContextoReporte.de("Estado de resultados por proyecto - " + mes + "/" + anio);
        StreamingResponseBody cuerpo = out -> {
            try {
                reportExportService.exportarPdf(contexto, COLUMNAS, filas, out);
            } catch (Exception e) {
                throw new IOException("No se pudo generar el PDF", e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"estado-resultados-por-proyecto.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(cuerpo);
    }

    private ResponseEntity<StreamingResponseBody> exportarExcel(EstadoResultadosResponse respuesta, String titulo, String nombreArchivo) {
        List<List<Object>> filas = aFilas(respuesta);
        StreamingResponseBody cuerpo = out -> reportExportService.exportarExcel(ContextoReporte.de(titulo), COLUMNAS, filas, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(cuerpo);
    }

    private ResponseEntity<StreamingResponseBody> exportarPdf(EstadoResultadosResponse respuesta, String titulo, String nombreArchivo) {
        List<List<Object>> filas = aFilas(respuesta);
        StreamingResponseBody cuerpo = out -> {
            try {
                reportExportService.exportarPdf(ContextoReporte.de(titulo), COLUMNAS, filas, out);
            } catch (Exception e) {
                throw new IOException("No se pudo generar el PDF", e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(cuerpo);
    }

    private List<List<Object>> aFilas(EstadoResultadosResponse respuesta) {
        List<List<Object>> filas = new ArrayList<>();
        EstadoResultadosCalculado c = respuesta.calculado();
        for (LineaCalculada linea : c.lineas()) {
            filas.add(List.of(ETIQUETA.get(linea.linea()), linea.monto()));
            for (CuentaMonto cuenta : linea.cuentas()) {
                filas.add(List.of("    " + cuenta.codigo() + " " + cuenta.nombre(), cuenta.monto()));
            }
        }
        if (c.montoSinMapear().signum() != 0 || !c.cuentasSinMapear().isEmpty()) {
            filas.add(List.of("Sin mapear (rubro sin línea asignada)", c.montoSinMapear()));
            for (CuentaMonto cuenta : c.cuentasSinMapear()) {
                filas.add(List.of("    " + cuenta.codigo() + " " + cuenta.nombre(), cuenta.monto()));
            }
        }
        filas.add(List.of("Resultado bruto", c.resultadoBruto()));
        filas.add(List.of("Resultado operativo", c.resultadoOperativo()));
        filas.add(List.of("Resultado final", c.resultadoFinal()));
        if (respuesta.comparativoMesAnterior() != null) {
            var comp = respuesta.comparativoMesAnterior();
            filas.add(List.of("Resultado final mes anterior (" + comp.mesAnterior() + "/" + comp.anioAnterior() + ")", comp.resultadoFinalAnterior()));
            filas.add(List.of("Variación absoluta", comp.variacionAbsoluta()));
            filas.add(List.of("Variación %", comp.variacionPorcentual() == null
                    ? "N/A (mes anterior en cero)"
                    : comp.variacionPorcentual().setScale(2, java.math.RoundingMode.HALF_UP) + " %"));
        }
        return filas;
    }

    private List<List<Object>> aFilasPorProyecto(EstadoResultadosPorProyectoResponse respuesta) {
        List<List<Object>> filas = new ArrayList<>();
        for (var item : respuesta.porProyecto()) {
            filas.add(List.of(item.proyectoNombre(), item.calculado().resultadoFinal()));
            for (LineaCalculada linea : item.calculado().lineas()) {
                if (linea.monto().signum() != 0) {
                    filas.add(List.of("    " + ETIQUETA.get(linea.linea()), linea.monto()));
                }
            }
        }
        filas.add(List.of("Sin proyecto", respuesta.sinProyecto().resultadoFinal()));
        return filas;
    }
}

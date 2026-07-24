package com.montanaritech.contable.maestros.proyecto.rentabilidad;

import com.montanaritech.contable.common.reporte.ContextoReporte;
import com.montanaritech.contable.common.reporte.ReportExportService;
import com.montanaritech.contable.maestros.proyecto.rentabilidad.dto.ReporteRentabilidadProyectoResponse;
import com.montanaritech.contable.maestros.proyecto.rentabilidad.dto.ReporteRentabilidadProyectoResponse.ComisionResumen;
import com.montanaritech.contable.maestros.proyecto.rentabilidad.dto.ReporteRentabilidadProyectoResponse.ProveedorResumen;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Reporte de rentabilidad por proyecto (F7.4, molde PL-3). */
@RestController
@RequestMapping("/api/v1/proyectos/{proyectoId}/reporte-rentabilidad")
@RequiredArgsConstructor
@Tag(name = "ReporteRentabilidadProyecto")
public class ReporteRentabilidadProyectoController {

    private static final List<String> COLUMNAS = List.of("Concepto", "Importe");

    private final ReporteRentabilidadProyectoService service;
    private final ReportExportService reportExportService;

    @GetMapping
    public ReporteRentabilidadProyectoResponse obtener(@PathVariable Long proyectoId) {
        return service.obtener(proyectoId);
    }

    @GetMapping("/exportar/excel")
    public ResponseEntity<StreamingResponseBody> exportarExcel(@PathVariable Long proyectoId) {
        ReporteRentabilidadProyectoResponse r = service.obtener(proyectoId);
        List<List<Object>> filas = aFilas(r);
        ContextoReporte contexto = ContextoReporte.de("Rentabilidad del proyecto - " + r.proyectoNombre());
        StreamingResponseBody cuerpo = out -> reportExportService.exportarExcel(contexto, COLUMNAS, filas, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-rentabilidad.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(cuerpo);
    }

    @GetMapping("/exportar/pdf")
    public ResponseEntity<StreamingResponseBody> exportarPdf(@PathVariable Long proyectoId) {
        ReporteRentabilidadProyectoResponse r = service.obtener(proyectoId);
        List<List<Object>> filas = aFilas(r);
        ContextoReporte contexto = ContextoReporte.de("Rentabilidad del proyecto - " + r.proyectoNombre());
        StreamingResponseBody cuerpo = out -> {
            try {
                reportExportService.exportarPdf(contexto, COLUMNAS, filas, out);
            } catch (Exception e) {
                throw new IOException("No se pudo generar el PDF", e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-rentabilidad.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(cuerpo);
    }

    private List<List<Object>> aFilas(ReporteRentabilidadProyectoResponse r) {
        List<List<Object>> filas = new ArrayList<>();
        filas.add(List.of("Proyecto", r.proyectoNombre()));
        filas.add(List.of("Cliente", r.clienteNombre()));
        filas.add(List.of("Estado", r.estado()));

        filas.add(List.of("Ingresos", ""));
        filas.add(List.of("    Total facturado (ARS)", r.totalFacturadoVentaArs()));
        filas.add(List.of("    Total cobrado (ARS)", r.totalCobradoArs()));
        filas.add(List.of("    Pendiente de cobro (ARS)", r.pendienteCobroArs()));

        filas.add(List.of("Egresos", ""));
        filas.add(List.of("    Total facturado de compra (ARS)", r.totalFacturadoCompraArs()));
        filas.add(List.of("    Total pagado (ARS)", r.totalPagadoArs()));
        filas.add(List.of("    Pendiente de pago (ARS)", r.pendientePagoArs()));
        for (ProveedorResumen p : r.proveedores()) {
            filas.add(List.of("    " + p.proveedorNombre(), p.pagadoArs()));
        }

        filas.add(List.of("Comisiones", ""));
        for (ComisionResumen c : r.comisiones()) {
            filas.add(List.of("    " + c.comisionistaNombre() + " (" + c.estadoPago() + ")",
                    (c.importeFinal() != null ? c.importeFinal() : c.importeEstimado()) + " " + c.monedaCodigo()));
        }
        filas.add(List.of("    Total comisiones (ARS)", r.comisionesArs()));

        filas.add(List.of("Impuestos atribuidos (ARS)", r.impuestosAtribuidosArs()));

        if (r.presupuesto() != null) {
            filas.add(List.of("Presupuesto vs. real", ""));
            filas.add(List.of("    Precio final presupuestado (USD)", r.presupuesto().calculado().precioFinalCliente()));
            filas.add(List.of("    Pagos emparejados con factura", r.presupuesto().pagosEmparejadosConFactura() + " de " + r.presupuesto().cantidadPagosPactados()));
            filas.add(List.of("    Presupuesto convertido (ARS, porción emparejada)", r.presupuesto().presupuestoConvertidoArs()));
            filas.add(List.of("    Facturado real (ARS, porción emparejada)", r.presupuesto().facturadoEmparejadoArs()));
            filas.add(List.of("    Diferencia (ARS)", r.presupuesto().diferenciaArs()));
        }

        filas.add(List.of("Margen real (ARS)", r.margenRealArs()));

        for (String advertencia : r.advertencias()) {
            filas.add(List.of("Advertencia", advertencia));
        }
        return filas;
    }
}

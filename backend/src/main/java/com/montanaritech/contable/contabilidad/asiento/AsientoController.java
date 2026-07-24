package com.montanaritech.contable.contabilidad.asiento;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.reporte.ContextoReporte;
import com.montanaritech.contable.common.reporte.FormatoReporte;
import com.montanaritech.contable.common.reporte.ReportExportService;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoAnularRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoCrearRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoEditarConfirmadoRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoEditarRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Libro diario (F7.6, molde PL-3 — mismo criterio que {@code CuentaPorCobrarController} de F4.5). */
@RestController
@RequestMapping("/api/v1/asientos")
@RequiredArgsConstructor
@Tag(name = "Asiento")
public class AsientoController {

    private static final List<String> COLUMNAS = List.of(
            "N°", "Fecha", "Descripción", "Estado", "Origen", "Debe", "Haber");

    private final AsientoService service;
    private final AsientoMapper mapper;
    private final ReportExportService reportExportService;

    @GetMapping
    public Page<AsientoResponse> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) EstadoDocumento estado,
            @RequestParam(required = false) OrigenAsiento origen,
            @RequestParam(required = false) Long numero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Long cuentaContableId,
            @RequestParam(required = false) BigDecimal importe,
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long proveedorId,
            Pageable p) {
        return service.listar(texto, estado, origen, numero, fechaDesde, fechaHasta, cuentaContableId, importe,
                proyectoId, clienteId, proveedorId, p).map(mapper::aResponse);
    }

    @GetMapping("/exportar/excel")
    public ResponseEntity<StreamingResponseBody> exportarExcel(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) EstadoDocumento estado,
            @RequestParam(required = false) OrigenAsiento origen,
            @RequestParam(required = false) Long numero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Long cuentaContableId,
            @RequestParam(required = false) BigDecimal importe,
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long proveedorId) {
        var pagina = service.listar(texto, estado, origen, numero, fechaDesde, fechaHasta, cuentaContableId, importe,
                proyectoId, clienteId, proveedorId, Pageable.unpaged());
        List<List<Object>> filas = aFilas(pagina);
        ContextoReporte contexto = contexto(texto, estado, origen, numero, fechaDesde, fechaHasta, cuentaContableId,
                importe, proyectoId, clienteId, proveedorId);
        StreamingResponseBody cuerpo = out -> reportExportService.exportarExcel(contexto, COLUMNAS, filas, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"libro-diario.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(cuerpo);
    }

    @GetMapping("/exportar/pdf")
    public ResponseEntity<StreamingResponseBody> exportarPdf(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) EstadoDocumento estado,
            @RequestParam(required = false) OrigenAsiento origen,
            @RequestParam(required = false) Long numero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Long cuentaContableId,
            @RequestParam(required = false) BigDecimal importe,
            @RequestParam(required = false) Long proyectoId,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long proveedorId) {
        var pagina = service.listar(texto, estado, origen, numero, fechaDesde, fechaHasta, cuentaContableId, importe,
                proyectoId, clienteId, proveedorId, Pageable.unpaged());
        List<List<Object>> filas = aFilas(pagina);
        ContextoReporte contexto = contexto(texto, estado, origen, numero, fechaDesde, fechaHasta, cuentaContableId,
                importe, proyectoId, clienteId, proveedorId);
        StreamingResponseBody cuerpo = out -> {
            try {
                reportExportService.exportarPdf(contexto, COLUMNAS, filas, out);
            } catch (Exception e) {
                throw new IOException("No se pudo generar el PDF", e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"libro-diario.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(cuerpo);
    }

    private List<List<Object>> aFilas(Page<Asiento> asientos) {
        return asientos.map(mapper::aResponse).stream()
                .<List<Object>>map(a -> List.of(
                        a.numero(),
                        a.fecha(),
                        a.descripcion(),
                        a.estado(),
                        a.origen(),
                        a.totalDebe(),
                        a.totalHaber()))
                .toList();
    }

    private ContextoReporte contexto(String texto, EstadoDocumento estado, OrigenAsiento origen, Long numero,
            LocalDate fechaDesde, LocalDate fechaHasta, Long cuentaContableId, BigDecimal importe, Long proyectoId,
            Long clienteId, Long proveedorId) {
        return ContextoReporte.de("Libro diario",
                texto == null ? null : "Texto: " + texto,
                estado == null ? null : "Estado: " + estado,
                origen == null ? null : "Origen: " + origen,
                numero == null ? null : "N°: " + numero,
                fechaDesde == null ? null : "Desde: " + FormatoReporte.formatearFecha(fechaDesde),
                fechaHasta == null ? null : "Hasta: " + FormatoReporte.formatearFecha(fechaHasta),
                cuentaContableId == null ? null : "Cuenta ID: " + cuentaContableId,
                importe == null ? null : "Importe: " + FormatoReporte.formatearMoneda(importe),
                proyectoId == null ? null : "Proyecto ID: " + proyectoId,
                clienteId == null ? null : "Cliente ID: " + clienteId,
                proveedorId == null ? null : "Proveedor ID: " + proveedorId);
    }

    @GetMapping("/{id}")
    public AsientoResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public AsientoResponse crear(@Valid @RequestBody AsientoCrearRequest req) {
        return mapper.aResponse(service.crearBorrador(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public AsientoResponse editar(@PathVariable Long id, @Valid @RequestBody AsientoEditarRequest req) {
        return mapper.aResponse(service.editarBorrador(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminarBorrador(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public AsientoResponse confirmar(@PathVariable Long id) {
        return mapper.aResponse(service.confirmar(id));
    }

    /**
     * Edita un asiento confirmado (F3.5, F3.1 §4.2). La restricción de que
     * solo ADMIN toque líneas {@code generada_auto = true} se valida dentro
     * del service (depende del contenido, no del endpoint).
     */
    @PutMapping("/{id}/confirmado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public AsientoResponse editarConfirmado(@PathVariable Long id, @Valid @RequestBody AsientoEditarConfirmadoRequest req) {
        return mapper.aResponse(service.editarConfirmado(id, req));
    }

    @PostMapping("/{id}/duplicar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public AsientoResponse duplicar(@PathVariable Long id) {
        return mapper.aResponse(service.duplicar(id));
    }

    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public AsientoResponse anular(@PathVariable Long id, @Valid @RequestBody AsientoAnularRequest req) {
        return mapper.aResponse(service.anular(id, req.motivo()));
    }
}

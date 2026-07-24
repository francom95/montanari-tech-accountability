package com.montanaritech.contable.maestros.proveedor;

import com.montanaritech.contable.common.reporte.ContextoReporte;
import com.montanaritech.contable.common.reporte.ReportExportService;
import com.montanaritech.contable.maestros.proveedor.dto.ProveedorCrearRequest;
import com.montanaritech.contable.maestros.proveedor.dto.ProveedorEditarRequest;
import com.montanaritech.contable.maestros.proveedor.dto.ProveedorResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Proveedores (F7.6, molde PL-3 — mismo criterio que {@code CuentaPorCobrarController} de F4.5). */
@RestController
@RequestMapping("/api/v1/proveedores")
@RequiredArgsConstructor
@Tag(name = "Proveedor")
public class ProveedorController {

    private static final List<String> COLUMNAS = List.of(
            "CUIT", "Nombre", "Jurisdicción", "Moneda habitual", "Condición IVA", "Tipos de costo",
            "Contacto", "Email", "Teléfono", "Cuenta CxP", "Estado");

    private final ProveedorService service;
    private final ProveedorMapper mapper;
    private final ReportExportService reportExportService;

    @GetMapping
    public Page<ProveedorResponse> listar(@RequestParam(required = false) String texto, @RequestParam(required = false) Boolean activo, Pageable p) {
        return service.listar(texto, activo, p).map(mapper::aResponse);
    }

    @GetMapping("/exportar/excel")
    public ResponseEntity<StreamingResponseBody> exportarExcel(
            @RequestParam(required = false) String texto, @RequestParam(required = false) Boolean activo) {
        List<List<Object>> filas = aFilas(service.listar(texto, activo, Pageable.unpaged()));
        ContextoReporte contexto = contexto(texto, activo);
        StreamingResponseBody cuerpo = out -> reportExportService.exportarExcel(contexto, COLUMNAS, filas, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"proveedores.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(cuerpo);
    }

    @GetMapping("/exportar/pdf")
    public ResponseEntity<StreamingResponseBody> exportarPdf(
            @RequestParam(required = false) String texto, @RequestParam(required = false) Boolean activo) {
        List<List<Object>> filas = aFilas(service.listar(texto, activo, Pageable.unpaged()));
        ContextoReporte contexto = contexto(texto, activo);
        StreamingResponseBody cuerpo = out -> {
            try {
                reportExportService.exportarPdf(contexto, COLUMNAS, filas, out);
            } catch (Exception e) {
                throw new IOException("No se pudo generar el PDF", e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"proveedores.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(cuerpo);
    }

    private List<List<Object>> aFilas(Page<Proveedor> proveedores) {
        return proveedores.map(mapper::aResponse).stream()
                .<List<Object>>map(p -> List.of(
                        p.cuit(),
                        p.nombre(),
                        p.jurisdiccionNombre(),
                        p.monedaHabitualCodigo(),
                        p.condicionIva() == null ? "" : p.condicionIva().name(),
                        p.tiposCosto().stream().map(ProveedorResponse.TipoCostoDto::nombre).collect(Collectors.joining(", ")),
                        p.contacto() == null ? "" : p.contacto(),
                        p.email() == null ? "" : p.email(),
                        p.telefono() == null ? "" : p.telefono(),
                        p.cuentaCxpCodigo() == null ? "" : p.cuentaCxpCodigo(),
                        p.activo() ? "Activo" : "Inactivo"))
                .toList();
    }

    private ContextoReporte contexto(String texto, Boolean activo) {
        return ContextoReporte.de("Proveedores",
                texto == null ? null : "Texto: " + texto,
                activo == null ? null : "Estado: " + (activo ? "Activo" : "Inactivo"));
    }

    @GetMapping("/{id}")
    public ProveedorResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ProveedorResponse crear(@Valid @RequestBody ProveedorCrearRequest req) {
        return mapper.aResponse(service.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ProveedorResponse editar(@PathVariable Long id, @Valid @RequestBody ProveedorEditarRequest req) {
        return mapper.aResponse(service.editar(id, req));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ProveedorResponse activar(@PathVariable Long id) {
        return mapper.aResponse(service.activar(id));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ProveedorResponse desactivar(@PathVariable Long id) {
        return mapper.aResponse(service.desactivar(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

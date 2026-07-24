package com.montanaritech.contable.maestros.cliente;

import com.montanaritech.contable.common.reporte.ContextoReporte;
import com.montanaritech.contable.common.reporte.ReportExportService;
import com.montanaritech.contable.maestros.cliente.dto.ClienteCrearRequest;
import com.montanaritech.contable.maestros.cliente.dto.ClienteEditarRequest;
import com.montanaritech.contable.maestros.cliente.dto.ClienteResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Clientes (F7.6, molde PL-3 — mismo criterio que {@code CuentaPorCobrarController} de F4.5). */
@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@Tag(name = "Cliente")
public class ClienteController {

    private static final List<String> COLUMNAS = List.of(
            "CUIT", "Nombre", "Jurisdicción", "Contacto", "Email", "Teléfono", "Cuenta CxC", "Estado");

    private final ClienteService service;
    private final ClienteMapper mapper;
    private final ReportExportService reportExportService;

    @GetMapping
    public Page<ClienteResponse> listar(@RequestParam(required = false) String texto, @RequestParam(required = false) Boolean activo, Pageable p) {
        return service.listar(texto, activo, p).map(mapper::aResponse);
    }

    @GetMapping("/exportar/excel")
    public ResponseEntity<StreamingResponseBody> exportarExcel(
            @RequestParam(required = false) String texto, @RequestParam(required = false) Boolean activo) {
        List<List<Object>> filas = aFilas(service.listar(texto, activo, Pageable.unpaged()));
        ContextoReporte contexto = contexto(texto, activo);
        StreamingResponseBody cuerpo = out -> reportExportService.exportarExcel(contexto, COLUMNAS, filas, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"clientes.xlsx\"")
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
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"clientes.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(cuerpo);
    }

    private List<List<Object>> aFilas(Page<Cliente> clientes) {
        return clientes.map(mapper::aResponse).stream()
                .<List<Object>>map(c -> List.of(
                        c.cuit(),
                        c.nombre(),
                        c.jurisdiccionNombre(),
                        c.contacto() == null ? "" : c.contacto(),
                        c.email() == null ? "" : c.email(),
                        c.telefono() == null ? "" : c.telefono(),
                        c.cuentaCxcCodigo() == null ? "" : c.cuentaCxcCodigo(),
                        c.activo() ? "Activo" : "Inactivo"))
                .toList();
    }

    private ContextoReporte contexto(String texto, Boolean activo) {
        return ContextoReporte.de("Clientes",
                texto == null ? null : "Texto: " + texto,
                activo == null ? null : "Estado: " + (activo ? "Activo" : "Inactivo"));
    }

    @GetMapping("/{id}")
    public ClienteResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ClienteResponse crear(@Valid @RequestBody ClienteCrearRequest req) {
        return mapper.aResponse(service.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ClienteResponse editar(@PathVariable Long id, @Valid @RequestBody ClienteEditarRequest req) {
        return mapper.aResponse(service.editar(id, req));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ClienteResponse activar(@PathVariable Long id) {
        return mapper.aResponse(service.activar(id));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ClienteResponse desactivar(@PathVariable Long id) {
        return mapper.aResponse(service.desactivar(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

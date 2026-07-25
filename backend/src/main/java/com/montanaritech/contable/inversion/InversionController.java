package com.montanaritech.contable.inversion;

import com.montanaritech.contable.inversion.dto.InversionCrearRequest;
import com.montanaritech.contable.inversion.dto.InversionEditarRequest;
import com.montanaritech.contable.inversion.dto.InversionResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Inversiones (Fondo Fima y similares, F8.4). */
@RestController
@RequestMapping("/api/v1/inversiones")
@RequiredArgsConstructor
@Tag(name = "Inversion")
public class InversionController {

    private final InversionService service;

    @GetMapping
    public Page<InversionResponse> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) EstadoInversion estado,
            @RequestParam(required = false) Boolean activo,
            Pageable p) {
        return service.listar(texto, estado, activo, p).map(service::aResponse);
    }

    @GetMapping("/{id}")
    public InversionResponse obtener(@PathVariable Long id) {
        return service.aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public InversionResponse crear(@Valid @RequestBody InversionCrearRequest req) {
        return service.aResponse(service.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public InversionResponse editar(@PathVariable Long id, @Valid @RequestBody InversionEditarRequest req) {
        return service.aResponse(service.editar(id, req));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public InversionResponse activar(@PathVariable Long id) {
        return service.aResponse(service.activar(id));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public InversionResponse desactivar(@PathVariable Long id) {
        return service.aResponse(service.desactivar(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

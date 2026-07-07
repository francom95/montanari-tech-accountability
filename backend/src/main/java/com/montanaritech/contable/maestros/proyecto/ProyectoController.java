package com.montanaritech.contable.maestros.proyecto;

import com.montanaritech.contable.maestros.proyecto.dto.ProyectoCrearRequest;
import com.montanaritech.contable.maestros.proyecto.dto.ProyectoEditarRequest;
import com.montanaritech.contable.maestros.proyecto.dto.ProyectoResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/proyectos")
@RequiredArgsConstructor
@Tag(name = "Proyecto")
public class ProyectoController {
    private final ProyectoService service;
    private final ProyectoMapper mapper;

    @GetMapping
    public Page<ProyectoResponse> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Long clienteId,
            Pageable p) {
        return service.listar(texto, activo, clienteId, p).map(mapper::aResponse);
    }

    @GetMapping("/{id}")
    public ProyectoResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ProyectoResponse crear(@Valid @RequestBody ProyectoCrearRequest req) {
        return mapper.aResponse(service.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ProyectoResponse editar(@PathVariable Long id, @Valid @RequestBody ProyectoEditarRequest req) {
        return mapper.aResponse(service.editar(id, req));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ProyectoResponse activar(@PathVariable Long id) {
        return mapper.aResponse(service.activar(id));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ProyectoResponse desactivar(@PathVariable Long id) {
        return mapper.aResponse(service.desactivar(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

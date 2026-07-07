package com.montanaritech.contable.maestros.proyecto.comision;

import com.montanaritech.contable.maestros.proyecto.comision.dto.ComisionProyectoCrearRequest;
import com.montanaritech.contable.maestros.proyecto.comision.dto.ComisionProyectoEditarRequest;
import com.montanaritech.contable.maestros.proyecto.comision.dto.ComisionProyectoResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/proyectos/{proyectoId}/comisiones")
@RequiredArgsConstructor
@Tag(name = "ComisionProyecto")
public class ComisionProyectoController {
    private final ComisionProyectoService service;
    private final ComisionProyectoMapper mapper;

    @GetMapping
    public Page<ComisionProyectoResponse> listar(
            @PathVariable Long proyectoId, @RequestParam(required = false) Boolean activo, Pageable p) {
        return service.listar(proyectoId, activo, p).map(mapper::aResponse);
    }

    @GetMapping("/{id}")
    public ComisionProyectoResponse obtener(@PathVariable Long proyectoId, @PathVariable Long id) {
        return mapper.aResponse(service.obtener(proyectoId, id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ComisionProyectoResponse crear(@PathVariable Long proyectoId, @Valid @RequestBody ComisionProyectoCrearRequest req) {
        return mapper.aResponse(service.crear(proyectoId, req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ComisionProyectoResponse editar(@PathVariable Long proyectoId, @PathVariable Long id, @Valid @RequestBody ComisionProyectoEditarRequest req) {
        return mapper.aResponse(service.editar(proyectoId, id, req));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ComisionProyectoResponse activar(@PathVariable Long proyectoId, @PathVariable Long id) {
        return mapper.aResponse(service.activar(proyectoId, id));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ComisionProyectoResponse desactivar(@PathVariable Long proyectoId, @PathVariable Long id) {
        return mapper.aResponse(service.desactivar(proyectoId, id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long proyectoId, @PathVariable Long id) {
        service.eliminar(proyectoId, id);
        return ResponseEntity.noContent().build();
    }
}

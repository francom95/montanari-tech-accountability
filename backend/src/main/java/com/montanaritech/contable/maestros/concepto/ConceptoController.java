package com.montanaritech.contable.maestros.concepto;
import com.montanaritech.contable.maestros.concepto.dto.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/conceptos")
@RequiredArgsConstructor
@Tag(name = "Concepto")
public class ConceptoController {
    private final ConceptoService service;
    private final ConceptoMapper mapper;
    @GetMapping
    public Page<ConceptoResponse> listar(@RequestParam(required = false) String texto, @RequestParam(required = false) Boolean activo, Pageable p) {
        return service.listar(texto, activo, p).map(mapper::aResponse);
    }
    @GetMapping("/{id}")
    public ConceptoResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ConceptoResponse crear(@Valid @RequestBody ConceptoCrearRequest req) {
        return mapper.aResponse(service.crear(req));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ConceptoResponse editar(@PathVariable Long id, @Valid @RequestBody ConceptoEditarRequest req) {
        return mapper.aResponse(service.editar(id, req));
    }
    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ConceptoResponse activar(@PathVariable Long id) {
        return mapper.aResponse(service.activar(id));
    }
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ConceptoResponse desactivar(@PathVariable Long id) {
        return mapper.aResponse(service.desactivar(id));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

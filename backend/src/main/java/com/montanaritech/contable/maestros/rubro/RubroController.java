package com.montanaritech.contable.maestros.rubro;
import com.montanaritech.contable.maestros.rubro.dto.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/rubros")
@RequiredArgsConstructor
@Tag(name = "Rubro")
public class RubroController {
    private final RubroService service;
    private final RubroMapper mapper;
    @GetMapping
    public Page<RubroResponse> listar(@RequestParam(required = false) String texto, @RequestParam(required = false) Boolean activo, Pageable p) {
        return service.listar(texto, activo, p).map(mapper::aResponse);
    }
    @GetMapping("/{id}")
    public RubroResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public RubroResponse crear(@Valid @RequestBody RubroCrearRequest req) {
        return mapper.aResponse(service.crear(req));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public RubroResponse editar(@PathVariable Long id, @Valid @RequestBody RubroEditarRequest req) {
        return mapper.aResponse(service.editar(id, req));
    }
    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public RubroResponse activar(@PathVariable Long id) {
        return mapper.aResponse(service.activar(id));
    }
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public RubroResponse desactivar(@PathVariable Long id) {
        return mapper.aResponse(service.desactivar(id));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

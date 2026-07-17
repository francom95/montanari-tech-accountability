package com.montanaritech.contable.contabilidad.asiento;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoCrearRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoEditarRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/asientos")
@RequiredArgsConstructor
@Tag(name = "Asiento")
public class AsientoController {

    private final AsientoService service;
    private final AsientoMapper mapper;

    @GetMapping
    public Page<AsientoResponse> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) EstadoDocumento estado,
            Pageable p) {
        return service.listar(texto, estado, p).map(mapper::aResponse);
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
}

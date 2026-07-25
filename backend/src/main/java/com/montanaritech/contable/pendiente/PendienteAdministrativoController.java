package com.montanaritech.contable.pendiente;

import com.montanaritech.contable.pendiente.dto.PendienteAdministrativoCrearRequest;
import com.montanaritech.contable.pendiente.dto.PendienteAdministrativoEditarRequest;
import com.montanaritech.contable.pendiente.dto.PendienteAdministrativoResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Pendientes administrativos (F8.5): recordatorios, controles y revisiones fuera del flujo contable formal. */
@RestController
@RequestMapping("/api/v1/pendientes-administrativos")
@RequiredArgsConstructor
@Tag(name = "PendienteAdministrativo")
public class PendienteAdministrativoController {

    private final PendienteAdministrativoService service;
    private final PendienteAdministrativoMapper mapper;

    @GetMapping
    public Page<PendienteAdministrativoResponse> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) EstadoPendiente estado,
            @RequestParam(required = false) PrioridadPendiente prioridad,
            @RequestParam(required = false) Long responsableId,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean activo,
            Pageable p) {
        return service.listar(texto, estado, prioridad, responsableId, categoria, activo, p).map(mapper::aResponse);
    }

    @GetMapping("/proximos")
    public List<PendienteAdministrativoResponse> proximos(@RequestParam(defaultValue = "15") int dias) {
        return service.proximosAVencer(dias).stream().map(mapper::aResponse).toList();
    }

    @GetMapping("/{id}")
    public PendienteAdministrativoResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public PendienteAdministrativoResponse crear(@Valid @RequestBody PendienteAdministrativoCrearRequest req) {
        return mapper.aResponse(service.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public PendienteAdministrativoResponse editar(@PathVariable Long id, @Valid @RequestBody PendienteAdministrativoEditarRequest req) {
        return mapper.aResponse(service.editar(id, req));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public PendienteAdministrativoResponse activar(@PathVariable Long id) {
        return mapper.aResponse(service.activar(id));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public PendienteAdministrativoResponse desactivar(@PathVariable Long id) {
        return mapper.aResponse(service.desactivar(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

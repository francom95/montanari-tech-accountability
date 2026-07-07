package com.montanaritech.contable.contabilidad.cuentacontable;

import com.montanaritech.contable.contabilidad.cuentacontable.dto.CuentaContableCrearRequest;
import com.montanaritech.contable.contabilidad.cuentacontable.dto.CuentaContableEditarRequest;
import com.montanaritech.contable.contabilidad.cuentacontable.dto.CuentaContableNodo;
import com.montanaritech.contable.contabilidad.cuentacontable.dto.CuentaContableResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cuentas-contables")
@RequiredArgsConstructor
@Tag(name = "CuentaContable")
public class CuentaContableController {
    private final CuentaContableService service;
    private final CuentaContableMapper mapper;

    @GetMapping
    public Page<CuentaContableResponse> listar(@RequestParam(required = false) String texto, @RequestParam(required = false) Boolean activo, Pageable p) {
        return service.listar(texto, activo, p).map(mapper::aResponse);
    }

    @GetMapping("/arbol")
    public List<CuentaContableNodo> arbol() {
        return service.arbol();
    }

    @GetMapping("/{id}")
    public CuentaContableResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public CuentaContableResponse crear(@Valid @RequestBody CuentaContableCrearRequest req) {
        return mapper.aResponse(service.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public CuentaContableResponse editar(@PathVariable Long id, @Valid @RequestBody CuentaContableEditarRequest req) {
        return mapper.aResponse(service.editar(id, req));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public CuentaContableResponse activar(@PathVariable Long id) {
        return mapper.aResponse(service.activar(id));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public CuentaContableResponse desactivar(@PathVariable Long id) {
        return mapper.aResponse(service.desactivar(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CARGA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

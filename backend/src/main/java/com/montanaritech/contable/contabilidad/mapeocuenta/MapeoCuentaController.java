package com.montanaritech.contable.contabilidad.mapeocuenta;

import com.montanaritech.contable.contabilidad.mapeocuenta.dto.MapeoCuentaCrearRequest;
import com.montanaritech.contable.contabilidad.mapeocuenta.dto.MapeoCuentaEditarRequest;
import com.montanaritech.contable.contabilidad.mapeocuenta.dto.MapeoCuentaResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Mapeo concepto→cuenta (F4.1 §1): lectura para cualquier rol autenticado
 * (los generadores automáticos lo consultan); escritura solo ADMINISTRADOR
 * (configura cómo se contabiliza cada evento — igual criterio que
 * {@code UsuarioController}).
 */
@RestController
@RequestMapping("/api/v1/mapeos-cuenta")
@RequiredArgsConstructor
@Tag(name = "MapeoCuenta")
public class MapeoCuentaController {

    private final MapeoCuentaService service;
    private final MapeoCuentaMapper mapper;

    @GetMapping
    public Page<MapeoCuentaResponse> listar(
            @RequestParam(required = false) ConceptoContable concepto,
            @RequestParam(required = false) Boolean activo,
            Pageable p) {
        return service.listar(concepto, activo, p).map(mapper::aResponse);
    }

    @GetMapping("/{id}")
    public MapeoCuentaResponse obtener(@PathVariable Long id) {
        return mapper.aResponse(service.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public MapeoCuentaResponse crear(@Valid @RequestBody MapeoCuentaCrearRequest req) {
        return mapper.aResponse(service.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public MapeoCuentaResponse editar(@PathVariable Long id, @Valid @RequestBody MapeoCuentaEditarRequest req) {
        return mapper.aResponse(service.editar(id, req));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public MapeoCuentaResponse activar(@PathVariable Long id) {
        return mapper.aResponse(service.activar(id));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public MapeoCuentaResponse desactivar(@PathVariable Long id) {
        return mapper.aResponse(service.desactivar(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

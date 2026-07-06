package com.montanaritech.contable.auth;

import com.montanaritech.contable.auth.dto.CambiarPasswordRequest;
import com.montanaritech.contable.auth.dto.UsuarioCrearRequest;
import com.montanaritech.contable.auth.dto.UsuarioEditarRequest;
import com.montanaritech.contable.auth.dto.UsuarioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestión de usuarios: solo ADMINISTRADOR (funcional §14.1). Endpoint
 * representativo para probar la matriz de permisos por rol de F1.5; el
 * resto de reglas ("solo admin cierra períodos/edita asientos automáticos")
 * se aplican en sus propios módulos cuando existan (F3+, F9.3).
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioMapper usuarioMapper;

    @GetMapping
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return usuarioService.listar(pageable).map(usuarioMapper::aResponse);
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioMapper.aResponse(usuarioService.obtener(id));
    }

    @PostMapping
    public UsuarioResponse crear(@Valid @RequestBody UsuarioCrearRequest request) {
        return usuarioMapper.aResponse(usuarioService.crear(request));
    }

    @PutMapping("/{id}")
    public UsuarioResponse editar(@PathVariable Long id, @Valid @RequestBody UsuarioEditarRequest request) {
        return usuarioMapper.aResponse(usuarioService.editar(id, request));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> cambiarPassword(
            @PathVariable Long id, @Valid @RequestBody CambiarPasswordRequest request) {
        usuarioService.cambiarPassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activar")
    public UsuarioResponse activar(@PathVariable Long id) {
        return usuarioMapper.aResponse(usuarioService.activar(id));
    }

    /**
     * No hay hard-delete de usuarios: "eliminar" es desactivar. Borrar
     * físicamente rompería la trazabilidad de auditoria_log (quién hizo
     * qué) y refresh_token; ver F1.1 §4.
     */
    @PatchMapping("/{id}/desactivar")
    public UsuarioResponse desactivar(@PathVariable Long id) {
        return usuarioMapper.aResponse(usuarioService.desactivar(id));
    }
}

package com.montanaritech.contable.auth;

import com.montanaritech.contable.auth.dto.CambiarPasswordRequest;
import com.montanaritech.contable.auth.dto.UsuarioCrearRequest;
import com.montanaritech.contable.auth.dto.UsuarioEditarRequest;
import com.montanaritech.contable.auth.dto.UsuarioResponse;
import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.Auditado;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.ConflictoException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión de usuarios (funcional §14.1: solo administrador). No sigue todavía
 * la plantilla formal PL-1 (se define recién en F1.8); la forma —listar
 * paginado, activar/desactivar en vez de borrar cuando hay motivo de
 * negocio para no perder trazabilidad, 409 en vez de dejar el sistema en un
 * estado inválido— ya queda alineada a lo que PL-1 va a formalizar.
 *
 * <p>"Administra usuarios" está en la lista de operaciones sensibles
 * auditadas (F1.1 §14.2). {@code crear} usa {@link Auditado} (solo importa
 * el resultado); el resto llama a {@link AuditoriaService} a mano porque
 * necesitan el "antes" para el diff.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public Page<Usuario> listar(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario " + id + " no encontrado"));
    }

    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "Usuario")
    @Transactional
    public Usuario crear(UsuarioCrearRequest request) {
        usuarioRepository.findByEmail(request.email()).ifPresent(u -> {
            throw new ConflictoException("EMAIL_DUPLICADO", "Ya existe un usuario con ese email");
        });

        Usuario usuario = new Usuario();
        usuario.setEmail(request.email());
        usuario.setNombre(request.nombre());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(request.rol());
        usuario.setActivo(true);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario editar(Long id, UsuarioEditarRequest request) {
        Usuario usuario = obtener(id);
        UsuarioResponse antes = usuarioMapper.aResponse(usuario);

        if (usuario.getRol() == RolUsuario.ADMINISTRADOR && request.rol() != RolUsuario.ADMINISTRADOR) {
            exigirQueNoSeaElUltimoAdminActivo(usuario);
        }

        usuario.setNombre(request.nombre());
        usuario.setRol(request.rol());

        auditoriaService.registrar(
                AccionAuditoria.EDITAR, "Usuario", usuario.getId(), antes, usuarioMapper.aResponse(usuario));
        return usuario;
    }

    @Transactional
    public void cambiarPassword(Long id, CambiarPasswordRequest request) {
        Usuario usuario = obtener(id);
        usuario.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
        // Sin antes/despues: el hash nunca va a un log, ni siquiera indirectamente.
        auditoriaService.registrar(AccionAuditoria.EDITAR, "Usuario", usuario.getId(), null, null,
                false, "Cambio de contraseña");
    }

    @Transactional
    public Usuario desactivar(Long id) {
        Usuario usuario = obtener(id);
        if (usuario.getRol() == RolUsuario.ADMINISTRADOR) {
            exigirQueNoSeaElUltimoAdminActivo(usuario);
        }
        UsuarioResponse antes = usuarioMapper.aResponse(usuario);
        usuario.setActivo(false);
        auditoriaService.registrar(
                AccionAuditoria.CAMBIO_ESTADO, "Usuario", usuario.getId(), antes, usuarioMapper.aResponse(usuario));
        return usuario;
    }

    @Transactional
    public Usuario activar(Long id) {
        Usuario usuario = obtener(id);
        UsuarioResponse antes = usuarioMapper.aResponse(usuario);
        usuario.setActivo(true);
        auditoriaService.registrar(
                AccionAuditoria.CAMBIO_ESTADO, "Usuario", usuario.getId(), antes, usuarioMapper.aResponse(usuario));
        return usuario;
    }

    /**
     * Invariante de negocio (no viene del funcional, pero es la consecuencia
     * lógica de "solo admin administra usuarios": si se permite desactivar o
     * degradar al último admin activo, el sistema queda sin nadie que pueda
     * volver a administrar usuarios).
     */
    private void exigirQueNoSeaElUltimoAdminActivo(Usuario usuario) {
        List<Usuario> admins = usuarioRepository.findAll().stream()
                .filter(Usuario::isActivo)
                .filter(u -> u.getRol() == RolUsuario.ADMINISTRADOR)
                .filter(u -> !u.getId().equals(usuario.getId()))
                .toList();

        if (admins.isEmpty()) {
            throw new ConflictoException(
                    "ULTIMO_ADMIN",
                    "No se puede desactivar ni degradar al único administrador activo");
        }
    }
}

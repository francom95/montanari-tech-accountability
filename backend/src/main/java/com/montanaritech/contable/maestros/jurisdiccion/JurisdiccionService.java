package com.montanaritech.contable.maestros.jurisdiccion;
import com.montanaritech.contable.common.audit.*;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.jurisdiccion.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class JurisdiccionService {
    private final JurisdiccionRepository repo;
    private final JurisdiccionMapper mapper;
    private final AuditoriaService auditoria;
    @Transactional(readOnly = true)
    public Page<Jurisdiccion> listar(String texto, Boolean activo, Pageable p) {
        return repo.buscar(texto, activo, p);
    }
    @Transactional(readOnly = true)
    public Jurisdiccion obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Jurisdiccion " + id + " no encontrado"));
    }
    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "Jurisdiccion")
    @Transactional
    public Jurisdiccion crear(JurisdiccionCrearRequest req) {
        Jurisdiccion e = new Jurisdiccion();
        e.setNombre(req.nombre());
        e.setDescripcion(req.descripcion());
        e.setActivo(true);
        return repo.save(e);
    }
    @Transactional
    public Jurisdiccion editar(Long id, JurisdiccionEditarRequest req) {
        Jurisdiccion e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setNombre(req.nombre());
        e.setDescripcion(req.descripcion());
        auditoria.registrar(AccionAuditoria.EDITAR, "Jurisdiccion", id, antes, mapper.aResponse(e));
        return e;
    }
    @Transactional
    public Jurisdiccion desactivar(Long id) {
        Jurisdiccion e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Jurisdiccion", id, antes, mapper.aResponse(e));
        return e;
    }
    @Transactional
    public Jurisdiccion activar(Long id) {
        Jurisdiccion e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Jurisdiccion", id, antes, mapper.aResponse(e));
        return e;
    }
    @Transactional
    public void eliminar(Long id) {
        Jurisdiccion e = obtener(id);
        var antes = mapper.aResponse(e);
        repo.delete(e);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "Jurisdiccion", id, antes, null);
    }
}

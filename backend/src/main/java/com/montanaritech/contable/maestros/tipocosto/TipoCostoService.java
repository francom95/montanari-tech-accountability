package com.montanaritech.contable.maestros.tipocosto;
import com.montanaritech.contable.common.audit.*;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.tipocosto.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class TipoCostoService {
    private final TipoCostoRepository repo;
    private final TipoCostoMapper mapper;
    private final AuditoriaService auditoria;
    @Transactional(readOnly = true)
    public Page<TipoCosto> listar(String texto, Boolean activo, Pageable p) {
        return repo.buscar(texto, activo, p);
    }
    @Transactional(readOnly = true)
    public TipoCosto obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("TipoCosto " + id + " no encontrado"));
    }
    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "TipoCosto")
    @Transactional
    public TipoCosto crear(TipoCostoCrearRequest req) {
        TipoCosto e = new TipoCosto();
        e.setNombre(req.nombre());
        e.setDescripcion(req.descripcion());
        e.setActivo(true);
        return repo.save(e);
    }
    @Transactional
    public TipoCosto editar(Long id, TipoCostoEditarRequest req) {
        TipoCosto e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setNombre(req.nombre());
        e.setDescripcion(req.descripcion());
        auditoria.registrar(AccionAuditoria.EDITAR, "TipoCosto", id, antes, mapper.aResponse(e));
        return e;
    }
    @Transactional
    public TipoCosto desactivar(Long id) {
        TipoCosto e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "TipoCosto", id, antes, mapper.aResponse(e));
        return e;
    }
    @Transactional
    public TipoCosto activar(Long id) {
        TipoCosto e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "TipoCosto", id, antes, mapper.aResponse(e));
        return e;
    }
    @Transactional
    public void eliminar(Long id) {
        TipoCosto e = obtener(id);
        var antes = mapper.aResponse(e);
        repo.delete(e);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "TipoCosto", id, antes, null);
    }
}

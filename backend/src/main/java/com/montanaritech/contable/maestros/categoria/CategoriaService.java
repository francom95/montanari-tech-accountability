package com.montanaritech.contable.maestros.categoria;
import com.montanaritech.contable.common.audit.*;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.categoria.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class CategoriaService {
    private final CategoriaRepository repo;
    private final CategoriaMapper mapper;
    private final AuditoriaService auditoria;
    @Transactional(readOnly = true)
    public Page<Categoria> listar(String texto, Boolean activo, Pageable p) {
        return repo.buscar(texto, activo, p);
    }
    @Transactional(readOnly = true)
    public Categoria obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Categoria " + id + " no encontrado"));
    }
    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "Categoria")
    @Transactional
    public Categoria crear(CategoriaCrearRequest req) {
        Categoria e = new Categoria();
        e.setNombre(req.nombre());
        e.setDescripcion(req.descripcion());
        e.setTipo(Categoria.TipoCategoria.valueOf(req.tipo()));
        e.setActivo(true);
        return repo.save(e);
    }
    @Transactional
    public Categoria editar(Long id, CategoriaEditarRequest req) {
        Categoria e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setNombre(req.nombre());
        e.setDescripcion(req.descripcion());
        e.setTipo(Categoria.TipoCategoria.valueOf(req.tipo()));
        auditoria.registrar(AccionAuditoria.EDITAR, "Categoria", id, antes, mapper.aResponse(e));
        return e;
    }
    @Transactional
    public Categoria desactivar(Long id) {
        Categoria e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Categoria", id, antes, mapper.aResponse(e));
        return e;
    }
    @Transactional
    public Categoria activar(Long id) {
        Categoria e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Categoria", id, antes, mapper.aResponse(e));
        return e;
    }
    @Transactional
    public void eliminar(Long id) {
        Categoria e = obtener(id);
        var antes = mapper.aResponse(e);
        repo.delete(e);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "Categoria", id, antes, null);
    }
}

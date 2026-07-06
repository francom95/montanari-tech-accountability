package com.montanaritech.contable.maestros.rubro;
import com.montanaritech.contable.common.audit.*;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.categoria.Categoria;
import com.montanaritech.contable.maestros.categoria.CategoriaRepository;
import com.montanaritech.contable.maestros.rubro.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class RubroService {
    private final RubroRepository repo;
    private final CategoriaRepository categoriaRepository;
    private final RubroMapper mapper;
    private final AuditoriaService auditoria;
    @Transactional(readOnly = true)
    public Page<Rubro> listar(String texto, Boolean activo, Pageable p) {
        return repo.buscar(texto, activo, p);
    }
    @Transactional(readOnly = true)
    public Rubro obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Rubro " + id + " no encontrado"));
    }
    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "Rubro")
    @Transactional
    public Rubro crear(RubroCrearRequest req) {
        Categoria categoria = categoriaRepository.findById(req.categoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoria " + req.categoriaId() + " no encontrada"));
        Rubro e = new Rubro();
        e.setNombre(req.nombre());
        e.setCategoria(categoria);
        e.setOrden(req.orden());
        e.setActivo(true);
        return repo.save(e);
    }
    @Transactional
    public Rubro editar(Long id, RubroEditarRequest req) {
        Rubro e = obtener(id);
        var antes = mapper.aResponse(e);
        Categoria categoria = categoriaRepository.findById(req.categoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoria " + req.categoriaId() + " no encontrada"));
        e.setNombre(req.nombre());
        e.setCategoria(categoria);
        e.setOrden(req.orden());
        auditoria.registrar(AccionAuditoria.EDITAR, "Rubro", id, antes, mapper.aResponse(e));
        return e;
    }
    @Transactional
    public Rubro desactivar(Long id) {
        Rubro e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Rubro", id, antes, mapper.aResponse(e));
        return e;
    }
    @Transactional
    public Rubro activar(Long id) {
        Rubro e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Rubro", id, antes, mapper.aResponse(e));
        return e;
    }
    @Transactional
    public void eliminar(Long id) {
        Rubro e = obtener(id);
        var antes = mapper.aResponse(e);
        repo.delete(e);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "Rubro", id, antes, null);
    }
}

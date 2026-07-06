package com.montanaritech.contable.maestros.concepto;
import com.montanaritech.contable.common.audit.*;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.concepto.dto.*;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class ConceptoService {
    private final ConceptoRepository repo;
    private final MonedaRepository monedaRepository;
    private final ConceptoMapper mapper;
    private final AuditoriaService auditoria;
    @Transactional(readOnly = true)
    public Page<Concepto> listar(String texto, Boolean activo, Pageable p) {
        return repo.buscar(texto, activo, p);
    }
    @Transactional(readOnly = true)
    public Concepto obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Concepto " + id + " no encontrado"));
    }
    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "Concepto")
    @Transactional
    public Concepto crear(ConceptoCrearRequest req) {
        Concepto e = new Concepto();
        e.setNombre(req.nombre());
        e.setDescripcion(req.descripcion());
        e.setCuentaSugerida(req.cuentaSugerida());
        e.setPeriodicidad(req.periodicidad());
        e.setImporte(req.importe());
        e.setMoneda(resolverMoneda(req.monedaId()));
        e.setActivo(true);
        return repo.save(e);
    }
    @Transactional
    public Concepto editar(Long id, ConceptoEditarRequest req) {
        Concepto e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setNombre(req.nombre());
        e.setDescripcion(req.descripcion());
        e.setCuentaSugerida(req.cuentaSugerida());
        e.setPeriodicidad(req.periodicidad());
        e.setImporte(req.importe());
        e.setMoneda(resolverMoneda(req.monedaId()));
        auditoria.registrar(AccionAuditoria.EDITAR, "Concepto", id, antes, mapper.aResponse(e));
        return e;
    }

    private Moneda resolverMoneda(Long monedaId) {
        if (monedaId == null) {
            return null;
        }
        return monedaRepository.findById(monedaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + monedaId + " no encontrada"));
    }
    @Transactional
    public Concepto desactivar(Long id) {
        Concepto e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Concepto", id, antes, mapper.aResponse(e));
        return e;
    }
    @Transactional
    public Concepto activar(Long id) {
        Concepto e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Concepto", id, antes, mapper.aResponse(e));
        return e;
    }
    @Transactional
    public void eliminar(Long id) {
        Concepto e = obtener(id);
        var antes = mapper.aResponse(e);
        repo.delete(e);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "Concepto", id, antes, null);
    }
}

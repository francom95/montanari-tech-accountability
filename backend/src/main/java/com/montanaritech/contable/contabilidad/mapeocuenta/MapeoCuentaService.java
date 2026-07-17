package com.montanaritech.contable.contabilidad.mapeocuenta;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.contabilidad.mapeocuenta.dto.MapeoCuentaCrearRequest;
import com.montanaritech.contable.contabilidad.mapeocuenta.dto.MapeoCuentaEditarRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD del mapeo concepto→cuenta (F4.1 §1), editable por ADMINISTRADOR. */
@Service
@RequiredArgsConstructor
public class MapeoCuentaService {

    private final MapeoCuentaRepository repo;
    private final MapeoCuentaMapper mapper;
    private final CuentaContableRepository cuentaRepo;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public Page<MapeoCuenta> listar(ConceptoContable concepto, Boolean activo, Pageable p) {
        return repo.buscar(concepto, activo, p);
    }

    @Transactional(readOnly = true)
    public MapeoCuenta obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Mapeo de cuenta " + id + " no encontrado"));
    }

    @Transactional
    public MapeoCuenta crear(MapeoCuentaCrearRequest req) {
        MapeoCuenta m = new MapeoCuenta();
        m.setConcepto(req.concepto());
        m.setDiscriminadorTipo(req.discriminadorTipo());
        m.setDiscriminadorValor(req.discriminadorValor());
        m.setCuentaContable(resolverCuenta(req.cuentaContableId()));
        m.setActivo(true);
        try {
            MapeoCuenta guardado = repo.save(m);
            auditoria.registrar(AccionAuditoria.CREAR, "MapeoCuenta", guardado.getId(), null, mapper.aResponse(guardado));
            return guardado;
        } catch (DataIntegrityViolationException e) {
            throw new NegocioException("MAPEO_DUPLICADO",
                    "Ya existe un mapeo para %s con ese discriminador".formatted(req.concepto()));
        }
    }

    @Transactional
    public MapeoCuenta editar(Long id, MapeoCuentaEditarRequest req) {
        MapeoCuenta m = obtener(id);
        var antes = mapper.aResponse(m);
        m.setCuentaContable(resolverCuenta(req.cuentaContableId()));
        auditoria.registrar(AccionAuditoria.EDITAR, "MapeoCuenta", id, antes, mapper.aResponse(m));
        return m;
    }

    @Transactional
    public MapeoCuenta desactivar(Long id) {
        MapeoCuenta m = obtener(id);
        var antes = mapper.aResponse(m);
        m.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "MapeoCuenta", id, antes, mapper.aResponse(m));
        return m;
    }

    @Transactional
    public MapeoCuenta activar(Long id) {
        MapeoCuenta m = obtener(id);
        var antes = mapper.aResponse(m);
        m.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "MapeoCuenta", id, antes, mapper.aResponse(m));
        return m;
    }

    @Transactional
    public void eliminar(Long id) {
        MapeoCuenta m = obtener(id);
        var antes = mapper.aResponse(m);
        repo.delete(m);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "MapeoCuenta", id, antes, null);
    }

    private CuentaContable resolverCuenta(Long id) {
        return cuentaRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cuenta contable " + id + " no encontrada"));
    }
}

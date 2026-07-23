package com.montanaritech.contable.bancos.tarjetacredito;

import com.montanaritech.contable.bancos.tarjetacredito.dto.ReglaClasificacionCrearRequest;
import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.maestros.concepto.Concepto;
import com.montanaritech.contable.maestros.concepto.ConceptoRepository;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD de reglas de clasificación masiva de consumos (F5.4 §2). */
@Service
@RequiredArgsConstructor
public class ReglaClasificacionConsumoService {

    private final ReglaClasificacionConsumoRepository repo;
    private final CuentaContableRepository cuentaContableRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProyectoRepository proyectoRepository;
    private final ConceptoRepository conceptoRepository;
    private final ReglaClasificacionMapper mapper;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public Page<ReglaClasificacionConsumo> listar(Boolean activo, Pageable p) {
        return repo.buscar(activo, p);
    }

    @Transactional(readOnly = true)
    public ReglaClasificacionConsumo obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Regla de clasificación " + id + " no encontrada"));
    }

    @Transactional
    public ReglaClasificacionConsumo crear(ReglaClasificacionCrearRequest req) {
        ReglaClasificacionConsumo r = new ReglaClasificacionConsumo();
        aplicarCampos(r, req);
        r.setActivo(true);
        ReglaClasificacionConsumo guardada = repo.save(r);
        auditoria.registrar(AccionAuditoria.CREAR, "ReglaClasificacionConsumo", guardada.getId(), null, mapper.aResponse(guardada));
        return guardada;
    }

    @Transactional
    public ReglaClasificacionConsumo editar(Long id, ReglaClasificacionCrearRequest req) {
        ReglaClasificacionConsumo r = obtener(id);
        var antes = mapper.aResponse(r);
        aplicarCampos(r, req);
        auditoria.registrar(AccionAuditoria.EDITAR, "ReglaClasificacionConsumo", id, antes, mapper.aResponse(r));
        return r;
    }

    @Transactional
    public ReglaClasificacionConsumo desactivar(Long id) {
        ReglaClasificacionConsumo r = obtener(id);
        var antes = mapper.aResponse(r);
        r.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "ReglaClasificacionConsumo", id, antes, mapper.aResponse(r));
        return r;
    }

    @Transactional
    public ReglaClasificacionConsumo activar(Long id) {
        ReglaClasificacionConsumo r = obtener(id);
        var antes = mapper.aResponse(r);
        r.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "ReglaClasificacionConsumo", id, antes, mapper.aResponse(r));
        return r;
    }

    private void aplicarCampos(ReglaClasificacionConsumo r, ReglaClasificacionCrearRequest req) {
        r.setPatron(req.patron());
        r.setCuentaContable(resolverCuentaContable(req.cuentaContableId()));
        r.setProveedor(req.proveedorId() != null ? resolverProveedor(req.proveedorId()) : null);
        r.setProyecto(req.proyectoId() != null ? resolverProyecto(req.proyectoId()) : null);
        r.setConcepto(req.conceptoId() != null ? resolverConcepto(req.conceptoId()) : null);
    }

    private CuentaContable resolverCuentaContable(Long id) {
        return cuentaContableRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cuenta contable " + id + " no encontrada"));
    }

    private Proveedor resolverProveedor(Long id) {
        return proveedorRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proveedor " + id + " no encontrado"));
    }

    private Proyecto resolverProyecto(Long id) {
        return proyectoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto " + id + " no encontrado"));
    }

    private Concepto resolverConcepto(Long id) {
        return conceptoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Concepto " + id + " no encontrado"));
    }
}

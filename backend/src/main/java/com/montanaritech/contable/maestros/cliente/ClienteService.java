package com.montanaritech.contable.maestros.cliente;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.audit.Auditado;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.maestros.cliente.dto.ClienteCrearRequest;
import com.montanaritech.contable.maestros.cliente.dto.ClienteEditarRequest;
import com.montanaritech.contable.maestros.jurisdiccion.JurisdiccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClienteService {
    private final ClienteRepository repo;
    private final ClienteMapper mapper;
    private final AuditoriaService auditoria;
    private final JurisdiccionRepository jurisdiccionRepo;
    private final CuentaContableRepository cuentaContableRepo;

    @Transactional(readOnly = true)
    public Page<Cliente> listar(String texto, Boolean activo, Pageable p) {
        return repo.buscar(texto, activo, p);
    }

    @Transactional(readOnly = true)
    public Cliente obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cliente " + id + " no encontrado"));
    }

    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "Cliente")
    @Transactional
    public Cliente crear(ClienteCrearRequest req) {
        var jurisdiccion = jurisdiccionRepo.findById(req.jurisdiccionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Jurisdicción " + req.jurisdiccionId() + " no encontrada"));

        Cliente e = new Cliente();
        e.setNombre(req.nombre());
        e.setCuit(req.cuit());
        e.setJurisdiccion(jurisdiccion);
        e.setContacto(req.contacto());
        e.setEmail(req.email());
        e.setTelefono(req.telefono());
        e.setCuentaCxc(resolverCuentaCxc(req.cuentaCxcId()));
        e.setActivo(true);
        return repo.save(e);
    }

    @Transactional
    public Cliente editar(Long id, ClienteEditarRequest req) {
        Cliente e = obtener(id);
        var jurisdiccion = jurisdiccionRepo.findById(req.jurisdiccionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Jurisdicción " + req.jurisdiccionId() + " no encontrada"));
        var antes = mapper.aResponse(e);

        e.setNombre(req.nombre());
        e.setJurisdiccion(jurisdiccion);
        e.setContacto(req.contacto());
        e.setEmail(req.email());
        e.setTelefono(req.telefono());
        e.setCuentaCxc(resolverCuentaCxc(req.cuentaCxcId()));

        auditoria.registrar(AccionAuditoria.EDITAR, "Cliente", id, antes, mapper.aResponse(e));
        return e;
    }

    private CuentaContable resolverCuentaCxc(Long cuentaCxcId) {
        if (cuentaCxcId == null) {
            return null;
        }
        return cuentaContableRepo.findById(cuentaCxcId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta contable " + cuentaCxcId + " no encontrada"));
    }

    @Transactional
    public Cliente desactivar(Long id) {
        Cliente e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Cliente", id, antes, mapper.aResponse(e));
        return e;
    }

    @Transactional
    public Cliente activar(Long id) {
        Cliente e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Cliente", id, antes, mapper.aResponse(e));
        return e;
    }

    @Transactional
    public void eliminar(Long id) {
        Cliente e = obtener(id);
        var antes = mapper.aResponse(e);
        repo.delete(e);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "Cliente", id, antes, null);
    }
}

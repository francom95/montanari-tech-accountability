package com.montanaritech.contable.maestros.proveedor;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.audit.Auditado;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.maestros.jurisdiccion.JurisdiccionRepository;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proveedor.dto.ProveedorCrearRequest;
import com.montanaritech.contable.maestros.proveedor.dto.ProveedorEditarRequest;
import com.montanaritech.contable.maestros.tipocosto.TipoCostoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProveedorService {
    private final ProveedorRepository repo;
    private final ProveedorMapper mapper;
    private final AuditoriaService auditoria;
    private final JurisdiccionRepository jurisdiccionRepo;
    private final MonedaRepository monedaRepo;
    private final TipoCostoRepository tipoCostoRepo;
    private final CuentaContableRepository cuentaContableRepo;

    @Transactional(readOnly = true)
    public Page<Proveedor> listar(String texto, Boolean activo, Pageable p) {
        return repo.buscar(texto, activo, p);
    }

    @Transactional(readOnly = true)
    public Proveedor obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proveedor " + id + " no encontrado"));
    }

    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "Proveedor")
    @Transactional
    public Proveedor crear(ProveedorCrearRequest req) {
        var jurisdiccion = jurisdiccionRepo.findById(req.jurisdiccionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Jurisdicción " + req.jurisdiccionId() + " no encontrada"));

        Proveedor e = new Proveedor();
        e.setNombre(req.nombre());
        e.setCuit(req.cuit());
        e.setJurisdiccion(jurisdiccion);

        if (req.monedaHabitualId() != null) {
            var moneda = monedaRepo.findById(req.monedaHabitualId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + req.monedaHabitualId() + " no encontrada"));
            e.setMonedaHabitual(moneda);
        }

        if (req.tiposCostoIds() != null && !req.tiposCostoIds().isEmpty()) {
            var tiposCosto = tipoCostoRepo.findAllById(req.tiposCostoIds());
            e.setTiposCosto(tiposCosto.stream().collect(java.util.stream.Collectors.toSet()));
        }

        e.setContacto(req.contacto());
        e.setEmail(req.email());
        e.setTelefono(req.telefono());
        e.setCondicionIva(req.condicionIva() != null ? req.condicionIva() : CondicionIva.RESPONSABLE_INSCRIPTO);
        e.setCuentaCxp(resolverCuentaCxp(req.cuentaCxpId()));
        e.setActivo(true);
        return repo.save(e);
    }

    @Transactional
    public Proveedor editar(Long id, ProveedorEditarRequest req) {
        Proveedor e = obtener(id);
        var jurisdiccion = jurisdiccionRepo.findById(req.jurisdiccionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Jurisdicción " + req.jurisdiccionId() + " no encontrada"));
        var antes = mapper.aResponse(e);

        e.setNombre(req.nombre());
        e.setJurisdiccion(jurisdiccion);

        if (req.monedaHabitualId() != null) {
            var moneda = monedaRepo.findById(req.monedaHabitualId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + req.monedaHabitualId() + " no encontrada"));
            e.setMonedaHabitual(moneda);
        } else {
            e.setMonedaHabitual(null);
        }

        if (req.tiposCostoIds() != null && !req.tiposCostoIds().isEmpty()) {
            var tiposCosto = tipoCostoRepo.findAllById(req.tiposCostoIds());
            e.setTiposCosto(tiposCosto.stream().collect(java.util.stream.Collectors.toSet()));
        } else {
            e.setTiposCosto(new java.util.HashSet<>());
        }

        e.setContacto(req.contacto());
        e.setEmail(req.email());
        e.setTelefono(req.telefono());
        e.setCondicionIva(req.condicionIva() != null ? req.condicionIva() : CondicionIva.RESPONSABLE_INSCRIPTO);
        e.setCuentaCxp(resolverCuentaCxp(req.cuentaCxpId()));

        auditoria.registrar(AccionAuditoria.EDITAR, "Proveedor", id, antes, mapper.aResponse(e));
        return e;
    }

    private CuentaContable resolverCuentaCxp(Long cuentaCxpId) {
        if (cuentaCxpId == null) {
            return null;
        }
        return cuentaContableRepo.findById(cuentaCxpId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta contable " + cuentaCxpId + " no encontrada"));
    }

    @Transactional
    public Proveedor desactivar(Long id) {
        Proveedor e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Proveedor", id, antes, mapper.aResponse(e));
        return e;
    }

    @Transactional
    public Proveedor activar(Long id) {
        Proveedor e = obtener(id);
        var antes = mapper.aResponse(e);
        e.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Proveedor", id, antes, mapper.aResponse(e));
        return e;
    }

    @Transactional
    public void eliminar(Long id) {
        Proveedor e = obtener(id);
        var antes = mapper.aResponse(e);
        repo.delete(e);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "Proveedor", id, antes, null);
    }
}

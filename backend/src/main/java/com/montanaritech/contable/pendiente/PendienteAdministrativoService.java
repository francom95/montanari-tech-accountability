package com.montanaritech.contable.pendiente;

import com.montanaritech.contable.auth.Usuario;
import com.montanaritech.contable.auth.UsuarioRepository;
import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.cliente.ClienteRepository;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.pendiente.dto.PendienteAdministrativoCrearRequest;
import com.montanaritech.contable.pendiente.dto.PendienteAdministrativoEditarRequest;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD de {@link PendienteAdministrativo} (F8.5), molde PL-1 sobre F1.8. */
@Service
@RequiredArgsConstructor
public class PendienteAdministrativoService {

    private final PendienteAdministrativoRepository repo;
    private final UsuarioRepository usuarioRepo;
    private final ProyectoRepository proyectoRepo;
    private final ClienteRepository clienteRepo;
    private final ProveedorRepository proveedorRepo;
    private final PendienteAdministrativoMapper mapper;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public Page<PendienteAdministrativo> listar(String texto, EstadoPendiente estado, PrioridadPendiente prioridad,
            Long responsableId, String categoria, Boolean activo, Pageable p) {
        return repo.buscar(texto, estado, prioridad, responsableId, categoria, activo, p);
    }

    /** F8.5: query service para que F9.1 (alertas) proyecte los pendientes próximos a vencer. */
    @Transactional(readOnly = true)
    public List<PendienteAdministrativo> proximosAVencer(int dias) {
        LocalDate limite = LocalDate.now().plusDays(dias);
        return repo.findByEstadoAndFechaEstimadaResolucionLessThanEqualOrderByFechaEstimadaResolucionAsc(
                EstadoPendiente.PENDIENTE, limite);
    }

    @Transactional(readOnly = true)
    public PendienteAdministrativo obtener(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pendiente administrativo " + id + " no encontrado"));
    }

    @Transactional
    public PendienteAdministrativo crear(PendienteAdministrativoCrearRequest req) {
        PendienteAdministrativo p = new PendienteAdministrativo();
        p.setTitulo(req.titulo());
        p.setDescripcion(req.descripcion());
        p.setFechaEstimadaResolucion(req.fechaEstimadaResolucion());
        p.setPrioridad(req.prioridad());
        p.setResponsable(resolverUsuario(req.responsableId()));
        p.setCategoria(req.categoria());
        p.setProyecto(resolverProyecto(req.proyectoId()));
        p.setCliente(resolverCliente(req.clienteId()));
        p.setProveedor(resolverProveedor(req.proveedorId()));
        p.setObservaciones(req.observaciones());
        p.setActivo(true);
        PendienteAdministrativo guardado = repo.save(p);
        auditoria.registrar(AccionAuditoria.CREAR, "PendienteAdministrativo", guardado.getId(), null, mapper.aResponse(guardado));
        return guardado;
    }

    @Transactional
    public PendienteAdministrativo editar(Long id, PendienteAdministrativoEditarRequest req) {
        PendienteAdministrativo p = obtener(id);
        var antes = mapper.aResponse(p);
        p.setTitulo(req.titulo());
        p.setDescripcion(req.descripcion());
        p.setFechaEstimadaResolucion(req.fechaEstimadaResolucion());
        p.setPrioridad(req.prioridad());
        p.setEstado(req.estado());
        p.setResponsable(resolverUsuario(req.responsableId()));
        p.setCategoria(req.categoria());
        p.setProyecto(resolverProyecto(req.proyectoId()));
        p.setCliente(resolverCliente(req.clienteId()));
        p.setProveedor(resolverProveedor(req.proveedorId()));
        p.setObservaciones(req.observaciones());
        auditoria.registrar(AccionAuditoria.EDITAR, "PendienteAdministrativo", id, antes, mapper.aResponse(p));
        return p;
    }

    @Transactional
    public PendienteAdministrativo activar(Long id) {
        PendienteAdministrativo p = obtener(id);
        var antes = mapper.aResponse(p);
        p.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "PendienteAdministrativo", id, antes, mapper.aResponse(p));
        return p;
    }

    @Transactional
    public PendienteAdministrativo desactivar(Long id) {
        PendienteAdministrativo p = obtener(id);
        var antes = mapper.aResponse(p);
        p.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "PendienteAdministrativo", id, antes, mapper.aResponse(p));
        return p;
    }

    @Transactional
    public void eliminar(Long id) {
        PendienteAdministrativo p = obtener(id);
        var antes = mapper.aResponse(p);
        repo.delete(p);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "PendienteAdministrativo", id, antes, null);
    }

    private Usuario resolverUsuario(Long id) {
        if (id == null) {
            return null;
        }
        return usuarioRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario " + id + " no encontrado"));
    }

    private Proyecto resolverProyecto(Long id) {
        if (id == null) {
            return null;
        }
        return proyectoRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto " + id + " no encontrado"));
    }

    private Cliente resolverCliente(Long id) {
        if (id == null) {
            return null;
        }
        return clienteRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cliente " + id + " no encontrado"));
    }

    private Proveedor resolverProveedor(Long id) {
        if (id == null) {
            return null;
        }
        return proveedorRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proveedor " + id + " no encontrado"));
    }
}

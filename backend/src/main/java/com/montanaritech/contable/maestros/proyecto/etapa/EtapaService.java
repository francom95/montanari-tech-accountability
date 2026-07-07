package com.montanaritech.contable.maestros.proyecto.etapa;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.Auditado;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.etapa.dto.EtapaCrearRequest;
import com.montanaritech.contable.maestros.proyecto.etapa.dto.EtapaEditarRequest;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EtapaService {

    private final EtapaRepository repo;
    private final EtapaMapper mapper;
    private final AuditoriaService auditoria;
    private final ProyectoRepository proyectoRepo;
    private final ProveedorRepository proveedorRepo;

    @Transactional(readOnly = true)
    public Page<Etapa> listar(Long proyectoId, String texto, Boolean activo, Pageable p) {
        resolverProyecto(proyectoId);
        return repo.buscar(proyectoId, texto, activo, p);
    }

    @Transactional(readOnly = true)
    public Etapa obtener(Long proyectoId, Long id) {
        return repo.findByIdAndProyectoId(id, proyectoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Etapa " + id + " no encontrada"));
    }

    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "Etapa")
    @Transactional
    public Etapa crear(Long proyectoId, EtapaCrearRequest req) {
        Proyecto proyecto = resolverProyecto(proyectoId);

        Etapa e = new Etapa();
        e.setProyecto(proyecto);
        e.setNombre(req.nombre());
        e.setDescripcion(req.descripcion());
        e.setEstado(req.estado() != null ? Etapa.EstadoEtapa.valueOf(req.estado()) : Etapa.EstadoEtapa.PENDIENTE);
        e.setFechaInicio(req.fechaInicio());
        e.setFechaEstimadaFin(req.fechaEstimadaFin());
        e.setPorcentajeAvance(req.porcentajeAvance());
        e.setMontoPresupuestado(req.montoPresupuestado());
        e.setCostosEstimados(req.costosEstimados());
        e.setProveedores(resolverProveedores(req.proveedoresIds()));
        e.setPagosPrevistos(req.pagosPrevistos());
        e.setCobrosPrevistos(req.cobrosPrevistos());
        e.setObservaciones(req.observaciones());
        e.setActivo(true);
        return repo.save(e);
    }

    @Transactional
    public Etapa editar(Long proyectoId, Long id, EtapaEditarRequest req) {
        Etapa e = obtener(proyectoId, id);
        var antes = mapper.aResponse(e);

        e.setNombre(req.nombre());
        e.setDescripcion(req.descripcion());
        e.setEstado(req.estado() != null ? Etapa.EstadoEtapa.valueOf(req.estado()) : e.getEstado());
        e.setFechaInicio(req.fechaInicio());
        e.setFechaEstimadaFin(req.fechaEstimadaFin());
        e.setPorcentajeAvance(req.porcentajeAvance());
        e.setMontoPresupuestado(req.montoPresupuestado());
        e.setCostosEstimados(req.costosEstimados());
        e.setProveedores(resolverProveedores(req.proveedoresIds()));
        e.setPagosPrevistos(req.pagosPrevistos());
        e.setCobrosPrevistos(req.cobrosPrevistos());
        e.setObservaciones(req.observaciones());

        auditoria.registrar(AccionAuditoria.EDITAR, "Etapa", id, antes, mapper.aResponse(e));
        return e;
    }

    @Transactional
    public Etapa desactivar(Long proyectoId, Long id) {
        Etapa e = obtener(proyectoId, id);
        var antes = mapper.aResponse(e);
        e.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Etapa", id, antes, mapper.aResponse(e));
        return e;
    }

    @Transactional
    public Etapa activar(Long proyectoId, Long id) {
        Etapa e = obtener(proyectoId, id);
        var antes = mapper.aResponse(e);
        e.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Etapa", id, antes, mapper.aResponse(e));
        return e;
    }

    @Transactional
    public void eliminar(Long proyectoId, Long id) {
        Etapa e = obtener(proyectoId, id);
        var antes = mapper.aResponse(e);
        repo.delete(e);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "Etapa", id, antes, null);
    }

    private Proyecto resolverProyecto(Long id) {
        return proyectoRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto " + id + " no encontrado"));
    }

    private Set<com.montanaritech.contable.maestros.proveedor.Proveedor> resolverProveedores(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(proveedorRepo.findAllById(ids));
    }
}

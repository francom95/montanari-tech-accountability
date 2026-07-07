package com.montanaritech.contable.maestros.proyecto.comision;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.Auditado;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.comisionista.Comisionista;
import com.montanaritech.contable.maestros.comisionista.ComisionistaRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.comision.dto.ComisionProyectoCrearRequest;
import com.montanaritech.contable.maestros.proyecto.comision.dto.ComisionProyectoEditarRequest;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ComisionProyectoService {

    private final ComisionProyectoRepository repo;
    private final ComisionProyectoMapper mapper;
    private final AuditoriaService auditoria;
    private final ProyectoRepository proyectoRepo;
    private final ComisionistaRepository comisionistaRepo;
    private final MonedaRepository monedaRepo;
    private final ComisionCalculoService calculoService;

    @Transactional(readOnly = true)
    public Page<ComisionProyecto> listar(Long proyectoId, Boolean activo, Pageable p) {
        resolverProyecto(proyectoId);
        return repo.buscarPorProyecto(proyectoId, activo, p);
    }

    @Transactional(readOnly = true)
    public ComisionProyecto obtener(Long proyectoId, Long id) {
        return repo.findByIdAndProyectoId(id, proyectoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comisión de proyecto " + id + " no encontrada"));
    }

    @Transactional(readOnly = true)
    public Page<ComisionProyecto> consultar(Long proyectoId, Long comisionistaId, String estadoPago, LocalDate desde, LocalDate hasta, Pageable p) {
        var estado = estadoPago != null ? ComisionProyecto.EstadoPago.valueOf(estadoPago) : null;
        return repo.consultar(proyectoId, comisionistaId, estado, desde, hasta, p);
    }

    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "ComisionProyecto")
    @Transactional
    public ComisionProyecto crear(Long proyectoId, ComisionProyectoCrearRequest req) {
        Proyecto proyecto = resolverProyecto(proyectoId);
        Comisionista comisionista = resolverComisionista(req.comisionistaId());
        Moneda moneda = resolverMoneda(req.monedaId());
        var base = ComisionProyecto.BaseCalculo.valueOf(req.baseCalculo());

        ComisionProyecto c = new ComisionProyecto();
        c.setProyecto(proyecto);
        c.setComisionista(comisionista);
        c.setPorcentajeComision(req.porcentajeComision());
        c.setBaseCalculo(base);
        c.setMoneda(moneda);
        c.setImporteEstimado(calculoService.calcularEstimado(proyecto, base, req.porcentajeComision()));
        c.setFechaEstimadaPago(req.fechaEstimadaPago());
        c.setObservaciones(req.observaciones());
        c.setActivo(true);
        return repo.save(c);
    }

    @Transactional
    public ComisionProyecto editar(Long proyectoId, Long id, ComisionProyectoEditarRequest req) {
        ComisionProyecto c = obtener(proyectoId, id);
        var antes = mapper.aResponse(c);

        Comisionista comisionista = resolverComisionista(req.comisionistaId());
        Moneda moneda = resolverMoneda(req.monedaId());
        var base = ComisionProyecto.BaseCalculo.valueOf(req.baseCalculo());

        c.setComisionista(comisionista);
        c.setPorcentajeComision(req.porcentajeComision());
        c.setBaseCalculo(base);
        c.setMoneda(moneda);
        c.setImporteEstimado(calculoService.calcularEstimado(c.getProyecto(), base, req.porcentajeComision()));
        c.setImporteFinal(req.importeFinal());
        c.setEstadoPago(req.estadoPago() != null ? ComisionProyecto.EstadoPago.valueOf(req.estadoPago()) : c.getEstadoPago());
        c.setFechaEstimadaPago(req.fechaEstimadaPago());
        c.setObservaciones(req.observaciones());

        auditoria.registrar(AccionAuditoria.EDITAR, "ComisionProyecto", id, antes, mapper.aResponse(c));
        return c;
    }

    @Transactional
    public ComisionProyecto desactivar(Long proyectoId, Long id) {
        ComisionProyecto c = obtener(proyectoId, id);
        var antes = mapper.aResponse(c);
        c.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "ComisionProyecto", id, antes, mapper.aResponse(c));
        return c;
    }

    @Transactional
    public ComisionProyecto activar(Long proyectoId, Long id) {
        ComisionProyecto c = obtener(proyectoId, id);
        var antes = mapper.aResponse(c);
        c.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "ComisionProyecto", id, antes, mapper.aResponse(c));
        return c;
    }

    @Transactional
    public void eliminar(Long proyectoId, Long id) {
        ComisionProyecto c = obtener(proyectoId, id);
        var antes = mapper.aResponse(c);
        repo.delete(c);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "ComisionProyecto", id, antes, null);
    }

    /** Invocado por {@code ProyectoService.editar} tras guardar cambios del proyecto. */
    @Transactional
    public void recalcularEstimadosDeProyecto(Proyecto proyecto) {
        for (ComisionProyecto c : repo.findByProyectoIdAndActivoTrue(proyecto.getId())) {
            c.setImporteEstimado(calculoService.calcularEstimado(proyecto, c.getBaseCalculo(), c.getPorcentajeComision()));
        }
    }

    private Proyecto resolverProyecto(Long id) {
        return proyectoRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto " + id + " no encontrado"));
    }

    private Comisionista resolverComisionista(Long id) {
        return comisionistaRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Comisionista " + id + " no encontrado"));
    }

    private Moneda resolverMoneda(Long id) {
        return monedaRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + id + " no encontrada"));
    }
}

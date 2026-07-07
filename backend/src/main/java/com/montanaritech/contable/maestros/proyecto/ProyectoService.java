package com.montanaritech.contable.maestros.proyecto;

import com.montanaritech.contable.auth.Usuario;
import com.montanaritech.contable.auth.UsuarioRepository;
import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.Auditado;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.ConflictoException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.cliente.ClienteRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proyecto.comision.ComisionProyectoService;
import com.montanaritech.contable.maestros.proyecto.dto.CuotaRequest;
import com.montanaritech.contable.maestros.proyecto.dto.ProyectoCrearRequest;
import com.montanaritech.contable.maestros.proyecto.dto.ProyectoEditarRequest;
import com.montanaritech.contable.maestros.proyecto.etapa.EtapaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProyectoService {

    private final ProyectoRepository repo;
    private final ProyectoMapper mapper;
    private final AuditoriaService auditoria;
    private final ClienteRepository clienteRepo;
    private final UsuarioRepository usuarioRepo;
    private final MonedaRepository monedaRepo;
    private final EtapaRepository etapaRepo;
    private final ComisionProyectoService comisionProyectoService;

    @Transactional(readOnly = true)
    public Page<Proyecto> listar(String texto, Boolean activo, Long clienteId, Pageable p) {
        return repo.buscar(texto, activo, clienteId, p);
    }

    @Transactional(readOnly = true)
    public Proyecto obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto " + id + " no encontrado"));
    }

    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "Proyecto")
    @Transactional
    public Proyecto crear(ProyectoCrearRequest req) {
        Proyecto p = new Proyecto();
        p.setNombre(req.nombre());
        p.setCliente(resolverCliente(req.clienteId()));
        p.setResponsable(resolverResponsable(req.responsableId()));
        p.setPais(req.pais());
        p.setTipoProyecto(req.tipoProyecto());
        p.setEstado(req.estado() != null ? Proyecto.EstadoProyecto.valueOf(req.estado()) : Proyecto.EstadoProyecto.PROSPECTO);
        p.setMoneda(resolverMoneda(req.monedaId()));
        p.setMontoTotal(req.montoTotal());
        p.setCantidadPagosPactados(req.cantidadPagosPactados());
        p.setComentarios(req.comentarios());
        p.setEstadoComercial(req.estadoComercial() != null ? Proyecto.EstadoComercial.valueOf(req.estadoComercial()) : Proyecto.EstadoComercial.PROSPECTO);
        p.setEstadoFacturacion(req.estadoFacturacion() != null ? Proyecto.EstadoFacturacion.valueOf(req.estadoFacturacion()) : Proyecto.EstadoFacturacion.NO_FACTURADO);
        p.setEstadoCobranza(req.estadoCobranza() != null ? Proyecto.EstadoCobranza.valueOf(req.estadoCobranza()) : Proyecto.EstadoCobranza.PENDIENTE);
        p.setFechaEstimadaFinalizacion(req.fechaEstimadaFinalizacion());
        p.setFechaRealFinalizacion(req.fechaRealFinalizacion());
        p.setActivo(true);
        reemplazarCuotas(p, req.cuotas());
        return repo.save(p);
    }

    @Transactional
    public Proyecto editar(Long id, ProyectoEditarRequest req) {
        Proyecto p = obtener(id);
        var antes = mapper.aResponse(p);

        p.setNombre(req.nombre());
        p.setCliente(resolverCliente(req.clienteId()));
        p.setResponsable(resolverResponsable(req.responsableId()));
        p.setPais(req.pais());
        p.setTipoProyecto(req.tipoProyecto());
        p.setEstado(req.estado() != null ? Proyecto.EstadoProyecto.valueOf(req.estado()) : p.getEstado());
        p.setMoneda(resolverMoneda(req.monedaId()));
        p.setMontoTotal(req.montoTotal());
        p.setCantidadPagosPactados(req.cantidadPagosPactados());
        p.setComentarios(req.comentarios());
        p.setEstadoComercial(req.estadoComercial() != null ? Proyecto.EstadoComercial.valueOf(req.estadoComercial()) : p.getEstadoComercial());
        p.setEstadoFacturacion(req.estadoFacturacion() != null ? Proyecto.EstadoFacturacion.valueOf(req.estadoFacturacion()) : p.getEstadoFacturacion());
        p.setEstadoCobranza(req.estadoCobranza() != null ? Proyecto.EstadoCobranza.valueOf(req.estadoCobranza()) : p.getEstadoCobranza());
        p.setFechaEstimadaFinalizacion(req.fechaEstimadaFinalizacion());
        p.setFechaRealFinalizacion(req.fechaRealFinalizacion());
        reemplazarCuotas(p, req.cuotas());
        comisionProyectoService.recalcularEstimadosDeProyecto(p);

        auditoria.registrar(AccionAuditoria.EDITAR, "Proyecto", id, antes, mapper.aResponse(p));
        return p;
    }

    @Transactional
    public Proyecto desactivar(Long id) {
        Proyecto p = obtener(id);
        var antes = mapper.aResponse(p);
        p.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Proyecto", id, antes, mapper.aResponse(p));
        return p;
    }

    @Transactional
    public Proyecto activar(Long id) {
        Proyecto p = obtener(id);
        var antes = mapper.aResponse(p);
        p.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Proyecto", id, antes, mapper.aResponse(p));
        return p;
    }

    @Transactional
    public void eliminar(Long id) {
        Proyecto p = obtener(id);
        if (etapaRepo.existsByProyectoId(id)) {
            throw new ConflictoException("ETAPAS_ASOCIADAS", "No se puede eliminar el proyecto: tiene etapas asociadas");
        }
        var antes = mapper.aResponse(p);
        repo.delete(p);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "Proyecto", id, antes, null);
    }

    private void reemplazarCuotas(Proyecto proyecto, List<CuotaRequest> nuevas) {
        proyecto.getCuotas().clear();
        if (nuevas == null) {
            return;
        }
        int numero = 1;
        for (CuotaRequest cr : nuevas) {
            ProyectoCuota c = new ProyectoCuota();
            c.setProyecto(proyecto);
            c.setNumero(numero++);
            c.setFechaEstimadaCobro(cr.fechaEstimadaCobro());
            c.setImporte(cr.importe());
            proyecto.getCuotas().add(c);
        }
    }

    private Cliente resolverCliente(Long id) {
        return clienteRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cliente " + id + " no encontrado"));
    }

    private Usuario resolverResponsable(Long id) {
        if (id == null) {
            return null;
        }
        return usuarioRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario " + id + " no encontrado"));
    }

    private Moneda resolverMoneda(Long id) {
        return monedaRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + id + " no encontrada"));
    }
}

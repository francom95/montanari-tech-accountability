package com.montanaritech.contable.compromiso;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.Auditado;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.ConflictoException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.compromiso.dto.CompromisoCrearRequest;
import com.montanaritech.contable.compromiso.dto.CompromisoEditarRequest;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.vencimientos.OrigenGeneracionVencimiento;
import com.montanaritech.contable.vencimientos.TipoRecurrencia;
import com.montanaritech.contable.vencimientos.TipoVencimiento;
import com.montanaritech.contable.vencimientos.Vencimiento;
import com.montanaritech.contable.vencimientos.VencimientoService;
import com.montanaritech.contable.vencimientos.dto.VencimientoCrearRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD de {@link Compromiso} (F8.2), molde PL-1 sobre F1.8. */
@Service
@RequiredArgsConstructor
public class CompromisoService {

    private static final Map<TipoCompromiso, TipoVencimiento> MAPEO_TIPO_VENCIMIENTO = Map.ofEntries(
            Map.entry(TipoCompromiso.CUOTA_PLAN_DE_PAGOS, TipoVencimiento.PLAN_DE_PAGO),
            Map.entry(TipoCompromiso.VENCIMIENTO_IMPOSITIVO, TipoVencimiento.OTRO),
            Map.entry(TipoCompromiso.IVA_DIFERIDO, TipoVencimiento.IVA),
            Map.entry(TipoCompromiso.IIBB, TipoVencimiento.IIBB),
            Map.entry(TipoCompromiso.PAGO_A_PROVEEDOR, TipoVencimiento.OTRO),
            Map.entry(TipoCompromiso.SUELDOS, TipoVencimiento.SUELDOS),
            Map.entry(TipoCompromiso.CARGAS_SOCIALES, TipoVencimiento.CARGAS_SOCIALES),
            Map.entry(TipoCompromiso.CONTADOR, TipoVencimiento.CONTADOR),
            Map.entry(TipoCompromiso.COMISION_BANCARIA, TipoVencimiento.OTRO),
            Map.entry(TipoCompromiso.COMISION_POR_VENTA, TipoVencimiento.OTRO),
            Map.entry(TipoCompromiso.SUSCRIPCION, TipoVencimiento.SUSCRIPCION),
            Map.entry(TipoCompromiso.TARJETA, TipoVencimiento.TARJETA),
            Map.entry(TipoCompromiso.OTRO_EGRESO, TipoVencimiento.OTRO));

    private final CompromisoRepository repo;
    private final MonedaRepository monedaRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProyectoRepository proyectoRepository;
    private final CompromisoMapper mapper;
    private final AuditoriaService auditoria;
    private final VencimientoService vencimientoService;

    @Transactional(readOnly = true)
    public Page<Compromiso> listar(String texto, EstadoCompromiso estado, Boolean activo, Pageable p) {
        return repo.buscar(texto, estado, activo, p);
    }

    /** F8.2: query service simple para que F8.3 proyecte los compromisos de un período. */
    @Transactional(readOnly = true)
    public List<Compromiso> porRangoDeFechas(LocalDate desde, LocalDate hasta) {
        return repo.findByFechaPrevistaBetweenOrderByFechaPrevistaAsc(desde, hasta);
    }

    @Transactional(readOnly = true)
    public Compromiso obtener(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compromiso " + id + " no encontrado"));
    }

    @Auditado(accion = AccionAuditoria.CREAR, entidadTipo = "Compromiso")
    @Transactional
    public Compromiso crear(CompromisoCrearRequest req) {
        Compromiso c = new Compromiso();
        c.setConcepto(req.concepto());
        c.setTipo(req.tipo());
        c.setFechaPrevista(req.fechaPrevista());
        c.setImporte(req.importe());
        c.setMoneda(resolverMoneda(req.monedaId()));
        c.setProveedor(resolverProveedor(req.proveedorId()));
        c.setProyecto(resolverProyecto(req.proyectoId()));
        c.setActivo(true);
        Compromiso guardado = repo.save(c);

        if (req.generarVencimiento()) {
            Vencimiento v = vencimientoService.crearDesdeOrigen(
                    new VencimientoCrearRequest(req.concepto(), MAPEO_TIPO_VENCIMIENTO.get(req.tipo()),
                            req.fechaPrevista(), req.importe(), req.monedaId(), TipoRecurrencia.UNICA, null, null,
                            req.proveedorId(), null, null, null, req.proyectoId(), null, req.observaciones()),
                    OrigenGeneracionVencimiento.COMPROMISO, guardado.getId());
            guardado.setVencimientoGeneradoId(v.getId());
        }
        return guardado;
    }

    @Transactional
    public Compromiso editar(Long id, CompromisoEditarRequest req) {
        Compromiso c = obtener(id);
        var antes = mapper.aResponse(c);
        c.setConcepto(req.concepto());
        c.setTipo(req.tipo());
        c.setFechaPrevista(req.fechaPrevista());
        c.setImporte(req.importe());
        c.setMoneda(resolverMoneda(req.monedaId()));
        c.setProveedor(resolverProveedor(req.proveedorId()));
        c.setProyecto(resolverProyecto(req.proyectoId()));
        c.setEstado(req.estado());
        c.setObservaciones(req.observaciones());
        auditoria.registrar(AccionAuditoria.EDITAR, "Compromiso", id, antes, mapper.aResponse(c));
        return c;
    }

    private Moneda resolverMoneda(Long id) {
        return monedaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + id + " no encontrada"));
    }

    private Proveedor resolverProveedor(Long id) {
        if (id == null) {
            return null;
        }
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor " + id + " no encontrado"));
    }

    private Proyecto resolverProyecto(Long id) {
        if (id == null) {
            return null;
        }
        return proyectoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto " + id + " no encontrado"));
    }

    @Transactional
    public Compromiso activar(Long id) {
        Compromiso c = obtener(id);
        var antes = mapper.aResponse(c);
        c.setActivo(true);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Compromiso", id, antes, mapper.aResponse(c));
        return c;
    }

    @Transactional
    public Compromiso desactivar(Long id) {
        Compromiso c = obtener(id);
        var antes = mapper.aResponse(c);
        c.setActivo(false);
        auditoria.registrar(AccionAuditoria.CAMBIO_ESTADO, "Compromiso", id, antes, mapper.aResponse(c));
        return c;
    }

    @Transactional
    public void eliminar(Long id) {
        Compromiso c = obtener(id);
        if (c.getVencimientoGeneradoId() != null) {
            throw new ConflictoException("VENCIMIENTO_GENERADO",
                    "No se puede eliminar: ya generó un vencimiento (F8.1). Cancelalo desde el calendario en su lugar.");
        }
        var antes = mapper.aResponse(c);
        repo.delete(c);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "Compromiso", id, antes, null);
    }
}

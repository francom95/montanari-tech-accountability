package com.montanaritech.contable.facturacion.facturacompra;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.estado.TransicionEstadoValidator;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.contabilidad.asiento.AsientoLinea;
import com.montanaritech.contable.contabilidad.asiento.AsientoService;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTipo;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTributo;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTributoRepository;
import com.montanaritech.contable.facturacion.comprobantetributo.TipoTributo;
import com.montanaritech.contable.facturacion.facturacompra.dto.FacturaCompraCrearRequest;
import com.montanaritech.contable.facturacion.facturacompra.dto.FacturaCompraEditarRequest;
import com.montanaritech.contable.facturacion.facturacompra.dto.FacturaCompraLineaRequest;
import com.montanaritech.contable.facturacion.facturacompra.dto.FacturaCompraTributoRequest;
import com.montanaritech.contable.maestros.jurisdiccion.Jurisdiccion;
import com.montanaritech.contable.maestros.jurisdiccion.JurisdiccionRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.tipocosto.TipoCosto;
import com.montanaritech.contable.maestros.tipocosto.TipoCostoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FacturaCompraService {

    private static final Set<BigDecimal> ALICUOTAS_IVA_VALIDAS = Set.of(
            new BigDecimal("0"), new BigDecimal("2.5"), new BigDecimal("5"),
            new BigDecimal("10.5"), new BigDecimal("21"), new BigDecimal("27"));
    private static final Set<TipoTributo> TRIBUTOS_APLICABLES_A_COMPRA = Set.of(TipoTributo.PERCEPCION_IVA, TipoTributo.PERCEPCION_IIBB);
    private static final String MONEDA_LIBRO = "ARS";

    private final FacturaCompraRepository repo;
    private final FacturaCompraMapper mapper;
    private final AuditoriaService auditoria;
    private final AsientoService asientoService;
    private final FacturaCompraAsientoGenerator generator;
    private final ProveedorRepository proveedorRepo;
    private final ProyectoRepository proyectoRepo;
    private final JurisdiccionRepository jurisdiccionRepo;
    private final MonedaRepository monedaRepo;
    private final CuentaContableRepository cuentaContableRepo;
    private final TipoCostoRepository tipoCostoRepo;
    private final ComprobanteTributoRepository comprobanteTributoRepo;

    @Transactional(readOnly = true)
    public Page<FacturaCompra> listar(String texto, EstadoDocumento estado, Long proveedorId, Long proyectoId,
            LocalDate fechaDesde, LocalDate fechaHasta, Pageable p) {
        return repo.buscar(texto, estado, proveedorId, proyectoId, fechaDesde, fechaHasta, p);
    }

    @Transactional(readOnly = true)
    public FacturaCompra obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Factura de compra " + id + " no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<ComprobanteTributo> tributosDe(Long facturaId) {
        return comprobanteTributoRepo.findByComprobanteTipoAndComprobanteIdOrderByIdAsc(ComprobanteTipo.FACTURA_COMPRA, facturaId);
    }

    @Transactional
    public FacturaCompra crearBorrador(FacturaCompraCrearRequest req) {
        FacturaCompra f = new FacturaCompra();
        f.setProveedor(resolverProveedor(req.proveedorId()));
        f.setProyecto(req.proyectoId() != null ? resolverProyecto(req.proyectoId()) : null);
        f.setFecha(req.fecha());
        f.setFechaVencimiento(req.fechaVencimiento());
        f.setTipoComprobante(req.tipoComprobante());
        f.setPuntoVenta(req.puntoVenta());
        f.setNumero(req.numero());
        f.setMoneda(resolverMoneda(req.monedaId()));
        f.setTipoCambio(req.tipoCambio());
        f.setObservaciones(req.observaciones());
        f.setEstado(EstadoDocumento.BORRADOR);
        reemplazarLineas(f, req.lineas());
        recalcularTotales(f, req.tributos());

        FacturaCompra guardada = repo.save(f);
        reemplazarTributos(guardada.getId(), req.tributos());
        auditoria.registrar(AccionAuditoria.CREAR, "FacturaCompra", guardada.getId(), null, mapper.aResponse(guardada, tributosDe(guardada.getId())));
        return guardada;
    }

    @Transactional
    public FacturaCompra editarBorrador(Long id, FacturaCompraEditarRequest req) {
        FacturaCompra f = obtenerBorrador(id);
        var antes = mapper.aResponse(f, tributosDe(id));

        f.setProveedor(resolverProveedor(req.proveedorId()));
        f.setProyecto(req.proyectoId() != null ? resolverProyecto(req.proyectoId()) : null);
        f.setFecha(req.fecha());
        f.setFechaVencimiento(req.fechaVencimiento());
        f.setTipoComprobante(req.tipoComprobante());
        f.setPuntoVenta(req.puntoVenta());
        f.setNumero(req.numero());
        f.setMoneda(resolverMoneda(req.monedaId()));
        f.setTipoCambio(req.tipoCambio());
        f.setObservaciones(req.observaciones());
        reemplazarLineas(f, req.lineas());
        recalcularTotales(f, req.tributos());
        reemplazarTributos(id, req.tributos());

        auditoria.registrar(AccionAuditoria.EDITAR, "FacturaCompra", id, antes, mapper.aResponse(f, tributosDe(id)));
        return f;
    }

    @Transactional
    public void eliminarBorrador(Long id) {
        FacturaCompra f = obtenerBorrador(id);
        var antes = mapper.aResponse(f, tributosDe(id));
        comprobanteTributoRepo.deleteByComprobanteTipoAndComprobanteId(ComprobanteTipo.FACTURA_COMPRA, id);
        repo.delete(f);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "FacturaCompra", id, antes, null);
    }

    @Transactional
    public FacturaCompra confirmar(Long id) {
        FacturaCompra f = obtener(id);
        var antes = mapper.aResponse(f, tributosDe(id));
        TransicionEstadoValidator.validar(f.getEstado(), EstadoDocumento.CONFIRMADO);

        var asientoGenerado = generator.generar(f);
        var asiento = asientoService.registrarAutomatico(asientoGenerado);

        f.setAsiento(asiento);
        f.setEstado(EstadoDocumento.CONFIRMADO);

        auditoria.registrar(AccionAuditoria.CONFIRMAR, "FacturaCompra", id, antes, mapper.aResponse(f, tributosDe(id)));
        return f;
    }

    @Transactional
    public FacturaCompra anular(Long id, String motivo) {
        FacturaCompra f = obtener(id);
        var antes = mapper.aResponse(f, tributosDe(id));
        TransicionEstadoValidator.validar(f.getEstado(), EstadoDocumento.ANULADO);

        if (f.getAsiento() != null) {
            asientoService.anularPorDocumento(f.getAsiento().getId(), motivo);
        }
        f.setEstado(EstadoDocumento.ANULADO);

        auditoria.registrar(AccionAuditoria.ANULAR, "FacturaCompra", id, antes, mapper.aResponse(f, tributosDe(id)));
        return f;
    }

    private FacturaCompra obtenerBorrador(Long id) {
        FacturaCompra f = obtener(id);
        if (f.getEstado() != EstadoDocumento.BORRADOR) {
            throw new NegocioException("TRANSICION_ESTADO_INVALIDA", "Solo se pueden editar o eliminar facturas en borrador");
        }
        return f;
    }

    private void reemplazarLineas(FacturaCompra f, List<FacturaCompraLineaRequest> nuevas) {
        f.getLineas().clear();
        int orden = 1;
        for (FacturaCompraLineaRequest r : nuevas) {
            if (!ALICUOTAS_IVA_VALIDAS.contains(r.alicuotaIva())) {
                throw new NegocioException("ALICUOTA_IVA_INVALIDA",
                        "%s no es una alícuota de IVA válida (0, 2.5, 5, 10.5, 21, 27)".formatted(r.alicuotaIva()));
            }
            FacturaCompraLinea l = new FacturaCompraLinea();
            l.setFacturaCompra(f);
            l.setOrden(orden++);
            l.setDescripcion(r.descripcion());
            l.setTipoCosto(resolverTipoCosto(r.tipoCostoId()));
            l.setImporteNeto(r.importeNeto());
            l.setAlicuotaIva(r.alicuotaIva());
            l.setImporteIva(r.importeNeto().multiply(r.alicuotaIva()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            l.setCuentaContable(r.cuentaContableId() != null ? resolverCuentaContable(r.cuentaContableId()) : null);
            f.getLineas().add(l);
        }
    }

    private void reemplazarTributos(Long facturaId, List<FacturaCompraTributoRequest> tributos) {
        comprobanteTributoRepo.deleteByComprobanteTipoAndComprobanteId(ComprobanteTipo.FACTURA_COMPRA, facturaId);
        if (tributos == null) {
            return;
        }
        for (FacturaCompraTributoRequest r : tributos) {
            if (!TRIBUTOS_APLICABLES_A_COMPRA.contains(r.tipo())) {
                throw new NegocioException("TRIBUTO_NO_APLICABLE_A_COMPRA",
                        "%s no es un tributo aplicable a una factura de compra (Montanari no retiene, solo percepciones sufridas)".formatted(r.tipo()));
            }
            ComprobanteTributo t = new ComprobanteTributo();
            t.setComprobanteTipo(ComprobanteTipo.FACTURA_COMPRA);
            t.setComprobanteId(facturaId);
            t.setTipo(r.tipo());
            t.setJurisdiccion(r.jurisdiccionId() != null ? resolverJurisdiccion(r.jurisdiccionId()) : null);
            t.setBase(r.base());
            t.setAlicuota(r.alicuota());
            t.setImporte(r.importe());
            comprobanteTributoRepo.save(t);
        }
    }

    private void recalcularTotales(FacturaCompra f, List<FacturaCompraTributoRequest> tributos) {
        BigDecimal neto = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;
        for (FacturaCompraLinea l : f.getLineas()) {
            neto = neto.add(l.getImporteNeto());
            iva = iva.add(l.getImporteIva());
        }
        BigDecimal percepciones = BigDecimal.ZERO;
        if (tributos != null) {
            for (FacturaCompraTributoRequest t : tributos) {
                if (TRIBUTOS_APLICABLES_A_COMPRA.contains(t.tipo())) {
                    percepciones = percepciones.add(t.importe());
                }
            }
        }
        f.setNeto(neto);
        f.setImporteIva(iva);
        f.setImportePercepciones(percepciones);
        BigDecimal total = neto.add(iva).add(percepciones);
        f.setTotal(total);

        if (MONEDA_LIBRO.equals(f.getMoneda().getCodigo())) {
            f.setTipoCambio(new BigDecimal("1.000000"));
            f.setFuenteTc(null);
            f.setTotalArs(total);
        } else {
            f.setFuenteTc(AsientoLinea.FuenteTc.MANUAL);
            f.setTotalArs(total.multiply(f.getTipoCambio()).setScale(2, RoundingMode.HALF_UP));
        }
    }

    private Proveedor resolverProveedor(Long id) {
        return proveedorRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proveedor " + id + " no encontrado"));
    }

    private Proyecto resolverProyecto(Long id) {
        return proyectoRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proyecto " + id + " no encontrado"));
    }

    private Jurisdiccion resolverJurisdiccion(Long id) {
        return jurisdiccionRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Jurisdicción " + id + " no encontrada"));
    }

    private Moneda resolverMoneda(Long id) {
        return monedaRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + id + " no encontrada"));
    }

    private CuentaContable resolverCuentaContable(Long id) {
        return cuentaContableRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cuenta contable " + id + " no encontrada"));
    }

    private TipoCosto resolverTipoCosto(Long id) {
        return tipoCostoRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Tipo de costo " + id + " no encontrado"));
    }
}

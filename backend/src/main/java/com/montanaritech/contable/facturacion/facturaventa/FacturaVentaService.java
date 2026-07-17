package com.montanaritech.contable.facturacion.facturaventa;

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
import com.montanaritech.contable.facturacion.facturaventa.dto.FacturaVentaCrearRequest;
import com.montanaritech.contable.facturacion.facturaventa.dto.FacturaVentaEditarRequest;
import com.montanaritech.contable.facturacion.facturaventa.dto.FacturaVentaLineaRequest;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.cliente.ClienteRepository;
import com.montanaritech.contable.maestros.jurisdiccion.Jurisdiccion;
import com.montanaritech.contable.maestros.jurisdiccion.JurisdiccionRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
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

/**
 * Ciclo de vida de la factura de venta (F4.2, sobre F4.1). Borrador sin
 * validación contable (F3.1 §3.5, mismo criterio que {@code AsientoService});
 * al confirmar corre {@link FacturaVentaAsientoGenerator} y entrega el
 * resultado a {@code AsientoService.registrarAutomatico} (ADR-07: nunca
 * inserta en {@code asiento} directamente).
 */
@Service
@RequiredArgsConstructor
public class FacturaVentaService {

    /** Catálogo AFIP de alícuotas de IVA vigentes (F1.1 §6.5). Fijo: no cambia hace décadas; un valor nuevo es un cambio de código, no de configuración de usuario. */
    private static final Set<BigDecimal> ALICUOTAS_IVA_VALIDAS = Set.of(
            new BigDecimal("0"), new BigDecimal("2.5"), new BigDecimal("5"),
            new BigDecimal("10.5"), new BigDecimal("21"), new BigDecimal("27"));
    private static final String MONEDA_LIBRO = "ARS";

    private final FacturaVentaRepository repo;
    private final FacturaVentaMapper mapper;
    private final AuditoriaService auditoria;
    private final AsientoService asientoService;
    private final FacturaVentaAsientoGenerator generator;
    private final ClienteRepository clienteRepo;
    private final ProyectoRepository proyectoRepo;
    private final JurisdiccionRepository jurisdiccionRepo;
    private final MonedaRepository monedaRepo;
    private final CuentaContableRepository cuentaContableRepo;

    @Transactional(readOnly = true)
    public Page<FacturaVenta> listar(String texto, EstadoDocumento estado, Long clienteId, Long proyectoId,
            LocalDate fechaDesde, LocalDate fechaHasta, Pageable p) {
        return repo.buscar(texto, estado, clienteId, proyectoId, fechaDesde, fechaHasta, p);
    }

    @Transactional(readOnly = true)
    public FacturaVenta obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Factura de venta " + id + " no encontrada"));
    }

    @Transactional
    public FacturaVenta crearBorrador(FacturaVentaCrearRequest req) {
        FacturaVenta f = new FacturaVenta();
        f.setCliente(resolverCliente(req.clienteId()));
        f.setProyecto(req.proyectoId() != null ? resolverProyecto(req.proyectoId()) : null);
        f.setFecha(req.fecha());
        f.setFechaVencimiento(req.fechaVencimiento());
        f.setTipoComprobante(req.tipoComprobante());
        f.setPuntoVenta(req.puntoVenta());
        f.setNumero(req.numero());
        f.setJurisdiccionDestino(req.jurisdiccionDestinoId() != null ? resolverJurisdiccion(req.jurisdiccionDestinoId()) : null);
        f.setMoneda(resolverMoneda(req.monedaId()));
        f.setTipoCambio(req.tipoCambio());
        f.setObservaciones(req.observaciones());
        f.setEstado(EstadoDocumento.BORRADOR);
        reemplazarLineas(f, req.lineas());
        recalcularTotales(f);

        FacturaVenta guardada = repo.save(f);
        auditoria.registrar(AccionAuditoria.CREAR, "FacturaVenta", guardada.getId(), null, mapper.aResponse(guardada));
        return guardada;
    }

    @Transactional
    public FacturaVenta editarBorrador(Long id, FacturaVentaEditarRequest req) {
        FacturaVenta f = obtenerBorrador(id);
        var antes = mapper.aResponse(f);

        f.setCliente(resolverCliente(req.clienteId()));
        f.setProyecto(req.proyectoId() != null ? resolverProyecto(req.proyectoId()) : null);
        f.setFecha(req.fecha());
        f.setFechaVencimiento(req.fechaVencimiento());
        f.setTipoComprobante(req.tipoComprobante());
        f.setPuntoVenta(req.puntoVenta());
        f.setNumero(req.numero());
        f.setJurisdiccionDestino(req.jurisdiccionDestinoId() != null ? resolverJurisdiccion(req.jurisdiccionDestinoId()) : null);
        f.setMoneda(resolverMoneda(req.monedaId()));
        f.setTipoCambio(req.tipoCambio());
        f.setObservaciones(req.observaciones());
        reemplazarLineas(f, req.lineas());
        recalcularTotales(f);

        auditoria.registrar(AccionAuditoria.EDITAR, "FacturaVenta", id, antes, mapper.aResponse(f));
        return f;
    }

    @Transactional
    public void eliminarBorrador(Long id) {
        FacturaVenta f = obtenerBorrador(id);
        var antes = mapper.aResponse(f);
        repo.delete(f);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "FacturaVenta", id, antes, null);
    }

    /**
     * Confirma la factura: arma el asiento vía {@link FacturaVentaAsientoGenerator}
     * y lo entrega a {@code AsientoService.registrarAutomatico}. Si el asiento
     * no balancea o falta un mapeo de cuenta, nada se persiste — ni la
     * factura pasa a CONFIRMADO ni el asiento se crea (misma transacción).
     */
    @Transactional
    public FacturaVenta confirmar(Long id) {
        FacturaVenta f = obtener(id);
        var antes = mapper.aResponse(f);
        TransicionEstadoValidator.validar(f.getEstado(), EstadoDocumento.CONFIRMADO);

        var asientoGenerado = generator.generar(f);
        var asiento = asientoService.registrarAutomatico(asientoGenerado);

        f.setAsiento(asiento);
        f.setEstado(EstadoDocumento.CONFIRMADO);

        auditoria.registrar(AccionAuditoria.CONFIRMAR, "FacturaVenta", id, antes, mapper.aResponse(f));
        return f;
    }

    /**
     * Anula la factura y, con ella, su asiento (F3.1 §4.4 D-3: los
     * documentos se anulan desde sí mismos, nunca anulando el asiento
     * directo — acá es al revés: anular la factura anula su asiento).
     */
    @Transactional
    public FacturaVenta anular(Long id, String motivo) {
        FacturaVenta f = obtener(id);
        var antes = mapper.aResponse(f);
        TransicionEstadoValidator.validar(f.getEstado(), EstadoDocumento.ANULADO);

        if (f.getAsiento() != null) {
            asientoService.anularPorDocumento(f.getAsiento().getId(), motivo);
        }
        f.setEstado(EstadoDocumento.ANULADO);

        auditoria.registrar(AccionAuditoria.ANULAR, "FacturaVenta", id, antes, mapper.aResponse(f));
        return f;
    }

    private FacturaVenta obtenerBorrador(Long id) {
        FacturaVenta f = obtener(id);
        if (f.getEstado() != EstadoDocumento.BORRADOR) {
            throw new NegocioException("TRANSICION_ESTADO_INVALIDA", "Solo se pueden editar o eliminar facturas en borrador");
        }
        return f;
    }

    private void reemplazarLineas(FacturaVenta f, List<FacturaVentaLineaRequest> nuevas) {
        f.getLineas().clear();
        int orden = 1;
        for (FacturaVentaLineaRequest r : nuevas) {
            if (!ALICUOTAS_IVA_VALIDAS.contains(r.alicuotaIva())) {
                throw new NegocioException("ALICUOTA_IVA_INVALIDA",
                        "%s no es una alícuota de IVA válida (0, 2.5, 5, 10.5, 21, 27)".formatted(r.alicuotaIva()));
            }
            FacturaVentaLinea l = new FacturaVentaLinea();
            l.setFacturaVenta(f);
            l.setOrden(orden++);
            l.setDescripcion(r.descripcion());
            l.setTipo(r.tipo());
            l.setImporteNeto(r.importeNeto());
            l.setAlicuotaIva(r.alicuotaIva());
            l.setImporteIva(r.importeNeto().multiply(r.alicuotaIva()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            l.setTipoIngreso(r.tipoIngreso() != null ? r.tipoIngreso() : TipoIngreso.VENTA);
            l.setCuentaContable(r.cuentaContableId() != null ? resolverCuentaContable(r.cuentaContableId()) : null);
            f.getLineas().add(l);
        }
    }

    /**
     * Recalcula los agregados persistidos (F1.1 §6.5) a partir de las
     * líneas: neto por tipo de IVA, IVA total, total en moneda original y
     * su materialización a ARS. En ARS, {@code tipoCambio} siempre es 1 —
     * mismo criterio que {@code AsientoService.validarYNormalizarLinea}.
     */
    private void recalcularTotales(FacturaVenta f) {
        BigDecimal netoGravado = BigDecimal.ZERO;
        BigDecimal noGravado = BigDecimal.ZERO;
        BigDecimal exento = BigDecimal.ZERO;
        BigDecimal importeIva = BigDecimal.ZERO;
        for (FacturaVentaLinea l : f.getLineas()) {
            switch (l.getTipo()) {
                case GRAVADO -> netoGravado = netoGravado.add(l.getImporteNeto());
                case NO_GRAVADO -> noGravado = noGravado.add(l.getImporteNeto());
                case EXENTO -> exento = exento.add(l.getImporteNeto());
            }
            importeIva = importeIva.add(l.getImporteIva());
        }
        f.setNetoGravado(netoGravado);
        f.setNoGravado(noGravado);
        f.setExento(exento);
        f.setImporteIva(importeIva);
        BigDecimal total = netoGravado.add(noGravado).add(exento).add(importeIva);
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

    private Cliente resolverCliente(Long id) {
        return clienteRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cliente " + id + " no encontrado"));
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
}

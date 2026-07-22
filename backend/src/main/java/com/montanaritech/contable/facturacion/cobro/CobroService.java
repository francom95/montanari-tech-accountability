package com.montanaritech.contable.facturacion.cobro;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.estado.TransicionEstadoValidator;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.contabilidad.asiento.AsientoLinea;
import com.montanaritech.contable.contabilidad.asiento.AsientoService;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTipo;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTributo;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTributoRepository;
import com.montanaritech.contable.facturacion.cobro.dto.AplicarAnticipoRequest;
import com.montanaritech.contable.facturacion.cobro.dto.CobroCrearRequest;
import com.montanaritech.contable.facturacion.cobro.dto.CobroEditarRequest;
import com.montanaritech.contable.facturacion.cobro.dto.CobroImputacionRequest;
import com.montanaritech.contable.facturacion.cobro.dto.CobroTributoRequest;
import com.montanaritech.contable.facturacion.cobro.dto.SaldoFacturaResponse;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaRepository;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.cliente.ClienteRepository;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
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
public class CobroService {

    private static final Set<com.montanaritech.contable.facturacion.comprobantetributo.TipoTributo> TRIBUTOS_APLICABLES_A_COBRO =
            Set.of(com.montanaritech.contable.facturacion.comprobantetributo.TipoTributo.RETENCION_GANANCIAS,
                    com.montanaritech.contable.facturacion.comprobantetributo.TipoTributo.RETENCION_IVA);
    private static final String MONEDA_LIBRO = "ARS";

    private final CobroRepository repo;
    private final CobroImputacionRepository cobroImputacionRepo;
    private final AplicacionAnticipoClienteRepository aplicacionAnticipoRepo;
    private final CobroMapper mapper;
    private final AuditoriaService auditoria;
    private final AsientoService asientoService;
    private final CobroAsientoGenerator generator;
    private final ClienteRepository clienteRepo;
    private final MonedaRepository monedaRepo;
    private final CuentaBancariaRepository cuentaBancariaRepo;
    private final FacturaVentaRepository facturaVentaRepo;
    private final ComprobanteTributoRepository comprobanteTributoRepo;

    @Transactional(readOnly = true)
    public Page<Cobro> listar(EstadoDocumento estado, Long clienteId, LocalDate fechaDesde, LocalDate fechaHasta, Pageable p) {
        return repo.buscar(estado, clienteId, fechaDesde, fechaHasta, p);
    }

    @Transactional(readOnly = true)
    public Cobro obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cobro " + id + " no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<ComprobanteTributo> tributosDe(Long cobroId) {
        return comprobanteTributoRepo.findByComprobanteTipoAndComprobanteIdOrderByIdAsc(ComprobanteTipo.COBRO, cobroId);
    }

    @Transactional(readOnly = true)
    public List<AplicacionAnticipoCliente> aplicacionesDe(Long cobroId) {
        return aplicacionAnticipoRepo.findByCobro_IdOrderByIdAsc(cobroId);
    }

    @Transactional(readOnly = true)
    public BigDecimal montoAnticipoDisponible(Cobro cobro) {
        BigDecimal aplicado = aplicacionAnticipoRepo.findByCobro_IdOrderByIdAsc(cobro.getId()).stream()
                .map(AplicacionAnticipoCliente::getMontoOriginal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return cobro.getMontoAnticipo().subtract(aplicado);
    }

    @Transactional
    public Cobro crearBorrador(CobroCrearRequest req) {
        Cobro c = new Cobro();
        c.setCliente(resolverCliente(req.clienteId()));
        c.setFecha(req.fecha());
        c.setMoneda(resolverMoneda(req.monedaId()));
        c.setTipoCambio(req.tipoCambio());
        c.setCuentaBancaria(resolverCuentaBancaria(req.cuentaBancariaId()));
        c.setObservaciones(req.observaciones());
        c.setEstado(EstadoDocumento.BORRADOR);
        c.setTotal(req.total());
        reemplazarLineas(c, req.lineas());
        recalcularTotales(c);

        Cobro guardado = repo.save(c);
        reemplazarTributos(guardado.getId(), req.tributos());
        auditoria.registrar(AccionAuditoria.CREAR, "Cobro", guardado.getId(), null, respuestaCompleta(guardado));
        return guardado;
    }

    @Transactional
    public Cobro editarBorrador(Long id, CobroEditarRequest req) {
        Cobro c = obtenerBorrador(id);
        var antes = respuestaCompleta(c);

        c.setCliente(resolverCliente(req.clienteId()));
        c.setFecha(req.fecha());
        c.setMoneda(resolverMoneda(req.monedaId()));
        c.setTipoCambio(req.tipoCambio());
        c.setCuentaBancaria(resolverCuentaBancaria(req.cuentaBancariaId()));
        c.setObservaciones(req.observaciones());
        c.setTotal(req.total());
        reemplazarLineas(c, req.lineas());
        recalcularTotales(c);
        reemplazarTributos(id, req.tributos());

        auditoria.registrar(AccionAuditoria.EDITAR, "Cobro", id, antes, respuestaCompleta(c));
        return c;
    }

    @Transactional
    public void eliminarBorrador(Long id) {
        Cobro c = obtenerBorrador(id);
        var antes = respuestaCompleta(c);
        comprobanteTributoRepo.deleteByComprobanteTipoAndComprobanteId(ComprobanteTipo.COBRO, id);
        repo.delete(c);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "Cobro", id, antes, null);
    }

    @Transactional
    public Cobro confirmar(Long id) {
        Cobro c = obtener(id);
        var antes = respuestaCompleta(c);
        TransicionEstadoValidator.validar(c.getEstado(), EstadoDocumento.CONFIRMADO);

        var asientoGenerado = generator.generar(c);
        var asiento = asientoService.registrarAutomatico(asientoGenerado);

        c.setAsiento(asiento);
        c.setEstado(EstadoDocumento.CONFIRMADO);

        auditoria.registrar(AccionAuditoria.CONFIRMAR, "Cobro", id, antes, respuestaCompleta(c));
        return c;
    }

    @Transactional
    public Cobro anular(Long id, String motivo) {
        Cobro c = obtener(id);
        var antes = respuestaCompleta(c);
        TransicionEstadoValidator.validar(c.getEstado(), EstadoDocumento.ANULADO);

        if (!aplicacionAnticipoRepo.findByCobro_IdOrderByIdAsc(id).isEmpty()) {
            throw new NegocioException("COBRO_CON_APLICACIONES_DE_ANTICIPO",
                    "No se puede anular: este cobro ya tiene aplicaciones de anticipo registradas contra facturas");
        }

        if (c.getAsiento() != null) {
            asientoService.anularPorDocumento(c.getAsiento().getId(), motivo);
        }
        c.setEstado(EstadoDocumento.ANULADO);

        auditoria.registrar(AccionAuditoria.ANULAR, "Cobro", id, antes, respuestaCompleta(c));
        return c;
    }

    @Transactional
    public AplicacionAnticipoCliente aplicarAnticipo(Long cobroId, AplicarAnticipoRequest req) {
        Cobro anticipo = obtener(cobroId);
        if (anticipo.getEstado() != EstadoDocumento.CONFIRMADO) {
            throw new NegocioException("COBRO_NO_CONFIRMADO", "Solo se puede aplicar el anticipo de un cobro confirmado");
        }
        BigDecimal disponible = montoAnticipoDisponible(anticipo);
        if (req.monto().compareTo(disponible) > 0) {
            throw new NegocioException("ANTICIPO_INSUFICIENTE",
                    "El anticipo disponible (%s) es menor al monto solicitado".formatted(disponible));
        }

        FacturaVenta factura = facturaVentaRepo.findById(req.facturaVentaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura de venta " + req.facturaVentaId() + " no encontrada"));

        var resultado = generator.generarAjusteAplicacionAnticipo(anticipo, factura, req.monto(), req.fecha());
        var asiento = asientoService.registrarAutomatico(resultado.asientoGenerado());

        AplicacionAnticipoCliente aplicacion = new AplicacionAnticipoCliente();
        aplicacion.setCobro(anticipo);
        aplicacion.setFacturaVenta(factura);
        aplicacion.setFecha(req.fecha());
        aplicacion.setMontoOriginal(req.monto());
        aplicacion.setMontoArsCancelado(resultado.montoArsCancelado());
        aplicacion.setAsiento(asiento);

        AplicacionAnticipoCliente guardada = aplicacionAnticipoRepo.save(aplicacion);
        auditoria.registrar(AccionAuditoria.CREAR, "AplicacionAnticipoCliente", guardada.getId(), null, null);
        return guardada;
    }

    @Transactional(readOnly = true)
    public SaldoFacturaResponse saldoFacturaVenta(Long facturaVentaId) {
        FacturaVenta factura = resolverFacturaVenta(facturaVentaId);
        List<CobroImputacion> imputaciones = cobroImputacionRepo.findByFacturaVenta_IdAndCobro_EstadoOrderByIdAsc(
                facturaVentaId, EstadoDocumento.CONFIRMADO);
        List<AplicacionAnticipoCliente> aplicaciones = aplicacionAnticipoRepo.findByFacturaVenta_IdOrderByIdAsc(facturaVentaId);

        BigDecimal imputado = imputaciones.stream().map(CobroImputacion::getMontoImputadoOriginal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(aplicaciones.stream().map(AplicacionAnticipoCliente::getMontoOriginal).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal imputadoArs = imputaciones.stream().map(CobroImputacion::getMontoArsCancelado)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(aplicaciones.stream().map(AplicacionAnticipoCliente::getMontoArsCancelado).reduce(BigDecimal.ZERO, BigDecimal::add));

        return new SaldoFacturaResponse(factura.getTotal(), imputado, factura.getTotal().subtract(imputado),
                factura.getTotalArs(), imputadoArs, factura.getTotalArs().subtract(imputadoArs));
    }

    private Cobro obtenerBorrador(Long id) {
        Cobro c = obtener(id);
        if (c.getEstado() != EstadoDocumento.BORRADOR) {
            throw new NegocioException("TRANSICION_ESTADO_INVALIDA", "Solo se pueden editar o eliminar cobros en borrador");
        }
        return c;
    }

    private void reemplazarLineas(Cobro c, List<CobroImputacionRequest> nuevas) {
        c.getLineas().clear();
        if (nuevas == null) {
            return;
        }
        int orden = 1;
        for (CobroImputacionRequest r : nuevas) {
            CobroImputacion l = new CobroImputacion();
            l.setCobro(c);
            l.setOrden(orden++);
            l.setFacturaVenta(resolverFacturaVenta(r.facturaVentaId()));
            l.setMontoImputadoOriginal(r.montoImputadoOriginal());
            c.getLineas().add(l);
        }
    }

    private void reemplazarTributos(Long cobroId, List<CobroTributoRequest> tributos) {
        comprobanteTributoRepo.deleteByComprobanteTipoAndComprobanteId(ComprobanteTipo.COBRO, cobroId);
        if (tributos == null) {
            return;
        }
        for (CobroTributoRequest r : tributos) {
            if (!TRIBUTOS_APLICABLES_A_COBRO.contains(r.tipo())) {
                throw new NegocioException("TRIBUTO_NO_APLICABLE_A_COBRO",
                        "%s no es una retención sufrida aplicable a un cobro".formatted(r.tipo()));
            }
            ComprobanteTributo t = new ComprobanteTributo();
            t.setComprobanteTipo(ComprobanteTipo.COBRO);
            t.setComprobanteId(cobroId);
            t.setTipo(r.tipo());
            t.setImporte(r.importe());
            comprobanteTributoRepo.save(t);
        }
    }

    private void recalcularTotales(Cobro c) {
        BigDecimal sumaImputado = c.getLineas().stream()
                .map(CobroImputacion::getMontoImputadoOriginal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sumaImputado.compareTo(c.getTotal()) > 0) {
            throw new NegocioException("IMPUTACIONES_EXCEDEN_TOTAL_COBRADO",
                    "La suma de las imputaciones no puede superar el total cobrado");
        }

        if (MONEDA_LIBRO.equals(c.getMoneda().getCodigo())) {
            c.setTipoCambio(new BigDecimal("1.000000"));
            c.setFuenteTc(null);
            c.setTotalArs(c.getTotal());
        } else {
            c.setFuenteTc(AsientoLinea.FuenteTc.MANUAL);
            c.setTotalArs(c.getTotal().multiply(c.getTipoCambio()).setScale(2, RoundingMode.HALF_UP));
        }
    }

    private com.montanaritech.contable.facturacion.cobro.dto.CobroResponse respuestaCompleta(Cobro c) {
        return mapper.aResponse(c, tributosDe(c.getId()), aplicacionesDe(c.getId()), montoAnticipoDisponible(c));
    }

    private Cliente resolverCliente(Long id) {
        return clienteRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cliente " + id + " no encontrado"));
    }

    private Moneda resolverMoneda(Long id) {
        return monedaRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + id + " no encontrada"));
    }

    private CuentaBancaria resolverCuentaBancaria(Long id) {
        return cuentaBancariaRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cuenta bancaria " + id + " no encontrada"));
    }

    private FacturaVenta resolverFacturaVenta(Long id) {
        return facturaVentaRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Factura de venta " + id + " no encontrada"));
    }
}

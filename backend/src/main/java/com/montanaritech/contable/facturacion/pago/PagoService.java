package com.montanaritech.contable.facturacion.pago;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.estado.TransicionEstadoValidator;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.contabilidad.asiento.AsientoLinea;
import com.montanaritech.contable.contabilidad.asiento.AsientoService;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompra;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompraRepository;
import com.montanaritech.contable.facturacion.pago.dto.AplicarAnticipoProveedorRequest;
import com.montanaritech.contable.facturacion.pago.dto.PagoCrearRequest;
import com.montanaritech.contable.facturacion.pago.dto.PagoEditarRequest;
import com.montanaritech.contable.facturacion.pago.dto.PagoImputacionRequest;
import com.montanaritech.contable.facturacion.pago.dto.PagoResponse;
import com.montanaritech.contable.facturacion.pago.dto.SaldoFacturaCompraResponse;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PagoService {

    private static final String MONEDA_LIBRO = "ARS";

    private final PagoRepository repo;
    private final PagoImputacionRepository pagoImputacionRepo;
    private final AplicacionAnticipoProveedorRepository aplicacionAnticipoRepo;
    private final PagoMapper mapper;
    private final AuditoriaService auditoria;
    private final AsientoService asientoService;
    private final PagoAsientoGenerator generator;
    private final ProveedorRepository proveedorRepo;
    private final MonedaRepository monedaRepo;
    private final CuentaBancariaRepository cuentaBancariaRepo;
    private final FacturaCompraRepository facturaCompraRepo;

    @Transactional(readOnly = true)
    public Page<Pago> listar(EstadoDocumento estado, Long proveedorId, LocalDate fechaDesde, LocalDate fechaHasta, Pageable p) {
        return repo.buscar(estado, proveedorId, fechaDesde, fechaHasta, p);
    }

    @Transactional(readOnly = true)
    public Pago obtener(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Pago " + id + " no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<AplicacionAnticipoProveedor> aplicacionesDe(Long pagoId) {
        return aplicacionAnticipoRepo.findByPago_IdOrderByIdAsc(pagoId);
    }

    @Transactional(readOnly = true)
    public BigDecimal montoAnticipoDisponible(Pago pago) {
        BigDecimal aplicado = aplicacionAnticipoRepo.findByPago_IdOrderByIdAsc(pago.getId()).stream()
                .map(AplicacionAnticipoProveedor::getMontoOriginal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return pago.getMontoAnticipo().subtract(aplicado);
    }

    @Transactional
    public Pago crearBorrador(PagoCrearRequest req) {
        Pago p = new Pago();
        p.setProveedor(resolverProveedor(req.proveedorId()));
        p.setFecha(req.fecha());
        p.setMoneda(resolverMoneda(req.monedaId()));
        p.setTipoCambio(req.tipoCambio());
        p.setCuentaBancaria(resolverCuentaBancaria(req.cuentaBancariaId()));
        p.setObservaciones(req.observaciones());
        p.setEstado(EstadoDocumento.BORRADOR);
        p.setTotal(req.total());
        reemplazarLineas(p, req.lineas());
        recalcularTotales(p);

        Pago guardado = repo.save(p);
        auditoria.registrar(AccionAuditoria.CREAR, "Pago", guardado.getId(), null, respuestaCompleta(guardado));
        return guardado;
    }

    @Transactional
    public Pago editarBorrador(Long id, PagoEditarRequest req) {
        Pago p = obtenerBorrador(id);
        var antes = respuestaCompleta(p);

        p.setProveedor(resolverProveedor(req.proveedorId()));
        p.setFecha(req.fecha());
        p.setMoneda(resolverMoneda(req.monedaId()));
        p.setTipoCambio(req.tipoCambio());
        p.setCuentaBancaria(resolverCuentaBancaria(req.cuentaBancariaId()));
        p.setObservaciones(req.observaciones());
        p.setTotal(req.total());
        reemplazarLineas(p, req.lineas());
        recalcularTotales(p);

        auditoria.registrar(AccionAuditoria.EDITAR, "Pago", id, antes, respuestaCompleta(p));
        return p;
    }

    @Transactional
    public void eliminarBorrador(Long id) {
        Pago p = obtenerBorrador(id);
        var antes = respuestaCompleta(p);
        repo.delete(p);
        auditoria.registrar(AccionAuditoria.ELIMINAR, "Pago", id, antes, null);
    }

    @Transactional
    public Pago confirmar(Long id) {
        Pago p = obtener(id);
        var antes = respuestaCompleta(p);
        TransicionEstadoValidator.validar(p.getEstado(), EstadoDocumento.CONFIRMADO);

        var asientoGenerado = generator.generar(p);
        var asiento = asientoService.registrarAutomatico(asientoGenerado);

        p.setAsiento(asiento);
        p.setEstado(EstadoDocumento.CONFIRMADO);

        auditoria.registrar(AccionAuditoria.CONFIRMAR, "Pago", id, antes, respuestaCompleta(p));
        return p;
    }

    @Transactional
    public Pago anular(Long id, String motivo) {
        Pago p = obtener(id);
        var antes = respuestaCompleta(p);
        TransicionEstadoValidator.validar(p.getEstado(), EstadoDocumento.ANULADO);

        if (!aplicacionAnticipoRepo.findByPago_IdOrderByIdAsc(id).isEmpty()) {
            throw new NegocioException("PAGO_CON_APLICACIONES_DE_ANTICIPO",
                    "No se puede anular: este pago ya tiene aplicaciones de anticipo registradas contra facturas");
        }

        if (p.getAsiento() != null) {
            asientoService.anularPorDocumento(p.getAsiento().getId(), motivo);
        }
        p.setEstado(EstadoDocumento.ANULADO);

        auditoria.registrar(AccionAuditoria.ANULAR, "Pago", id, antes, respuestaCompleta(p));
        return p;
    }

    @Transactional
    public AplicacionAnticipoProveedor aplicarAnticipo(Long pagoId, AplicarAnticipoProveedorRequest req) {
        Pago anticipo = obtener(pagoId);
        if (anticipo.getEstado() != EstadoDocumento.CONFIRMADO) {
            throw new NegocioException("PAGO_NO_CONFIRMADO", "Solo se puede aplicar el anticipo de un pago confirmado");
        }
        BigDecimal disponible = montoAnticipoDisponible(anticipo);
        if (req.monto().compareTo(disponible) > 0) {
            throw new NegocioException("ANTICIPO_INSUFICIENTE",
                    "El anticipo disponible (%s) es menor al monto solicitado".formatted(disponible));
        }

        FacturaCompra factura = facturaCompraRepo.findById(req.facturaCompraId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura de compra " + req.facturaCompraId() + " no encontrada"));

        var resultado = generator.generarAjusteAplicacionAnticipo(anticipo, factura, req.monto(), req.fecha());
        var asiento = asientoService.registrarAutomatico(resultado.asientoGenerado());

        AplicacionAnticipoProveedor aplicacion = new AplicacionAnticipoProveedor();
        aplicacion.setPago(anticipo);
        aplicacion.setFacturaCompra(factura);
        aplicacion.setFecha(req.fecha());
        aplicacion.setMontoOriginal(req.monto());
        aplicacion.setMontoArsCancelado(resultado.montoArsCancelado());
        aplicacion.setAsiento(asiento);

        AplicacionAnticipoProveedor guardada = aplicacionAnticipoRepo.save(aplicacion);
        auditoria.registrar(AccionAuditoria.CREAR, "AplicacionAnticipoProveedor", guardada.getId(), null, null);
        return guardada;
    }

    @Transactional(readOnly = true)
    public SaldoFacturaCompraResponse saldoFacturaCompra(Long facturaCompraId) {
        FacturaCompra factura = resolverFacturaCompra(facturaCompraId);
        List<PagoImputacion> imputaciones = pagoImputacionRepo.findByFacturaCompra_IdAndPago_EstadoOrderByIdAsc(
                facturaCompraId, EstadoDocumento.CONFIRMADO);
        List<AplicacionAnticipoProveedor> aplicaciones = aplicacionAnticipoRepo.findByFacturaCompra_IdOrderByIdAsc(facturaCompraId);

        BigDecimal imputado = imputaciones.stream().map(PagoImputacion::getMontoImputadoOriginal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(aplicaciones.stream().map(AplicacionAnticipoProveedor::getMontoOriginal).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal imputadoArs = imputaciones.stream().map(PagoImputacion::getMontoArsCancelado)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(aplicaciones.stream().map(AplicacionAnticipoProveedor::getMontoArsCancelado).reduce(BigDecimal.ZERO, BigDecimal::add));

        return new SaldoFacturaCompraResponse(factura.getTotal(), imputado, factura.getTotal().subtract(imputado),
                factura.getTotalArs(), imputadoArs, factura.getTotalArs().subtract(imputadoArs));
    }

    private Pago obtenerBorrador(Long id) {
        Pago p = obtener(id);
        if (p.getEstado() != EstadoDocumento.BORRADOR) {
            throw new NegocioException("TRANSICION_ESTADO_INVALIDA", "Solo se pueden editar o eliminar pagos en borrador");
        }
        return p;
    }

    private void reemplazarLineas(Pago p, List<PagoImputacionRequest> nuevas) {
        p.getLineas().clear();
        if (nuevas == null) {
            return;
        }
        int orden = 1;
        for (PagoImputacionRequest r : nuevas) {
            PagoImputacion l = new PagoImputacion();
            l.setPago(p);
            l.setOrden(orden++);
            l.setFacturaCompra(resolverFacturaCompra(r.facturaCompraId()));
            l.setMontoImputadoOriginal(r.montoImputadoOriginal());
            p.getLineas().add(l);
        }
    }

    private void recalcularTotales(Pago p) {
        BigDecimal sumaImputado = p.getLineas().stream()
                .map(PagoImputacion::getMontoImputadoOriginal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sumaImputado.compareTo(p.getTotal()) > 0) {
            throw new NegocioException("IMPUTACIONES_EXCEDEN_TOTAL_PAGADO",
                    "La suma de las imputaciones no puede superar el total pagado");
        }

        if (MONEDA_LIBRO.equals(p.getMoneda().getCodigo())) {
            p.setTipoCambio(new BigDecimal("1.000000"));
            p.setFuenteTc(null);
            p.setTotalArs(p.getTotal());
        } else {
            p.setFuenteTc(AsientoLinea.FuenteTc.MANUAL);
            p.setTotalArs(p.getTotal().multiply(p.getTipoCambio()).setScale(2, RoundingMode.HALF_UP));
        }
    }

    private PagoResponse respuestaCompleta(Pago p) {
        return mapper.aResponse(p, aplicacionesDe(p.getId()), montoAnticipoDisponible(p));
    }

    private Proveedor resolverProveedor(Long id) {
        return proveedorRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proveedor " + id + " no encontrado"));
    }

    private Moneda resolverMoneda(Long id) {
        return monedaRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Moneda " + id + " no encontrada"));
    }

    private CuentaBancaria resolverCuentaBancaria(Long id) {
        return cuentaBancariaRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cuenta bancaria " + id + " no encontrada"));
    }

    private FacturaCompra resolverFacturaCompra(Long id) {
        return facturaCompraRepo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Factura de compra " + id + " no encontrada"));
    }
}

package com.montanaritech.contable.busqueda;

import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancario;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancarioRepository;
import com.montanaritech.contable.busqueda.dto.BusquedaGlobalResponse;
import com.montanaritech.contable.busqueda.dto.GrupoResultado;
import com.montanaritech.contable.busqueda.dto.ResultadoItem;
import com.montanaritech.contable.common.tenant.TenantContext;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoRepository;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.facturacion.cobro.Cobro;
import com.montanaritech.contable.facturacion.cobro.CobroRepository;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompra;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompraRepository;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaRepository;
import com.montanaritech.contable.facturacion.pago.Pago;
import com.montanaritech.contable.facturacion.pago.PagoRepository;
import com.montanaritech.contable.impuestos.iibb.LiquidacionIibb;
import com.montanaritech.contable.impuestos.iibb.LiquidacionIibbRepository;
import com.montanaritech.contable.impuestos.iva.LiquidacionIva;
import com.montanaritech.contable.impuestos.iva.LiquidacionIvaRepository;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.cliente.ClienteRepository;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.etapa.Etapa;
import com.montanaritech.contable.maestros.proyecto.etapa.EtapaRepository;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCredito;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCreditoRepository;
import com.montanaritech.contable.pendiente.PendienteAdministrativo;
import com.montanaritech.contable.pendiente.PendienteAdministrativoRepository;
import com.montanaritech.contable.vencimientos.Vencimiento;
import com.montanaritech.contable.vencimientos.VencimientoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Búsqueda global "Lupita" (F9.2): detecta el tipo de término una vez
 * (CUIT → fecha → importe → texto, en ese orden de prioridad — ver
 * {@link DetectorTerminoBusqueda}) y federa a los 16 tipos de entidad con el
 * criterio apropiado para ese tipo, sin repetir la detección por entidad.
 */
@Service
@RequiredArgsConstructor
public class BusquedaGlobalService {

    private static final int LIMITE_POR_GRUPO = 5;

    private final AsientoRepository asientoRepo;
    private final ClienteRepository clienteRepo;
    private final ProveedorRepository proveedorRepo;
    private final ProyectoRepository proyectoRepo;
    private final EtapaRepository etapaRepo;
    private final CuentaContableRepository cuentaContableRepo;
    private final PagoRepository pagoRepo;
    private final CobroRepository cobroRepo;
    private final FacturaVentaRepository facturaVentaRepo;
    private final FacturaCompraRepository facturaCompraRepo;
    private final MovimientoBancarioRepository movimientoBancarioRepo;
    private final TarjetaCreditoRepository tarjetaCreditoRepo;
    private final VencimientoRepository vencimientoRepo;
    private final PendienteAdministrativoRepository pendienteRepo;
    private final LiquidacionIvaRepository liquidacionIvaRepo;
    private final LiquidacionIibbRepository liquidacionIibbRepo;

    @Transactional(readOnly = true)
    public BusquedaGlobalResponse buscar(String termino) {
        String t = termino.trim();
        Pageable pageable = PageRequest.of(0, LIMITE_POR_GRUPO);

        String cuit = DetectorTerminoBusqueda.esCuit(t);
        if (cuit != null) {
            return buscarPorCuit(cuit, pageable);
        }
        LocalDate fecha = DetectorTerminoBusqueda.comoFecha(t);
        if (fecha != null) {
            return buscarPorFecha(fecha, pageable);
        }
        BigDecimal importe = DetectorTerminoBusqueda.comoImporte(t);
        if (importe != null) {
            return buscarPorImporte(importe, pageable);
        }
        return buscarPorTexto(t, pageable);
    }

    private BusquedaGlobalResponse buscarPorCuit(String cuit, Pageable pageable) {
        Map<TipoEntidadBusqueda, GrupoResultado> grupos = new EnumMap<>(TipoEntidadBusqueda.class);
        clienteRepo.findByCuit(cuit).ifPresent(c -> grupos.put(TipoEntidadBusqueda.CLIENTE, unico(mapCliente(c))));
        proveedorRepo.findByCuit(cuit).ifPresent(p -> grupos.put(TipoEntidadBusqueda.PROVEEDOR, unico(mapProveedor(p))));
        grupos.put(TipoEntidadBusqueda.FACTURA_VENTA, grupo(facturaVentaRepo.findByCliente_Cuit(cuit, pageable), this::mapFacturaVenta));
        grupos.put(TipoEntidadBusqueda.FACTURA_COMPRA, grupo(facturaCompraRepo.findByProveedor_Cuit(cuit, pageable), this::mapFacturaCompra));
        grupos.put(TipoEntidadBusqueda.PAGO, grupo(pagoRepo.findByProveedor_Cuit(cuit, pageable), this::mapPago));
        grupos.put(TipoEntidadBusqueda.COBRO, grupo(cobroRepo.findByCliente_Cuit(cuit, pageable), this::mapCobro));
        return new BusquedaGlobalResponse(soloNoVacios(grupos));
    }

    private BusquedaGlobalResponse buscarPorFecha(LocalDate fecha, Pageable pageable) {
        Map<TipoEntidadBusqueda, GrupoResultado> grupos = new EnumMap<>(TipoEntidadBusqueda.class);
        grupos.put(TipoEntidadBusqueda.ASIENTO, grupo(asientoRepo.findByFecha(fecha, pageable), this::mapAsiento));
        grupos.put(TipoEntidadBusqueda.FACTURA_VENTA, grupo(facturaVentaRepo.findByFecha(fecha, pageable), this::mapFacturaVenta));
        grupos.put(TipoEntidadBusqueda.FACTURA_COMPRA, grupo(facturaCompraRepo.findByFecha(fecha, pageable), this::mapFacturaCompra));
        grupos.put(TipoEntidadBusqueda.PAGO, grupo(pagoRepo.findByFecha(fecha, pageable), this::mapPago));
        grupos.put(TipoEntidadBusqueda.COBRO, grupo(cobroRepo.findByFecha(fecha, pageable), this::mapCobro));
        grupos.put(TipoEntidadBusqueda.MOVIMIENTO_BANCARIO, grupo(movimientoBancarioRepo.findByFecha(fecha, pageable), this::mapMovimientoBancario));
        grupos.put(TipoEntidadBusqueda.VENCIMIENTO, grupo(vencimientoRepo.findByFecha(fecha, pageable), this::mapVencimiento));
        grupos.put(TipoEntidadBusqueda.PENDIENTE_ADMINISTRATIVO, grupo(pendienteRepo.findByFechaEstimadaResolucion(fecha, pageable), this::mapPendiente));
        grupos.put(TipoEntidadBusqueda.PROYECTO, grupo(proyectoRepo.findByFechaEstimadaFinalizacionOrFechaRealFinalizacion(fecha, fecha, pageable), this::mapProyecto));
        grupos.put(TipoEntidadBusqueda.ETAPA, grupo(etapaRepo.findByFechaInicioOrFechaEstimadaFin(fecha, fecha, pageable), this::mapEtapa));
        grupos.put(TipoEntidadBusqueda.LIQUIDACION_IVA, grupo(liquidacionIvaRepo.findByFechaDesdeLessThanEqualAndFechaHastaGreaterThanEqual(fecha, fecha, pageable), this::mapLiquidacionIva));
        grupos.put(TipoEntidadBusqueda.LIQUIDACION_IIBB, grupo(liquidacionIibbRepo.findByFechaDesdeLessThanEqualAndFechaHastaGreaterThanEqual(fecha, fecha, pageable), this::mapLiquidacionIibb));
        return new BusquedaGlobalResponse(soloNoVacios(grupos));
    }

    private BusquedaGlobalResponse buscarPorImporte(BigDecimal importe, Pageable pageable) {
        BigDecimal tolerancia = DetectorTerminoBusqueda.toleranciaImporte(importe);
        BigDecimal desde = importe.subtract(tolerancia);
        BigDecimal hasta = importe.add(tolerancia);
        Map<TipoEntidadBusqueda, GrupoResultado> grupos = new EnumMap<>(TipoEntidadBusqueda.class);
        grupos.put(TipoEntidadBusqueda.ASIENTO, grupo(asientoRepo.buscarGlobalPorImporte(desde, hasta, pageable), this::mapAsiento));
        grupos.put(TipoEntidadBusqueda.FACTURA_VENTA, grupo(facturaVentaRepo.findByTotalArsBetween(desde, hasta, pageable), this::mapFacturaVenta));
        grupos.put(TipoEntidadBusqueda.FACTURA_COMPRA, grupo(facturaCompraRepo.findByTotalArsBetween(desde, hasta, pageable), this::mapFacturaCompra));
        grupos.put(TipoEntidadBusqueda.PAGO, grupo(pagoRepo.findByTotalArsBetween(desde, hasta, pageable), this::mapPago));
        grupos.put(TipoEntidadBusqueda.COBRO, grupo(cobroRepo.findByTotalArsBetween(desde, hasta, pageable), this::mapCobro));
        grupos.put(TipoEntidadBusqueda.MOVIMIENTO_BANCARIO, grupo(movimientoBancarioRepo.findByImporteArsBetween(desde, hasta, pageable), this::mapMovimientoBancario));
        grupos.put(TipoEntidadBusqueda.VENCIMIENTO, grupo(vencimientoRepo.findByImporteEstimadoBetween(desde, hasta, pageable), this::mapVencimiento));
        grupos.put(TipoEntidadBusqueda.PROYECTO, grupo(proyectoRepo.findByMontoTotalBetween(desde, hasta, pageable), this::mapProyecto));
        grupos.put(TipoEntidadBusqueda.ETAPA, grupo(etapaRepo.buscarGlobalPorImporte(desde, hasta, pageable), this::mapEtapa));
        grupos.put(TipoEntidadBusqueda.TARJETA_CREDITO, grupo(tarjetaCreditoRepo.findBySaldoActualBetween(desde, hasta, pageable), this::mapTarjetaCredito));
        grupos.put(TipoEntidadBusqueda.LIQUIDACION_IVA, grupo(liquidacionIvaRepo.findBySaldoAPagarBetween(desde, hasta, pageable), this::mapLiquidacionIva));
        grupos.put(TipoEntidadBusqueda.LIQUIDACION_IIBB, grupo(liquidacionIibbRepo.findBySaldoAPagarTotalBetween(desde, hasta, pageable), this::mapLiquidacionIibb));
        return new BusquedaGlobalResponse(soloNoVacios(grupos));
    }

    private BusquedaGlobalResponse buscarPorTexto(String texto, Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Map<TipoEntidadBusqueda, GrupoResultado> grupos = new EnumMap<>(TipoEntidadBusqueda.class);
        grupos.put(TipoEntidadBusqueda.ASIENTO, grupo(asientoRepo.buscarGlobalPorTexto(tenantId, texto, pageable), this::mapAsiento));
        grupos.put(TipoEntidadBusqueda.FACTURA_VENTA, grupo(facturaVentaRepo.buscar(texto, null, null, null, null, null, pageable), this::mapFacturaVenta));
        grupos.put(TipoEntidadBusqueda.FACTURA_COMPRA, grupo(facturaCompraRepo.buscar(texto, null, null, null, null, null, pageable), this::mapFacturaCompra));
        grupos.put(TipoEntidadBusqueda.CLIENTE, grupo(clienteRepo.buscar(texto, null, pageable), this::mapCliente));
        grupos.put(TipoEntidadBusqueda.PROVEEDOR, grupo(proveedorRepo.buscar(texto, null, pageable), this::mapProveedor));
        grupos.put(TipoEntidadBusqueda.PROYECTO, grupo(proyectoRepo.buscar(texto, null, null, pageable), this::mapProyecto));
        grupos.put(TipoEntidadBusqueda.ETAPA, grupo(etapaRepo.buscarGlobalPorTexto(texto, pageable), this::mapEtapa));
        grupos.put(TipoEntidadBusqueda.CUENTA_CONTABLE, grupo(cuentaContableRepo.buscar(texto, null, pageable), this::mapCuentaContable));
        grupos.put(TipoEntidadBusqueda.PAGO, grupo(pagoRepo.buscarGlobalPorTexto(texto, pageable), this::mapPago));
        grupos.put(TipoEntidadBusqueda.COBRO, grupo(cobroRepo.buscarGlobalPorTexto(texto, pageable), this::mapCobro));
        grupos.put(TipoEntidadBusqueda.MOVIMIENTO_BANCARIO, grupo(movimientoBancarioRepo.buscarGlobalPorTexto(tenantId, texto, pageable), this::mapMovimientoBancario));
        grupos.put(TipoEntidadBusqueda.TARJETA_CREDITO, grupo(tarjetaCreditoRepo.buscar(texto, null, pageable), this::mapTarjetaCredito));
        grupos.put(TipoEntidadBusqueda.VENCIMIENTO, grupo(vencimientoRepo.buscarGlobalPorTexto(tenantId, texto, pageable), this::mapVencimiento));
        grupos.put(TipoEntidadBusqueda.PENDIENTE_ADMINISTRATIVO, grupo(pendienteRepo.buscar(texto, null, null, null, null, null, pageable), this::mapPendiente));
        return new BusquedaGlobalResponse(soloNoVacios(grupos));
    }

    private <T> GrupoResultado grupo(Page<T> page, Function<T, ResultadoItem> mapper) {
        return new GrupoResultado(page.getContent().stream().map(mapper).toList(), page.getTotalElements());
    }

    private GrupoResultado unico(ResultadoItem item) {
        return new GrupoResultado(List.of(item), 1);
    }

    private Map<TipoEntidadBusqueda, GrupoResultado> soloNoVacios(Map<TipoEntidadBusqueda, GrupoResultado> grupos) {
        grupos.values().removeIf(g -> g.items().isEmpty());
        return grupos;
    }

    private ResultadoItem mapAsiento(Asiento a) {
        String numero = a.getNumero() != null ? " (Nº " + a.getNumero() + ")" : "";
        return new ResultadoItem(a.getId(), TipoEntidadBusqueda.ASIENTO, a.getDescripcion() + numero, a.getFecha(), null);
    }

    private ResultadoItem mapFacturaVenta(FacturaVenta f) {
        return new ResultadoItem(f.getId(), TipoEntidadBusqueda.FACTURA_VENTA,
                "Factura " + f.getNumero() + " - " + f.getCliente().getNombre(), f.getFecha(), null);
    }

    private ResultadoItem mapFacturaCompra(FacturaCompra f) {
        return new ResultadoItem(f.getId(), TipoEntidadBusqueda.FACTURA_COMPRA,
                "Factura " + f.getNumero() + " - " + f.getProveedor().getNombre(), f.getFecha(), null);
    }

    private ResultadoItem mapCliente(Cliente c) {
        return new ResultadoItem(c.getId(), TipoEntidadBusqueda.CLIENTE, c.getNombre() + " (" + c.getCuit() + ")", null, null);
    }

    private ResultadoItem mapProveedor(Proveedor p) {
        return new ResultadoItem(p.getId(), TipoEntidadBusqueda.PROVEEDOR, p.getNombre() + " (" + p.getCuit() + ")", null, null);
    }

    private ResultadoItem mapProyecto(Proyecto p) {
        return new ResultadoItem(p.getId(), TipoEntidadBusqueda.PROYECTO, p.getNombre(), p.getFechaEstimadaFinalizacion(), null);
    }

    private ResultadoItem mapEtapa(Etapa e) {
        return new ResultadoItem(e.getId(), TipoEntidadBusqueda.ETAPA,
                e.getNombre() + " - " + e.getProyecto().getNombre(), e.getFechaInicio(), e.getProyecto().getId());
    }

    private ResultadoItem mapCuentaContable(CuentaContable c) {
        return new ResultadoItem(c.getId(), TipoEntidadBusqueda.CUENTA_CONTABLE, c.getCodigo() + " - " + c.getNombre(), null, null);
    }

    private ResultadoItem mapPago(Pago p) {
        return new ResultadoItem(p.getId(), TipoEntidadBusqueda.PAGO, "Pago a " + p.getProveedor().getNombre(), p.getFecha(), null);
    }

    private ResultadoItem mapCobro(Cobro c) {
        return new ResultadoItem(c.getId(), TipoEntidadBusqueda.COBRO, "Cobro de " + c.getCliente().getNombre(), c.getFecha(), null);
    }

    private ResultadoItem mapMovimientoBancario(MovimientoBancario m) {
        return new ResultadoItem(m.getId(), TipoEntidadBusqueda.MOVIMIENTO_BANCARIO, m.getDescripcion(), m.getFecha(), null);
    }

    private ResultadoItem mapTarjetaCredito(TarjetaCredito t) {
        return new ResultadoItem(t.getId(), TipoEntidadBusqueda.TARJETA_CREDITO, t.getEntidad(), null, null);
    }

    private ResultadoItem mapVencimiento(Vencimiento v) {
        return new ResultadoItem(v.getId(), TipoEntidadBusqueda.VENCIMIENTO, v.getDescripcion(), v.getFecha(), null);
    }

    private ResultadoItem mapPendiente(PendienteAdministrativo p) {
        return new ResultadoItem(p.getId(), TipoEntidadBusqueda.PENDIENTE_ADMINISTRATIVO, p.getTitulo(), p.getFechaEstimadaResolucion(), null);
    }

    private ResultadoItem mapLiquidacionIva(LiquidacionIva l) {
        return new ResultadoItem(l.getId(), TipoEntidadBusqueda.LIQUIDACION_IVA, "IVA " + l.getMes() + "/" + l.getAnio(), l.getFechaDesde(), null);
    }

    private ResultadoItem mapLiquidacionIibb(LiquidacionIibb l) {
        return new ResultadoItem(l.getId(), TipoEntidadBusqueda.LIQUIDACION_IIBB, "IIBB " + l.getMes() + "/" + l.getAnio(), l.getFechaDesde(), null);
    }
}

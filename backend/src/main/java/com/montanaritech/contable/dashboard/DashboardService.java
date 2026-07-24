package com.montanaritech.contable.dashboard;

import com.montanaritech.contable.common.reporte.EstadoVencimiento;
import com.montanaritech.contable.common.saldo.RecalculoSaldoService;
import com.montanaritech.contable.contabilidad.estadoresultados.EstadoResultadosService;
import com.montanaritech.contable.dashboard.dto.DashboardResponse;
import com.montanaritech.contable.dashboard.dto.DashboardResponse.IndicadorMonto;
import com.montanaritech.contable.dashboard.dto.DashboardResponse.VencimientoImpuesto;
import com.montanaritech.contable.facturacion.cobro.CobroRepository;
import com.montanaritech.contable.facturacion.cuentasporcobrar.CuentaPorCobrarService;
import com.montanaritech.contable.facturacion.cuentasporcobrar.dto.TotalPorMonedaResponse;
import com.montanaritech.contable.facturacion.cuentasporpagar.CuentaPorPagarService;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaRepository;
import com.montanaritech.contable.impuestos.iibb.LiquidacionIibbRepository;
import com.montanaritech.contable.impuestos.iva.LiquidacionIvaRepository;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.etapa.Etapa;
import com.montanaritech.contable.maestros.proyecto.etapa.EtapaRepository;
import com.montanaritech.contable.maestros.proyecto.presupuesto.PresupuestoProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.rentabilidad.ReporteRentabilidadProyectoService;
import com.montanaritech.contable.maestros.proyecto.rentabilidad.dto.ReporteRentabilidadProyectoResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agrega los 12 indicadores del dashboard (F7.5) para un período, sin
 * recalcular nada que ya exista: reusa los servicios de F4.5/F7.2-F7.4 tal
 * cual. Cacheado 2 minutos por (anio, mes) — ver {@code CacheConfig}.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ConfiguracionDashboardRepository configuracionRepo;
    private final EstadoResultadosService estadoResultadosService;
    private final FacturaVentaRepository facturaVentaRepo;
    private final CobroRepository cobroRepo;
    private final CuentaPorCobrarService cuentaPorCobrarService;
    private final CuentaPorPagarService cuentaPorPagarService;
    private final CuentaBancariaRepository cuentaBancariaRepo;
    private final RecalculoSaldoService recalculoSaldoService;
    private final ProyectoRepository proyectoRepo;
    private final PresupuestoProyectoRepository presupuestoProyectoRepo;
    private final EtapaRepository etapaRepo;
    private final ReporteRentabilidadProyectoService reporteRentabilidadService;
    private final LiquidacionIvaRepository liquidacionIvaRepo;
    private final LiquidacionIibbRepository liquidacionIibbRepo;

    @Transactional(readOnly = true)
    @Cacheable("dashboard")
    public DashboardResponse obtener(int anio, int mes) {
        LocalDate fechaDesde = LocalDate.of(anio, mes, 1);
        LocalDate fechaHasta = fechaDesde.plusMonths(1).minusDays(1);
        ConfiguracionDashboard config = configuracionRepo.findFirstByOrderByIdAsc().orElseGet(ConfiguracionDashboard::new);

        IndicadorMonto resultadoMensual = new IndicadorMonto(
                estadoResultadosService.porMes(anio, mes).calculado().resultadoFinal(), "/reportes/estado-resultados");

        IndicadorMonto ventasDelPeriodo = new IndicadorMonto(ventasNetasDelPeriodo(fechaDesde, fechaHasta), "/facturacion/ventas");

        IndicadorMonto cobrosDelPeriodo = new IndicadorMonto(
                cobroRepo.sumarTotalArsConfirmadoEnPeriodo(fechaDesde, fechaHasta), "/facturacion/cobros");

        IndicadorMonto cuentasPorCobrar = new IndicadorMonto(
                sumarTotalArs(cuentaPorCobrarService.calcular(null, null, null, null, fechaHasta, null).totalesPorMoneda()),
                "/facturacion/cuentas-por-cobrar");

        IndicadorMonto cuentasPorPagar = new IndicadorMonto(
                sumarTotalArsCxP(cuentaPorPagarService.calcular(null, null, null, null, fechaHasta, null).totalesPorMoneda()),
                "/facturacion/cuentas-por-pagar");

        IndicadorMonto obligacionesProximas = new IndicadorMonto(
                obligacionesProximas(fechaHasta, config.getVentanaObligacionesDias()), "/facturacion/cuentas-por-pagar");

        List<String> alertas = new java.util.ArrayList<>();
        BigDecimal[] saldosCajaYBanco = saldosCajaYBanco(fechaHasta, alertas);
        IndicadorMonto saldoCaja = new IndicadorMonto(saldosCajaYBanco[0], "/cuentas-bancarias");
        IndicadorMonto saldoBanco = new IndicadorMonto(saldosCajaYBanco[1], "/cuentas-bancarias");

        BigDecimal[] margenYEgresos = margenEstimadoYEgresosProyectados();
        IndicadorMonto margenEstimado = new IndicadorMonto(margenYEgresos[0], "/proyectos");
        IndicadorMonto egresosProyectados = new IndicadorMonto(margenYEgresos[1], "/proyectos");

        VencimientoImpuesto proximoVencimientoIva = vencimientoIva(anio, mes, config.getDiaVencimientoIva());
        VencimientoImpuesto proximoVencimientoIibb = vencimientoIibb(anio, mes, config.getDiaVencimientoIibb());

        return new DashboardResponse(anio, mes, resultadoMensual, ventasDelPeriodo, cobrosDelPeriodo, cuentasPorCobrar,
                cuentasPorPagar, obligacionesProximas, saldoCaja, saldoBanco, margenEstimado, egresosProyectados,
                proximoVencimientoIva, proximoVencimientoIibb, alertas);
    }

    /** Facturas de venta confirmadas del período; las notas de crédito restan (mismo criterio que F6.3 §FACTURACION). */
    private BigDecimal ventasNetasDelPeriodo(LocalDate fechaDesde, LocalDate fechaHasta) {
        BigDecimal total = BigDecimal.ZERO;
        for (FacturaVenta f : facturaVentaRepo.buscarConfirmadasParaReporte(null, null, null, fechaDesde, fechaHasta)) {
            BigDecimal monto = f.getTotalArs();
            if (f.getTipoComprobante().name().startsWith("NOTA_CREDITO")) {
                monto = monto.negate();
            }
            total = total.add(monto);
        }
        return total;
    }

    private BigDecimal sumarTotalArs(List<TotalPorMonedaResponse> totales) {
        BigDecimal total = BigDecimal.ZERO;
        for (TotalPorMonedaResponse t : totales) {
            total = total.add(t.totalSaldoArs());
        }
        return total;
    }

    private BigDecimal sumarTotalArsCxP(List<com.montanaritech.contable.facturacion.cuentasporpagar.dto.TotalPorMonedaResponse> totales) {
        BigDecimal total = BigDecimal.ZERO;
        for (var t : totales) {
            total = total.add(t.totalSaldoArs());
        }
        return total;
    }

    /** Facturas de compra POR_VENCER dentro de la ventana configurada (distinto de "cuentas por pagar", que no tiene ventana). */
    private BigDecimal obligacionesProximas(LocalDate fechaHasta, int ventanaDias) {
        LocalDate limite = fechaHasta.plusDays(ventanaDias);
        BigDecimal total = BigDecimal.ZERO;
        var resultado = cuentaPorPagarService.calcular(null, null, null, null, fechaHasta, EstadoVencimiento.POR_VENCER);
        for (var fila : resultado.filas()) {
            if (fila.fechaVencimiento() != null && !fila.fechaVencimiento().isAfter(limite)) {
                total = total.add(fila.saldoArs());
            }
        }
        return total;
    }

    /**
     * Saldo real de caja (CAJA_FISICA) y de banco (resto de tipos), a la fecha pedida. Solo cuentas en
     * ARS: sumar saldos de distintas monedas sin convertir sería incorrecto, y no hay TC histórico por
     * movimiento para hacerlo bien (mismo criterio que F7.4 con comisiones en otra moneda: se excluyen
     * y se avisa, en vez de asumir un TC).
     */
    private BigDecimal[] saldosCajaYBanco(LocalDate fechaHasta, List<String> alertas) {
        BigDecimal caja = BigDecimal.ZERO;
        BigDecimal banco = BigDecimal.ZERO;
        int excluidas = 0;
        for (CuentaBancaria cuenta : cuentaBancariaRepo.findByActivoTrue()) {
            if (cuenta.getMoneda() == null || !"ARS".equals(cuenta.getMoneda().getCodigo())) {
                excluidas++;
                continue;
            }
            BigDecimal saldo = recalculoSaldoService.recalcularCuentaBancariaHasta(cuenta, fechaHasta);
            if (cuenta.getTipo() == CuentaBancaria.TipoCuenta.CAJA_FISICA) {
                caja = caja.add(saldo);
            } else {
                banco = banco.add(saldo);
            }
        }
        if (excluidas > 0) {
            alertas.add(excluidas + " cuenta(s) bancaria(s) en otra moneda excluida(s) del saldo de caja/banco (sin TC histórico para convertir)");
        }
        return new BigDecimal[] {caja, banco};
    }

    /**
     * Margen estimado (F7.4, emparejamiento cuota↔factura): por proyecto activo con presupuesto y al
     * menos un pago emparejado, deriva el TC efectivo de la porción ya facturada y lo aplica al margen
     * deseado en USD. Egresos proyectados: Σ (pagosPrevistos + costosEstimados) de etapas no canceladas
     * de proyectos activos — foto del portafolio, no acotada al período.
     */
    private BigDecimal[] margenEstimadoYEgresosProyectados() {
        BigDecimal margenEstimado = BigDecimal.ZERO;
        BigDecimal egresosProyectados = BigDecimal.ZERO;
        for (Proyecto proyecto : proyectoRepo.findByActivoTrueOrderByNombreAsc()) {
            for (Etapa etapa : etapaRepo.findByProyectoIdOrderByFechaInicioAsc(proyecto.getId())) {
                if (etapa.getEstado() != Etapa.EstadoEtapa.CANCELADA) {
                    egresosProyectados = egresosProyectados
                            .add(nvl(etapa.getPagosPrevistos()))
                            .add(nvl(etapa.getCostosEstimados()));
                }
            }
            if (presupuestoProyectoRepo.findByProyectoId(proyecto.getId()).isEmpty()) {
                continue;
            }
            ReporteRentabilidadProyectoResponse reporte = reporteRentabilidadService.obtener(proyecto.getId());
            var presupuesto = reporte.presupuesto();
            if (presupuesto == null || presupuesto.pagosEmparejadosConFactura() == 0 || presupuesto.cantidadPagosPactados() == 0) {
                continue;
            }
            BigDecimal precioFinalCliente = presupuesto.calculado().precioFinalCliente();
            if (precioFinalCliente == null || precioFinalCliente.signum() == 0) {
                continue;
            }
            BigDecimal montoEmparejadoUsd = precioFinalCliente
                    .divide(BigDecimal.valueOf(presupuesto.cantidadPagosPactados()), 10, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(presupuesto.pagosEmparejadosConFactura()));
            if (montoEmparejadoUsd.signum() == 0) {
                continue;
            }
            BigDecimal tcEfectivo = presupuesto.presupuestoConvertidoArs().divide(montoEmparejadoUsd, 10, RoundingMode.HALF_UP);
            margenEstimado = margenEstimado.add(presupuesto.calculado().margenDeseadoUsd().multiply(tcEfectivo));
        }
        return new BigDecimal[] {margenEstimado, egresosProyectados};
    }

    private BigDecimal nvl(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private VencimientoImpuesto vencimientoIva(int anio, int mes, int diaVencimiento) {
        LocalDate vencimiento = LocalDate.of(anio, mes, 1).plusMonths(1).withDayOfMonth(diaVencimiento);
        BigDecimal saldo = liquidacionIvaRepo.findByAnioAndMesAndEstadoIn(anio, mes, List.of(EstadoDocumento.BORRADOR, EstadoDocumento.CONFIRMADO))
                .stream().findFirst().map(l -> l.getSaldoAPagar()).orElse(BigDecimal.ZERO);
        return new VencimientoImpuesto(vencimiento, saldo, "/impuestos/iva");
    }

    private VencimientoImpuesto vencimientoIibb(int anio, int mes, int diaVencimiento) {
        LocalDate vencimiento = LocalDate.of(anio, mes, 1).plusMonths(1).withDayOfMonth(diaVencimiento);
        BigDecimal saldo = liquidacionIibbRepo.findByAnioAndMesAndEstadoIn(anio, mes, List.of(EstadoDocumento.BORRADOR, EstadoDocumento.CONFIRMADO))
                .stream().findFirst().map(l -> l.getSaldoAPagarTotal()).orElse(BigDecimal.ZERO);
        return new VencimientoImpuesto(vencimiento, saldo, "/impuestos/iibb");
    }
}

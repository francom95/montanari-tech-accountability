package com.montanaritech.contable.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.reporte.EstadoVencimiento;
import com.montanaritech.contable.common.saldo.RecalculoSaldoService;
import com.montanaritech.contable.contabilidad.estadoresultados.EstadoResultadosService;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.EstadoResultadosDtos.EstadoResultadosCalculado;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.EstadoResultadosDtos.EstadoResultadosResponse;
import com.montanaritech.contable.facturacion.TipoComprobante;
import com.montanaritech.contable.facturacion.cobro.CobroRepository;
import com.montanaritech.contable.facturacion.cuentasporcobrar.CuentaPorCobrarService;
import com.montanaritech.contable.facturacion.cuentasporcobrar.dto.CuentaPorCobrarResponse;
import com.montanaritech.contable.facturacion.cuentasporpagar.CuentaPorPagarService;
import com.montanaritech.contable.facturacion.cuentasporpagar.dto.CuentaPorPagarFilaResponse;
import com.montanaritech.contable.facturacion.cuentasporpagar.dto.CuentaPorPagarResponse;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaRepository;
import com.montanaritech.contable.impuestos.iibb.LiquidacionIibb;
import com.montanaritech.contable.impuestos.iibb.LiquidacionIibbRepository;
import com.montanaritech.contable.impuestos.iva.LiquidacionIva;
import com.montanaritech.contable.impuestos.iva.LiquidacionIvaRepository;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.etapa.Etapa;
import com.montanaritech.contable.maestros.proyecto.etapa.EtapaRepository;
import com.montanaritech.contable.maestros.proyecto.presupuesto.PresupuestoCalculado;
import com.montanaritech.contable.maestros.proyecto.presupuesto.PresupuestoProyecto;
import com.montanaritech.contable.maestros.proyecto.presupuesto.PresupuestoProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.rentabilidad.ReporteRentabilidadProyectoService;
import com.montanaritech.contable.maestros.proyecto.rentabilidad.dto.ReporteRentabilidadProyectoResponse;
import com.montanaritech.contable.maestros.proyecto.rentabilidad.dto.ReporteRentabilidadProyectoResponse.PresupuestoComparacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private ConfiguracionDashboardRepository configuracionRepo;
    @Mock private EstadoResultadosService estadoResultadosService;
    @Mock private FacturaVentaRepository facturaVentaRepo;
    @Mock private CobroRepository cobroRepo;
    @Mock private CuentaPorCobrarService cuentaPorCobrarService;
    @Mock private CuentaPorPagarService cuentaPorPagarService;
    @Mock private CuentaBancariaRepository cuentaBancariaRepo;
    @Mock private RecalculoSaldoService recalculoSaldoService;
    @Mock private ProyectoRepository proyectoRepo;
    @Mock private PresupuestoProyectoRepository presupuestoProyectoRepo;
    @Mock private EtapaRepository etapaRepo;
    @Mock private ReporteRentabilidadProyectoService reporteRentabilidadService;
    @Mock private LiquidacionIvaRepository liquidacionIvaRepo;
    @Mock private LiquidacionIibbRepository liquidacionIibbRepo;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(configuracionRepo, estadoResultadosService, facturaVentaRepo, cobroRepo,
                cuentaPorCobrarService, cuentaPorPagarService, cuentaBancariaRepo, recalculoSaldoService, proyectoRepo,
                presupuestoProyectoRepo, etapaRepo, reporteRentabilidadService, liquidacionIvaRepo, liquidacionIibbRepo);

        lenient().when(configuracionRepo.findFirstByOrderByIdAsc()).thenReturn(Optional.of(configuracionPorDefecto()));
        lenient().when(estadoResultadosService.porMes(anyInt(), anyInt()))
                .thenReturn(new EstadoResultadosResponse(
                        new EstadoResultadosCalculado(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000.00"),
                                BigDecimal.ZERO, List.of(), true),
                        null));
        lenient().when(facturaVentaRepo.buscarConfirmadasParaReporte(any(), any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(cobroRepo.sumarTotalArsConfirmadoEnPeriodo(any(), any())).thenReturn(BigDecimal.ZERO);
        lenient().when(cuentaPorCobrarService.calcular(any(), any(), any(), any(), any(), any()))
                .thenReturn(new CuentaPorCobrarResponse(List.of(), List.of()));
        lenient().when(cuentaPorPagarService.calcular(any(), any(), any(), any(), any(), any()))
                .thenReturn(new CuentaPorPagarResponse(List.of(), List.of()));
        lenient().when(cuentaBancariaRepo.findByActivoTrue()).thenReturn(List.of());
        lenient().when(proyectoRepo.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of());
        lenient().when(liquidacionIvaRepo.findByAnioAndMesAndEstadoIn(anyInt(), anyInt(), any())).thenReturn(List.of());
        lenient().when(liquidacionIibbRepo.findByAnioAndMesAndEstadoIn(anyInt(), anyInt(), any())).thenReturn(List.of());
    }

    private ConfiguracionDashboard configuracionPorDefecto() {
        ConfiguracionDashboard c = new ConfiguracionDashboard();
        c.setDiaVencimientoIva(20);
        c.setDiaVencimientoIibb(15);
        c.setVentanaObligacionesDias(15);
        return c;
    }

    private FacturaVenta factura(TipoComprobante tipo, BigDecimal totalArs) {
        FacturaVenta f = new FacturaVenta();
        f.setTipoComprobante(tipo);
        f.setTotalArs(totalArs);
        return f;
    }

    private CuentaBancaria cuenta(CuentaBancaria.TipoCuenta tipo) {
        Moneda ars = new Moneda();
        ars.setCodigo("ARS");
        CuentaBancaria c = new CuentaBancaria();
        c.setId(1L);
        c.setTipo(tipo);
        c.setMoneda(ars);
        return c;
    }

    @Test
    void obtenerCombinaLosIndicadoresConCeroCuandoNoHayDatos() {
        var resultado = service.obtener(2026, 6);

        assertThat(resultado.anio()).isEqualTo(2026);
        assertThat(resultado.mes()).isEqualTo(6);
        assertThat(resultado.resultadoMensual().valorArs()).isEqualByComparingTo("1000.00");
        assertThat(resultado.resultadoMensual().ruta()).isEqualTo("/reportes/estado-resultados");
        assertThat(resultado.ventasDelPeriodo().valorArs()).isEqualByComparingTo("0");
        assertThat(resultado.cobrosDelPeriodo().valorArs()).isEqualByComparingTo("0");
        assertThat(resultado.cuentasPorCobrar().valorArs()).isEqualByComparingTo("0");
        assertThat(resultado.cuentasPorPagar().valorArs()).isEqualByComparingTo("0");
        assertThat(resultado.obligacionesProximas().valorArs()).isEqualByComparingTo("0");
        assertThat(resultado.saldoCaja().valorArs()).isEqualByComparingTo("0");
        assertThat(resultado.saldoBanco().valorArs()).isEqualByComparingTo("0");
        assertThat(resultado.margenEstimado().valorArs()).isEqualByComparingTo("0");
        assertThat(resultado.egresosProyectados().valorArs()).isEqualByComparingTo("0");
        assertThat(resultado.proximoVencimientoIva().fechaVencimiento()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(resultado.proximoVencimientoIva().saldoAPagarArs()).isEqualByComparingTo("0");
        assertThat(resultado.proximoVencimientoIibb().fechaVencimiento()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(resultado.alertas()).isEmpty();
    }

    @Test
    void ventasDelPeriodoRestaLasNotasDeCredito() {
        when(facturaVentaRepo.buscarConfirmadasParaReporte(eq(null), eq(null), eq(null), any(), any()))
                .thenReturn(List.of(
                        factura(TipoComprobante.FACTURA_A, new BigDecimal("1000.00")),
                        factura(TipoComprobante.NOTA_CREDITO_A, new BigDecimal("300.00"))));

        var resultado = service.obtener(2026, 6);

        assertThat(resultado.ventasDelPeriodo().valorArs()).isEqualByComparingTo("700.00");
    }

    @Test
    void obligacionesProximasSoloSumaLasDentroDeLaVentana() {
        LocalDate fin = LocalDate.of(2026, 6, 30);
        CuentaPorPagarFilaResponse dentro = new CuentaPorPagarFilaResponse(1L, 1L, "Prov A", null, null, "F1",
                LocalDate.of(2026, 6, 1), fin.plusDays(5), 1L, "ARS", new BigDecimal("100"), new BigDecimal("100"),
                new BigDecimal("100"), new BigDecimal("100"), "POR_VENCER");
        CuentaPorPagarFilaResponse fuera = new CuentaPorPagarFilaResponse(2L, 1L, "Prov A", null, null, "F2",
                LocalDate.of(2026, 6, 1), fin.plusDays(60), 1L, "ARS", new BigDecimal("500"), new BigDecimal("500"),
                new BigDecimal("500"), new BigDecimal("500"), "POR_VENCER");
        when(cuentaPorPagarService.calcular(eq(null), eq(null), eq(null), eq(null), eq(fin), eq(EstadoVencimiento.POR_VENCER)))
                .thenReturn(new CuentaPorPagarResponse(List.of(dentro, fuera), List.of()));

        var resultado = service.obtener(2026, 6);

        assertThat(resultado.obligacionesProximas().valorArs()).isEqualByComparingTo("100");
    }

    @Test
    void saldoCajaYBancoSeparaPorTipoDeCuenta() {
        CuentaBancaria caja = cuenta(CuentaBancaria.TipoCuenta.CAJA_FISICA);
        CuentaBancaria banco = cuenta(CuentaBancaria.TipoCuenta.CUENTA_CORRIENTE);
        when(cuentaBancariaRepo.findByActivoTrue()).thenReturn(List.of(caja, banco));
        when(recalculoSaldoService.recalcularCuentaBancariaHasta(eq(caja), any())).thenReturn(new BigDecimal("500.00"));
        when(recalculoSaldoService.recalcularCuentaBancariaHasta(eq(banco), any())).thenReturn(new BigDecimal("3000.00"));

        var resultado = service.obtener(2026, 6);

        assertThat(resultado.saldoCaja().valorArs()).isEqualByComparingTo("500.00");
        assertThat(resultado.saldoBanco().valorArs()).isEqualByComparingTo("3000.00");
    }

    @Test
    void saldoExcluyeCuentasEnOtraMonedaYLoAvisaEnAlertas() {
        CuentaBancaria banco = cuenta(CuentaBancaria.TipoCuenta.CUENTA_CORRIENTE);
        Moneda usd = new Moneda();
        usd.setCodigo("USD");
        CuentaBancaria cuentaUsd = cuenta(CuentaBancaria.TipoCuenta.CUENTA_CORRIENTE);
        cuentaUsd.setMoneda(usd);
        when(cuentaBancariaRepo.findByActivoTrue()).thenReturn(List.of(banco, cuentaUsd));
        when(recalculoSaldoService.recalcularCuentaBancariaHasta(eq(banco), any())).thenReturn(new BigDecimal("1000.00"));

        var resultado = service.obtener(2026, 6);

        assertThat(resultado.saldoBanco().valorArs()).isEqualByComparingTo("1000.00");
        assertThat(resultado.alertas()).hasSize(1);
        assertThat(resultado.alertas().get(0)).contains("1 cuenta");
    }

    private Etapa etapa(Etapa.EstadoEtapa estado, BigDecimal pagosPrevistos, BigDecimal costosEstimados) {
        Etapa e = new Etapa();
        e.setEstado(estado);
        e.setPagosPrevistos(pagosPrevistos);
        e.setCostosEstimados(costosEstimados);
        return e;
    }

    private Proyecto proyecto(Long id) {
        Proyecto p = new Proyecto();
        p.setId(id);
        return p;
    }

    @Test
    void egresosProyectadosSumaEtapasNoCanceladasDeTodosLosProyectosActivos() {
        Proyecto p1 = proyecto(1L);
        when(proyectoRepo.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(p1));
        when(etapaRepo.findByProyectoIdOrderByFechaInicioAsc(1L)).thenReturn(List.of(
                etapa(Etapa.EstadoEtapa.PENDIENTE, new BigDecimal("100.00"), new BigDecimal("50.00")),
                etapa(Etapa.EstadoEtapa.CANCELADA, new BigDecimal("9999.00"), new BigDecimal("9999.00"))));
        when(presupuestoProyectoRepo.findByProyectoId(1L)).thenReturn(Optional.empty());

        var resultado = service.obtener(2026, 6);

        assertThat(resultado.egresosProyectados().valorArs()).isEqualByComparingTo("150.00");
        assertThat(resultado.margenEstimado().valorArs()).isEqualByComparingTo("0");
    }

    @Test
    void margenEstimadoExcluyeProyectoSinPagosEmparejadosYSumaElQueSiTiene() {
        Proyecto sinPresupuesto = proyecto(1L);
        Proyecto sinEmparejar = proyecto(2L);
        Proyecto conMargen = proyecto(3L);
        when(proyectoRepo.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(sinPresupuesto, sinEmparejar, conMargen));
        when(etapaRepo.findByProyectoIdOrderByFechaInicioAsc(any())).thenReturn(List.of());

        when(presupuestoProyectoRepo.findByProyectoId(1L)).thenReturn(Optional.empty());
        when(presupuestoProyectoRepo.findByProyectoId(2L)).thenReturn(Optional.of(new PresupuestoProyecto()));
        when(presupuestoProyectoRepo.findByProyectoId(3L)).thenReturn(Optional.of(new PresupuestoProyecto()));

        when(reporteRentabilidadService.obtener(2L)).thenReturn(reporteSinEmparejar());
        when(reporteRentabilidadService.obtener(3L)).thenReturn(reporteConMargen(
                new BigDecimal("500.00"), new BigDecimal("2000.00"), 4, 2, new BigDecimal("2200.00")));

        var resultado = service.obtener(2026, 6);

        // TC efectivo = 2200 / (2000/4 * 2) = 2200/1000 = 2.2; margen = 500 * 2.2 = 1100.00
        assertThat(resultado.margenEstimado().valorArs()).isEqualByComparingTo("1100.00");
    }

    private ReporteRentabilidadProyectoResponse reporteSinEmparejar() {
        PresupuestoComparacion presupuesto = new PresupuestoComparacion(
                presupuestoCalculado(BigDecimal.ZERO, BigDecimal.ZERO), 3, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        return reporteConPresupuesto(presupuesto);
    }

    private ReporteRentabilidadProyectoResponse reporteConMargen(BigDecimal margenDeseadoUsd, BigDecimal precioFinalCliente,
            int cantidadPagosPactados, int pagosEmparejados, BigDecimal presupuestoConvertidoArs) {
        PresupuestoComparacion presupuesto = new PresupuestoComparacion(
                presupuestoCalculado(margenDeseadoUsd, precioFinalCliente), cantidadPagosPactados, pagosEmparejados,
                presupuestoConvertidoArs, BigDecimal.ZERO, BigDecimal.ZERO);
        return reporteConPresupuesto(presupuesto);
    }

    private PresupuestoCalculado presupuestoCalculado(BigDecimal margenDeseadoUsd, BigDecimal precioFinalCliente) {
        return new PresupuestoCalculado(null, BigDecimal.ZERO, margenDeseadoUsd, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                precioFinalCliente);
    }

    private ReporteRentabilidadProyectoResponse reporteConPresupuesto(PresupuestoComparacion presupuesto) {
        return new ReporteRentabilidadProyectoResponse(1L, "Proyecto", "Cliente", "ARGENTINA", "ACTIVO", null, null,
                List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, List.of(), List.of(), List.of(), BigDecimal.ZERO, BigDecimal.ZERO, presupuesto,
                BigDecimal.ZERO, List.of());
    }

    @Test
    void vencimientoIvaUsaElSaldoAPagarDeLaLiquidacionDelPeriodo() {
        LiquidacionIva liquidacion = new LiquidacionIva();
        liquidacion.setSaldoAPagar(new BigDecimal("42000.00"));
        when(liquidacionIvaRepo.findByAnioAndMesAndEstadoIn(2026, 6,
                List.of(EstadoDocumento.BORRADOR, EstadoDocumento.CONFIRMADO))).thenReturn(List.of(liquidacion));

        var resultado = service.obtener(2026, 6);

        assertThat(resultado.proximoVencimientoIva().saldoAPagarArs()).isEqualByComparingTo("42000.00");
        assertThat(resultado.proximoVencimientoIva().fechaVencimiento()).isEqualTo(LocalDate.of(2026, 7, 20));
    }

    @Test
    void vencimientoIibbUsaElSaldoAPagarTotalDeLaLiquidacionDelPeriodo() {
        LiquidacionIibb liquidacion = new LiquidacionIibb();
        liquidacion.setSaldoAPagarTotal(new BigDecimal("15000.00"));
        when(liquidacionIibbRepo.findByAnioAndMesAndEstadoIn(2026, 6,
                List.of(EstadoDocumento.BORRADOR, EstadoDocumento.CONFIRMADO))).thenReturn(List.of(liquidacion));

        var resultado = service.obtener(2026, 6);

        assertThat(resultado.proximoVencimientoIibb().saldoAPagarArs()).isEqualByComparingTo("15000.00");
        assertThat(resultado.proximoVencimientoIibb().fechaVencimiento()).isEqualTo(LocalDate.of(2026, 7, 15));
    }
}

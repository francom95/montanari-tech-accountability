package com.montanaritech.contable.flujocaja;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.bancos.movimientobancario.EstadoMovimientoBancario;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancario;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancarioRepository;
import com.montanaritech.contable.bancos.tarjetacredito.ConsumoTarjetaRepository;
import com.montanaritech.contable.bancos.tarjetacredito.PagoTarjetaRepository;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.saldo.RecalculoSaldoService;
import com.montanaritech.contable.compromiso.Compromiso;
import com.montanaritech.contable.compromiso.CompromisoService;
import com.montanaritech.contable.compromiso.EstadoCompromiso;
import com.montanaritech.contable.facturacion.cobro.Cobro;
import com.montanaritech.contable.facturacion.cobro.CobroImputacion;
import com.montanaritech.contable.facturacion.cobro.CobroImputacionRepository;
import com.montanaritech.contable.facturacion.cuentasporpagar.CuentaPorPagarService;
import com.montanaritech.contable.facturacion.cuentasporpagar.dto.CuentaPorPagarFilaResponse;
import com.montanaritech.contable.facturacion.cuentasporpagar.dto.CuentaPorPagarResponse;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaRepository;
import com.montanaritech.contable.flujocaja.dto.FlujoCajaResponse;
import com.montanaritech.contable.flujocaja.dto.PuntoFlujoCaja;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoCuota;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCreditoRepository;
import com.montanaritech.contable.maestros.tipocambio.TipoCambio;
import com.montanaritech.contable.maestros.tipocambio.TipoCambioRepository;
import com.montanaritech.contable.vencimientos.EstadoVencimientoObligacion;
import com.montanaritech.contable.vencimientos.TipoVencimiento;
import com.montanaritech.contable.vencimientos.Vencimiento;
import com.montanaritech.contable.vencimientos.VencimientoService;
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
class FlujoCajaServiceTest {

    @Mock private CuentaBancariaRepository cuentaBancariaRepo;
    @Mock private TarjetaCreditoRepository tarjetaCreditoRepo;
    @Mock private MovimientoBancarioRepository movimientoBancarioRepo;
    @Mock private ConsumoTarjetaRepository consumoTarjetaRepo;
    @Mock private PagoTarjetaRepository pagoTarjetaRepo;
    @Mock private RecalculoSaldoService recalculoSaldoService;
    @Mock private TipoCambioRepository tipoCambioRepo;
    @Mock private CompromisoService compromisoService;
    @Mock private VencimientoService vencimientoService;
    @Mock private CuentaPorPagarService cuentaPorPagarService;
    @Mock private ProyectoRepository proyectoRepo;
    @Mock private FacturaVentaRepository facturaVentaRepo;
    @Mock private CobroImputacionRepository cobroImputacionRepo;

    private FlujoCajaService service;
    private Moneda ars;
    private Moneda usd;
    private CuentaBancaria cuenta;

    @BeforeEach
    void setUp() {
        service = new FlujoCajaService(cuentaBancariaRepo, tarjetaCreditoRepo, movimientoBancarioRepo,
                consumoTarjetaRepo, pagoTarjetaRepo, recalculoSaldoService, tipoCambioRepo, compromisoService,
                vencimientoService, cuentaPorPagarService, proyectoRepo, facturaVentaRepo, cobroImputacionRepo);

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");
        usd = new Moneda();
        usd.setId(2L);
        usd.setCodigo("USD");

        cuenta = new CuentaBancaria();
        cuenta.setId(10L);
        cuenta.setAlias("Banco Galicia CC");
        cuenta.setMoneda(ars);

        lenient().when(tarjetaCreditoRepo.findByActivoTrue()).thenReturn(List.of());
        lenient().when(compromisoService.porRangoDeFechas(any(), any())).thenReturn(List.of());
        lenient().when(vencimientoService.porRangoDeFechas(any(), any())).thenReturn(List.of());
        lenient().when(cuentaPorPagarService.calcular(any(), any(), any(), any(), any(), any()))
                .thenReturn(new CuentaPorPagarResponse(List.of(), List.of()));
        lenient().when(proyectoRepo.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of());
    }

    private MovimientoBancario movimiento(LocalDate fecha, BigDecimal importe, EstadoMovimientoBancario estado) {
        MovimientoBancario m = new MovimientoBancario();
        m.setFecha(fecha);
        m.setImporte(importe);
        m.setImporteArs(importe);
        m.setEstado(estado);
        return m;
    }

    // ---- flujoReal: bucketing ----

    @Test
    void bucketingDiarioAcumulaSaldoCorridoYDetectaSaldoNegativo() {
        when(cuentaBancariaRepo.findByActivoTrue()).thenReturn(List.of(cuenta));
        when(recalculoSaldoService.recalcularCuentaBancariaHasta(eq(cuenta), eq(LocalDate.of(2026, 7, 31))))
                .thenReturn(new BigDecimal("100.00"));
        when(movimientoBancarioRepo.buscarParaConciliacion(10L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3)))
                .thenReturn(List.of(
                        movimiento(LocalDate.of(2026, 8, 1), new BigDecimal("50.00"), EstadoMovimientoBancario.CONCILIADO),
                        movimiento(LocalDate.of(2026, 8, 2), new BigDecimal("-300.00"), EstadoMovimientoBancario.CONCILIADO),
                        movimiento(LocalDate.of(2026, 8, 2), new BigDecimal("-999.00"), EstadoMovimientoBancario.DESCARTADO)));

        FlujoCajaResponse respuesta = service.flujoReal(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3), Granularidad.DIARIO);

        List<PuntoFlujoCaja> serie = respuesta.porCuenta().get("Banco Galicia CC");
        assertThat(serie).hasSize(3);
        assertThat(serie.get(0).saldoInicial()).isEqualByComparingTo("100.00");
        assertThat(serie.get(0).ingresos()).isEqualByComparingTo("50.00");
        assertThat(serie.get(0).saldoFinal()).isEqualByComparingTo("150.00");
        assertThat(serie.get(0).saldoNegativo()).isFalse();
        assertThat(serie.get(1).saldoInicial()).isEqualByComparingTo("150.00");
        assertThat(serie.get(1).egresos()).isEqualByComparingTo("300.00");
        assertThat(serie.get(1).saldoFinal()).isEqualByComparingTo("-150.00");
        assertThat(serie.get(1).saldoNegativo()).isTrue();
        assertThat(serie.get(2).saldoInicial()).isEqualByComparingTo("-150.00");
        assertThat(serie.get(2).ingresos()).isEqualByComparingTo("0");
        assertThat(serie.get(2).saldoFinal()).isEqualByComparingTo("-150.00");
    }

    @Test
    void bucketingMensualAgrupaMovimientosDelMismoMes() {
        when(cuentaBancariaRepo.findByActivoTrue()).thenReturn(List.of(cuenta));
        when(recalculoSaldoService.recalcularCuentaBancariaHasta(eq(cuenta), eq(LocalDate.of(2026, 7, 31))))
                .thenReturn(BigDecimal.ZERO);
        when(movimientoBancarioRepo.buscarParaConciliacion(10L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(
                        movimiento(LocalDate.of(2026, 8, 5), new BigDecimal("100.00"), EstadoMovimientoBancario.CONCILIADO),
                        movimiento(LocalDate.of(2026, 8, 20), new BigDecimal("200.00"), EstadoMovimientoBancario.PENDIENTE)));

        FlujoCajaResponse respuesta = service.flujoReal(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), Granularidad.MENSUAL);

        List<PuntoFlujoCaja> serie = respuesta.porCuenta().get("Banco Galicia CC");
        assertThat(serie).hasSize(1);
        assertThat(serie.get(0).fecha()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(serie.get(0).ingresos()).isEqualByComparingTo("300.00");
    }

    @Test
    void cuentaEnMonedaSinCotizacionSeExcluyeDelConsolidadoConAdvertencia() {
        CuentaBancaria cuentaUsd = new CuentaBancaria();
        cuentaUsd.setId(20L);
        cuentaUsd.setAlias("Cuenta USD");
        cuentaUsd.setMoneda(usd);
        when(cuentaBancariaRepo.findByActivoTrue()).thenReturn(List.of(cuentaUsd));
        when(recalculoSaldoService.recalcularCuentaBancariaHasta(eq(cuentaUsd), any())).thenReturn(new BigDecimal("500.00"));
        when(movimientoBancarioRepo.buscarParaConciliacion(eq(20L), any(), any())).thenReturn(List.of());
        when(tipoCambioRepo.findFirstByMonedaIdAndActivoTrueOrderByFechaDesc(2L)).thenReturn(Optional.empty());

        FlujoCajaResponse respuesta = service.flujoReal(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3), Granularidad.DIARIO);

        assertThat(respuesta.consolidado()).isEmpty();
        assertThat(respuesta.porCuenta()).containsKey("Cuenta USD");
        assertThat(respuesta.advertencias()).anyMatch(a -> a.contains("USD"));
    }

    // ---- flujoProyectado ----

    @Test
    void flujoProyectadoSumaCompromisoYVencimientoComoEgresos() {
        when(cuentaBancariaRepo.findByActivoTrue()).thenReturn(List.of(cuenta));
        when(recalculoSaldoService.recalcularCuentaBancariaHasta(eq(cuenta), any())).thenReturn(new BigDecimal("1000.00"));

        Compromiso compromiso = new Compromiso();
        compromiso.setEstado(EstadoCompromiso.PENDIENTE);
        compromiso.setActivo(true);
        compromiso.setFechaPrevista(LocalDate.now().plusDays(2));
        compromiso.setImporte(new BigDecimal("300.00"));
        compromiso.setMoneda(ars);
        when(compromisoService.porRangoDeFechas(any(), any())).thenReturn(List.of(compromiso));

        Vencimiento vencimiento = new Vencimiento();
        vencimiento.setFecha(LocalDate.now().plusDays(5));
        vencimiento.setImporteEstimado(new BigDecimal("200.00"));
        vencimiento.setMoneda(ars);
        vencimiento.setEstado(EstadoVencimientoObligacion.PENDIENTE);
        vencimiento.setTipo(TipoVencimiento.OTRO);
        when(vencimientoService.porRangoDeFechas(any(), any())).thenReturn(List.of(vencimiento));

        FlujoCajaResponse respuesta = service.flujoProyectado(10);

        BigDecimal totalEgresos = respuesta.consolidado().stream().map(PuntoFlujoCaja::egresos).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalEgresos).isEqualByComparingTo("500.00");
        BigDecimal saldoFinalUltimoDia = respuesta.consolidado().get(respuesta.consolidado().size() - 1).saldoFinal();
        assertThat(saldoFinalUltimoDia).isEqualByComparingTo("500.00");
        assertThat(respuesta.consolidado()).noneMatch(PuntoFlujoCaja::saldoNegativo);
    }

    @Test
    void flujoProyectadoDetectaSaldoNegativoCuandoElEgresoSuperaElSaldo() {
        when(cuentaBancariaRepo.findByActivoTrue()).thenReturn(List.of(cuenta));
        when(recalculoSaldoService.recalcularCuentaBancariaHasta(eq(cuenta), any())).thenReturn(new BigDecimal("100.00"));

        Compromiso compromiso = new Compromiso();
        compromiso.setEstado(EstadoCompromiso.PENDIENTE);
        compromiso.setActivo(true);
        compromiso.setFechaPrevista(LocalDate.now().plusDays(1));
        compromiso.setImporte(new BigDecimal("500.00"));
        compromiso.setMoneda(ars);
        when(compromisoService.porRangoDeFechas(any(), any())).thenReturn(List.of(compromiso));

        FlujoCajaResponse respuesta = service.flujoProyectado(5);

        assertThat(respuesta.consolidado()).anyMatch(PuntoFlujoCaja::saldoNegativo);
    }

    // ---- cuotas pendientes (emparejamiento cuota-cobro) ----

    private Proyecto proyectoConCuotas(int cantidadCuotas) {
        Proyecto p = new Proyecto();
        p.setId(1L);
        p.setMoneda(ars);
        for (int i = 1; i <= cantidadCuotas; i++) {
            ProyectoCuota c = new ProyectoCuota();
            c.setNumero(i);
            c.setFechaEstimadaCobro(LocalDate.now().plusDays(i));
            c.setImporte(new BigDecimal("1000.00"));
            p.getCuotas().add(c);
        }
        return p;
    }

    @Test
    void sinCobrosConfirmadosTodasLasCuotasQuedanPendientes() {
        Proyecto proyecto = proyectoConCuotas(3);
        when(proyectoRepo.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(proyecto));
        FacturaVenta factura = new FacturaVenta();
        factura.setId(100L);
        when(facturaVentaRepo.buscarConfirmadasParaReporte(null, 1L, null, null, null)).thenReturn(List.of(factura));
        when(cobroImputacionRepo.findByFacturaVenta_IdAndCobro_EstadoOrderByIdAsc(100L, EstadoDocumento.CONFIRMADO))
                .thenReturn(List.of());
        when(cuentaBancariaRepo.findByActivoTrue()).thenReturn(List.of());

        FlujoCajaResponse respuesta = service.flujoProyectado(10);

        BigDecimal totalIngresos = respuesta.consolidado().stream().map(PuntoFlujoCaja::ingresos).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalIngresos).isEqualByComparingTo("3000.00");
    }

    @Test
    void cuotasYaCobradasQuedanExcluidasDeLaProyeccion() {
        Proyecto proyecto = proyectoConCuotas(3);
        when(proyectoRepo.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(proyecto));
        FacturaVenta factura = new FacturaVenta();
        factura.setId(100L);
        when(facturaVentaRepo.buscarConfirmadasParaReporte(null, 1L, null, null, null)).thenReturn(List.of(factura));

        Cobro cobro1 = new Cobro();
        cobro1.setId(500L);
        CobroImputacion ci1 = new CobroImputacion();
        ci1.setCobro(cobro1);
        when(cobroImputacionRepo.findByFacturaVenta_IdAndCobro_EstadoOrderByIdAsc(100L, EstadoDocumento.CONFIRMADO))
                .thenReturn(List.of(ci1));
        when(cuentaBancariaRepo.findByActivoTrue()).thenReturn(List.of());

        FlujoCajaResponse respuesta = service.flujoProyectado(10);

        // 1 de 3 cuotas ya cobrada -> quedan 2 pendientes de 1000 c/u
        BigDecimal totalIngresos = respuesta.consolidado().stream().map(PuntoFlujoCaja::ingresos).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalIngresos).isEqualByComparingTo("2000.00");
    }

    @Test
    void todasLasCuotasCobradasNoGeneraIngresoProyectado() {
        Proyecto proyecto = proyectoConCuotas(2);
        when(proyectoRepo.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(proyecto));
        FacturaVenta factura = new FacturaVenta();
        factura.setId(100L);
        when(facturaVentaRepo.buscarConfirmadasParaReporte(null, 1L, null, null, null)).thenReturn(List.of(factura));

        Cobro cobro1 = new Cobro();
        cobro1.setId(500L);
        Cobro cobro2 = new Cobro();
        cobro2.setId(501L);
        CobroImputacion ci1 = new CobroImputacion();
        ci1.setCobro(cobro1);
        CobroImputacion ci2 = new CobroImputacion();
        ci2.setCobro(cobro2);
        when(cobroImputacionRepo.findByFacturaVenta_IdAndCobro_EstadoOrderByIdAsc(100L, EstadoDocumento.CONFIRMADO))
                .thenReturn(List.of(ci1, ci2));
        when(cuentaBancariaRepo.findByActivoTrue()).thenReturn(List.of());

        FlujoCajaResponse respuesta = service.flujoProyectado(10);

        BigDecimal totalIngresos = respuesta.consolidado().stream().map(PuntoFlujoCaja::ingresos).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalIngresos).isEqualByComparingTo("0");
    }

    // ---- combinado ----

    @Test
    void combinadoConcatenaRealYProyectadoConFlagCorrecto() {
        when(cuentaBancariaRepo.findByActivoTrue()).thenReturn(List.of(cuenta));
        when(recalculoSaldoService.recalcularCuentaBancariaHasta(eq(cuenta), any())).thenReturn(new BigDecimal("100.00"));
        when(movimientoBancarioRepo.buscarParaConciliacion(anyLong(), any(), any())).thenReturn(List.of());

        FlujoCajaResponse respuesta = service.combinado(3, 3);

        // real = [hoy-3, hoy-1] (3 días) + proyectado = [hoy, hoy+3] (4 días, igual
        // criterio inclusivo que VencimientoService.proximos) = 7 puntos.
        assertThat(respuesta.consolidado()).hasSize(7);
        assertThat(respuesta.consolidado().subList(0, 3)).allMatch(PuntoFlujoCaja::esReal);
        assertThat(respuesta.consolidado().subList(3, 7)).noneMatch(PuntoFlujoCaja::esReal);
    }
}

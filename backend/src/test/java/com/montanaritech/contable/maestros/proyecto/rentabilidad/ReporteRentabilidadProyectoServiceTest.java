package com.montanaritech.contable.maestros.proyecto.rentabilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.facturacion.cobro.AplicacionAnticipoClienteRepository;
import com.montanaritech.contable.facturacion.cobro.CobroImputacionRepository;
import com.montanaritech.contable.facturacion.cobro.dto.ImputadoFacturaVenta;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompraRepository;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaRepository;
import com.montanaritech.contable.facturacion.pago.AplicacionAnticipoProveedorRepository;
import com.montanaritech.contable.facturacion.pago.PagoImputacionRepository;
import com.montanaritech.contable.impuestos.atribucion.AtribucionImpuestoLineaRepository;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.Proyecto.EstadoProyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoCuota;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.comision.ComisionProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.etapa.EtapaRepository;
import com.montanaritech.contable.maestros.proyecto.presupuesto.PresupuestoCalculado;
import com.montanaritech.contable.maestros.proyecto.presupuesto.PresupuestoProyecto;
import com.montanaritech.contable.maestros.proyecto.presupuesto.PresupuestoProyectoService;
import com.montanaritech.contable.maestros.proyecto.rentabilidad.dto.ReporteRentabilidadProyectoResponse;
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
class ReporteRentabilidadProyectoServiceTest {

    @Mock private ProyectoRepository proyectoRepo;
    @Mock private EtapaRepository etapaRepo;
    @Mock private FacturaVentaRepository facturaVentaRepo;
    @Mock private FacturaCompraRepository facturaCompraRepo;
    @Mock private CobroImputacionRepository cobroImputacionRepo;
    @Mock private AplicacionAnticipoClienteRepository aplicacionAnticipoClienteRepo;
    @Mock private PagoImputacionRepository pagoImputacionRepo;
    @Mock private AplicacionAnticipoProveedorRepository aplicacionAnticipoProveedorRepo;
    @Mock private ComisionProyectoRepository comisionProyectoRepo;
    @Mock private AtribucionImpuestoLineaRepository atribucionImpuestoLineaRepo;
    @Mock private PresupuestoProyectoService presupuestoProyectoService;

    private ReporteRentabilidadProyectoService service;
    private Proyecto proyecto;
    private Cliente cliente;
    private Moneda usd;

    @BeforeEach
    void setUp() {
        service = new ReporteRentabilidadProyectoService(proyectoRepo, etapaRepo, facturaVentaRepo, facturaCompraRepo,
                cobroImputacionRepo, aplicacionAnticipoClienteRepo, pagoImputacionRepo, aplicacionAnticipoProveedorRepo,
                comisionProyectoRepo, atribucionImpuestoLineaRepo, presupuestoProyectoService);

        usd = new Moneda();
        usd.setId(2L);
        usd.setCodigo("USD");

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Cliente Test");

        proyecto = new Proyecto();
        proyecto.setId(1L);
        proyecto.setNombre("Proyecto Test");
        proyecto.setCliente(cliente);
        proyecto.setEstado(EstadoProyecto.EN_CURSO);
        proyecto.setTipoProyecto(Proyecto.TipoProyecto.ARGENTINA);

        when(proyectoRepo.findById(1L)).thenReturn(Optional.of(proyecto));
        lenient().when(etapaRepo.findByProyectoIdOrderByFechaInicioAsc(1L)).thenReturn(List.of());
        lenient().when(facturaVentaRepo.buscarConfirmadasParaReporte(any(), any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(facturaCompraRepo.buscarConfirmadasParaReporte(any(), any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(comisionProyectoRepo.findByProyectoIdAndActivoTrue(1L)).thenReturn(List.of());
        lenient().when(atribucionImpuestoLineaRepo.findByProyectoId(1L)).thenReturn(List.of());
        lenient().when(presupuestoProyectoService.obtener(1L)).thenReturn(Optional.empty());
    }

    private FacturaVenta facturaVenta(Long id, LocalDate fecha, BigDecimal tc, BigDecimal total, BigDecimal totalArs) {
        FacturaVenta f = new FacturaVenta();
        f.setId(id);
        f.setNumero("00001-" + id);
        f.setCliente(cliente);
        f.setMoneda(usd);
        f.setFecha(fecha);
        f.setTipoCambio(tc);
        f.setTotal(total);
        f.setTotalArs(totalArs);
        f.setEstado(EstadoDocumento.CONFIRMADO);
        return f;
    }

    private ProyectoCuota cuota(int numero, BigDecimal importe) {
        ProyectoCuota c = new ProyectoCuota();
        c.setProyecto(proyecto);
        c.setNumero(numero);
        c.setFechaEstimadaCobro(LocalDate.of(2026, numero, 1));
        c.setImporte(importe);
        return c;
    }

    @Test
    void sinPresupuestoNoRompeYAvisaEnAdvertencias() {
        ReporteRentabilidadProyectoResponse r = service.obtener(1L);

        assertThat(r.presupuesto()).isNull();
        assertThat(r.advertencias()).anyMatch(a -> a.contains("no tiene presupuesto"));
    }

    @Test
    void ingresosSumanFacturadoYCobradoCorrectamente() {
        FacturaVenta f = facturaVenta(10L, LocalDate.of(2026, 1, 10), new BigDecimal("1000.000000"),
                new BigDecimal("100.00"), new BigDecimal("100000.00"));
        when(facturaVentaRepo.buscarConfirmadasParaReporte(null, 1L, null, null, null)).thenReturn(List.of(f));
        when(cobroImputacionRepo.sumarImputadoPorFactura(List.of(10L), EstadoDocumento.CONFIRMADO))
                .thenReturn(List.of(new ImputadoFacturaVenta(10L, new BigDecimal("60.00"), new BigDecimal("60000.00"))));
        when(aplicacionAnticipoClienteRepo.sumarAplicacionesPorFactura(List.of(10L))).thenReturn(List.of());

        ReporteRentabilidadProyectoResponse r = service.obtener(1L);

        assertThat(r.totalFacturadoVentaArs()).isEqualByComparingTo("100000.00");
        assertThat(r.totalCobradoArs()).isEqualByComparingTo("60000.00");
        assertThat(r.pendienteCobroArs()).isEqualByComparingTo("40000.00");
    }

    @Test
    void emparejaCuotasConFacturasPorOrdenYConvierteSoloLaPorcionEmparejada() {
        proyecto.setCantidadPagosPactados(3);
        proyecto.getCuotas().add(cuota(1, new BigDecimal("1000.00")));
        proyecto.getCuotas().add(cuota(2, new BigDecimal("1000.00")));
        proyecto.getCuotas().add(cuota(3, new BigDecimal("1000.00")));

        // Solo 2 de los 3 pagos tienen factura real todavía.
        FacturaVenta f1 = facturaVenta(20L, LocalDate.of(2026, 1, 5), new BigDecimal("1000.000000"),
                new BigDecimal("1000.00"), new BigDecimal("1000000.00"));
        FacturaVenta f2 = facturaVenta(21L, LocalDate.of(2026, 2, 5), new BigDecimal("1100.000000"),
                new BigDecimal("1000.00"), new BigDecimal("1100000.00"));
        when(facturaVentaRepo.buscarConfirmadasParaReporte(null, 1L, null, null, null)).thenReturn(List.of(f2, f1));
        when(cobroImputacionRepo.sumarImputadoPorFactura(any(), any())).thenReturn(List.of());
        when(aplicacionAnticipoClienteRepo.sumarAplicacionesPorFactura(any())).thenReturn(List.of());

        PresupuestoProyecto presupuesto = new PresupuestoProyecto();
        presupuesto.setProyecto(proyecto);
        when(presupuestoProyectoService.obtener(1L)).thenReturn(Optional.of(presupuesto));
        PresupuestoCalculado calculado = new PresupuestoCalculado(Proyecto.TipoProyecto.ARGENTINA,
                new BigDecimal("2000.00"), new BigDecimal("1000.00"), new BigDecimal("428.5714285714"),
                new BigDecimal("3428.5714285714"),
                new BigDecimal("400.00"), new BigDecimal("4000.00"), new BigDecimal("200.00"),
                new BigDecimal("50.00"), new BigDecimal("800.00"), new BigDecimal("4800.00"),
                null, null, null, null, null, null,
                new BigDecimal("3000.00"));
        when(presupuestoProyectoService.calcular(presupuesto)).thenReturn(calculado);

        ReporteRentabilidadProyectoResponse r = service.obtener(1L);

        // montoPorPago = 3000/3 = 1000 USD; emparejado con f1 (tc 1000) y f2 (tc 1100), por fecha ascendente.
        assertThat(r.presupuesto()).isNotNull();
        assertThat(r.presupuesto().pagosEmparejadosConFactura()).isEqualTo(2);
        assertThat(r.presupuesto().cantidadPagosPactados()).isEqualTo(3);
        assertThat(r.presupuesto().presupuestoConvertidoArs()).isEqualByComparingTo("2100000.00");
        assertThat(r.presupuesto().facturadoEmparejadoArs()).isEqualByComparingTo("2100000.00");
        assertThat(r.presupuesto().diferenciaArs()).isEqualByComparingTo("0.00");
        assertThat(r.advertencias()).anyMatch(a -> a.contains("1 de 3 pagos"));
    }

    @Test
    void margenRealDescuentaPagadoComisionesEImpuestosAtribuidos() {
        FacturaVenta fv = facturaVenta(30L, LocalDate.of(2026, 1, 1), BigDecimal.ONE, new BigDecimal("1000.00"), new BigDecimal("1000.00"));
        when(facturaVentaRepo.buscarConfirmadasParaReporte(null, 1L, null, null, null)).thenReturn(List.of(fv));
        when(cobroImputacionRepo.sumarImputadoPorFactura(List.of(30L), EstadoDocumento.CONFIRMADO))
                .thenReturn(List.of(new ImputadoFacturaVenta(30L, new BigDecimal("1000.00"), new BigDecimal("1000.00"))));
        when(aplicacionAnticipoClienteRepo.sumarAplicacionesPorFactura(List.of(30L))).thenReturn(List.of());

        ReporteRentabilidadProyectoResponse r = service.obtener(1L);

        // cobrado 1000, sin pagos/comisiones/impuestos -> margen = 1000.
        assertThat(r.margenRealArs()).isEqualByComparingTo("1000.00");
    }
}

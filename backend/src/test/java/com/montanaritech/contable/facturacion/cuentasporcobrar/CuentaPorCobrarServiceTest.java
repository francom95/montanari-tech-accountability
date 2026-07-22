package com.montanaritech.contable.facturacion.cuentasporcobrar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.reporte.EstadoVencimiento;
import com.montanaritech.contable.facturacion.cobro.AplicacionAnticipoClienteRepository;
import com.montanaritech.contable.facturacion.cobro.CobroImputacionRepository;
import com.montanaritech.contable.facturacion.cobro.dto.ImputadoFacturaVenta;
import com.montanaritech.contable.facturacion.cuentasporcobrar.dto.CuentaPorCobrarResponse;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaRepository;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.moneda.Moneda;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CuentaPorCobrarServiceTest {

    @Mock private FacturaVentaRepository facturaVentaRepo;
    @Mock private CobroImputacionRepository cobroImputacionRepo;
    @Mock private AplicacionAnticipoClienteRepository aplicacionAnticipoRepo;

    private CuentaPorCobrarService service;
    private Cliente cliente;
    private Moneda ars;

    @BeforeEach
    void setUp() {
        service = new CuentaPorCobrarService(facturaVentaRepo, cobroImputacionRepo, aplicacionAnticipoRepo);

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Valvecchia Gerardo");

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        lenient().when(cobroImputacionRepo.sumarImputadoPorFactura(any(), eq(EstadoDocumento.CONFIRMADO))).thenReturn(List.of());
        lenient().when(aplicacionAnticipoRepo.sumarAplicacionesPorFactura(any())).thenReturn(List.of());
    }

    private FacturaVenta factura(Long id, BigDecimal total, LocalDate fechaVencimiento) {
        FacturaVenta f = new FacturaVenta();
        f.setId(id);
        f.setNumero("00001-" + id);
        f.setCliente(cliente);
        f.setMoneda(ars);
        f.setFecha(LocalDate.of(2026, 1, 1));
        f.setFechaVencimiento(fechaVencimiento);
        f.setTotal(total);
        f.setTotalArs(total);
        f.setEstado(EstadoDocumento.CONFIRMADO);
        return f;
    }

    @Test
    void sinFacturasDevuelveListaVacia() {
        when(facturaVentaRepo.buscarConfirmadasParaReporte(any(), any(), any(), any(), any())).thenReturn(List.of());

        CuentaPorCobrarResponse resultado = service.calcular(null, null, null, null, null, null);

        assertThat(resultado.filas()).isEmpty();
        assertThat(resultado.totalesPorMoneda()).isEmpty();
    }

    @Test
    void facturaConSaldoPendienteApareceConSaldoCorrecto() {
        FacturaVenta f = factura(1L, new BigDecimal("1000.00"), null);
        when(facturaVentaRepo.buscarConfirmadasParaReporte(any(), any(), any(), any(), any())).thenReturn(List.of(f));
        when(cobroImputacionRepo.sumarImputadoPorFactura(List.of(1L), EstadoDocumento.CONFIRMADO))
                .thenReturn(List.of(new ImputadoFacturaVenta(1L, new BigDecimal("400.00"), new BigDecimal("400.00"))));

        CuentaPorCobrarResponse resultado = service.calcular(null, null, null, null, null, null);

        assertThat(resultado.filas()).hasSize(1);
        assertThat(resultado.filas().get(0).saldo()).isEqualByComparingTo("600.00");
        assertThat(resultado.filas().get(0).estadoVencimiento()).isEqualTo("SIN_VENCIMIENTO");
    }

    @Test
    void facturaTotalmenteCobradaNoApareceEnElListado() {
        FacturaVenta f = factura(2L, new BigDecimal("1000.00"), null);
        when(facturaVentaRepo.buscarConfirmadasParaReporte(any(), any(), any(), any(), any())).thenReturn(List.of(f));
        when(cobroImputacionRepo.sumarImputadoPorFactura(List.of(2L), EstadoDocumento.CONFIRMADO))
                .thenReturn(List.of(new ImputadoFacturaVenta(2L, new BigDecimal("1000.00"), new BigDecimal("1000.00"))));

        CuentaPorCobrarResponse resultado = service.calcular(null, null, null, null, null, null);

        assertThat(resultado.filas()).isEmpty();
    }

    @Test
    void combinaImputacionesDeCobroYAplicacionesDeAnticipo() {
        FacturaVenta f = factura(3L, new BigDecimal("1000.00"), null);
        when(facturaVentaRepo.buscarConfirmadasParaReporte(any(), any(), any(), any(), any())).thenReturn(List.of(f));
        when(cobroImputacionRepo.sumarImputadoPorFactura(List.of(3L), EstadoDocumento.CONFIRMADO))
                .thenReturn(List.of(new ImputadoFacturaVenta(3L, new BigDecimal("300.00"), new BigDecimal("300.00"))));
        when(aplicacionAnticipoRepo.sumarAplicacionesPorFactura(List.of(3L)))
                .thenReturn(List.of(new ImputadoFacturaVenta(3L, new BigDecimal("200.00"), new BigDecimal("200.00"))));

        CuentaPorCobrarResponse resultado = service.calcular(null, null, null, null, null, null);

        assertThat(resultado.filas().get(0).saldo()).isEqualByComparingTo("500.00");
    }

    @Test
    void filtroVencidoSoloDevuelveFacturasVencidas() {
        FacturaVenta vencida = factura(4L, new BigDecimal("100.00"), LocalDate.now().minusDays(5));
        FacturaVenta porVencer = factura(5L, new BigDecimal("100.00"), LocalDate.now().plusDays(5));
        when(facturaVentaRepo.buscarConfirmadasParaReporte(any(), any(), any(), any(), any())).thenReturn(List.of(vencida, porVencer));

        CuentaPorCobrarResponse resultado = service.calcular(null, null, null, null, null, EstadoVencimiento.VENCIDO);

        assertThat(resultado.filas()).hasSize(1);
        assertThat(resultado.filas().get(0).facturaVentaId()).isEqualTo(4L);
    }

    @Test
    void totalesPorMonedaSumanTodasLasFacturasDeEsaMoneda() {
        FacturaVenta f1 = factura(6L, new BigDecimal("100.00"), null);
        FacturaVenta f2 = factura(7L, new BigDecimal("250.00"), null);
        when(facturaVentaRepo.buscarConfirmadasParaReporte(any(), any(), any(), any(), any())).thenReturn(List.of(f1, f2));

        CuentaPorCobrarResponse resultado = service.calcular(null, null, null, null, null, null);

        assertThat(resultado.totalesPorMoneda()).hasSize(1);
        assertThat(resultado.totalesPorMoneda().get(0).monedaCodigo()).isEqualTo("ARS");
        assertThat(resultado.totalesPorMoneda().get(0).totalSaldo()).isEqualByComparingTo("350.00");
    }
}

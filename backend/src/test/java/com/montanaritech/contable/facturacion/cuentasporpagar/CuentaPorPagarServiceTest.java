package com.montanaritech.contable.facturacion.cuentasporpagar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.reporte.EstadoVencimiento;
import com.montanaritech.contable.facturacion.cuentasporpagar.dto.CuentaPorPagarResponse;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompra;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompraRepository;
import com.montanaritech.contable.facturacion.pago.AplicacionAnticipoProveedorRepository;
import com.montanaritech.contable.facturacion.pago.PagoImputacionRepository;
import com.montanaritech.contable.facturacion.pago.dto.ImputadoFacturaCompra;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CuentaPorPagarServiceTest {

    @Mock private FacturaCompraRepository facturaCompraRepo;
    @Mock private PagoImputacionRepository pagoImputacionRepo;
    @Mock private AplicacionAnticipoProveedorRepository aplicacionAnticipoRepo;

    private CuentaPorPagarService service;
    private Proveedor proveedor;
    private Moneda ars;

    @BeforeEach
    void setUp() {
        service = new CuentaPorPagarService(facturaCompraRepo, pagoImputacionRepo, aplicacionAnticipoRepo);

        proveedor = new Proveedor();
        proveedor.setId(1L);
        proveedor.setNombre("Juan Programador");

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        lenient().when(pagoImputacionRepo.sumarImputadoPorFactura(any(), eq(EstadoDocumento.CONFIRMADO))).thenReturn(List.of());
        lenient().when(aplicacionAnticipoRepo.sumarAplicacionesPorFactura(any())).thenReturn(List.of());
    }

    private FacturaCompra factura(Long id, BigDecimal total, LocalDate fechaVencimiento) {
        FacturaCompra f = new FacturaCompra();
        f.setId(id);
        f.setNumero("0000" + id);
        f.setProveedor(proveedor);
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
        when(facturaCompraRepo.buscarConfirmadasParaReporte(any(), any(), any(), any(), any())).thenReturn(List.of());

        CuentaPorPagarResponse resultado = service.calcular(null, null, null, null, null, null);

        assertThat(resultado.filas()).isEmpty();
        assertThat(resultado.totalesPorMoneda()).isEmpty();
    }

    @Test
    void facturaConSaldoPendienteApareceConSaldoCorrecto() {
        FacturaCompra f = factura(1L, new BigDecimal("1000.00"), null);
        when(facturaCompraRepo.buscarConfirmadasParaReporte(any(), any(), any(), any(), any())).thenReturn(List.of(f));
        when(pagoImputacionRepo.sumarImputadoPorFactura(List.of(1L), EstadoDocumento.CONFIRMADO))
                .thenReturn(List.of(new ImputadoFacturaCompra(1L, new BigDecimal("400.00"), new BigDecimal("400.00"))));

        CuentaPorPagarResponse resultado = service.calcular(null, null, null, null, null, null);

        assertThat(resultado.filas()).hasSize(1);
        assertThat(resultado.filas().get(0).saldo()).isEqualByComparingTo("600.00");
    }

    @Test
    void facturaTotalmentePagadaNoApareceEnElListado() {
        FacturaCompra f = factura(2L, new BigDecimal("1000.00"), null);
        when(facturaCompraRepo.buscarConfirmadasParaReporte(any(), any(), any(), any(), any())).thenReturn(List.of(f));
        when(pagoImputacionRepo.sumarImputadoPorFactura(List.of(2L), EstadoDocumento.CONFIRMADO))
                .thenReturn(List.of(new ImputadoFacturaCompra(2L, new BigDecimal("1000.00"), new BigDecimal("1000.00"))));

        CuentaPorPagarResponse resultado = service.calcular(null, null, null, null, null, null);

        assertThat(resultado.filas()).isEmpty();
    }

    @Test
    void filtroPorVencerSoloDevuelveFacturasPorVencer() {
        FacturaCompra vencida = factura(3L, new BigDecimal("100.00"), LocalDate.now().minusDays(5));
        FacturaCompra porVencer = factura(4L, new BigDecimal("100.00"), LocalDate.now().plusDays(5));
        when(facturaCompraRepo.buscarConfirmadasParaReporte(any(), any(), any(), any(), any())).thenReturn(List.of(vencida, porVencer));

        CuentaPorPagarResponse resultado = service.calcular(null, null, null, null, null, EstadoVencimiento.POR_VENCER);

        assertThat(resultado.filas()).hasSize(1);
        assertThat(resultado.filas().get(0).facturaCompraId()).isEqualTo(4L);
    }
}

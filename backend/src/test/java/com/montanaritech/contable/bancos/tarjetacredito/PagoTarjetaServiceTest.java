package com.montanaritech.contable.bancos.tarjetacredito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.bancos.tarjetacredito.dto.PagoTarjetaCrearRequest;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.saldo.RecalculoSaldoService;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoService;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCredito;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCreditoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Pago del resumen de tarjeta (F5.4 §3): borrador -> confirmar (genera asiento, recalcula saldo) -> anular. */
@ExtendWith(MockitoExtension.class)
class PagoTarjetaServiceTest {

    @Mock private PagoTarjetaRepository repo;
    @Mock private TarjetaCreditoRepository tarjetaCreditoRepository;
    @Mock private MonedaRepository monedaRepository;
    @Mock private PagoTarjetaMapper mapper;
    @Mock private AuditoriaService auditoria;
    @Mock private AsientoService asientoService;
    @Mock private PagoTarjetaAsientoGenerator generator;
    @Mock private RecalculoSaldoService recalculoSaldoService;

    private PagoTarjetaService service;
    private TarjetaCredito tarjeta;
    private Moneda ars;

    @BeforeEach
    void setUp() {
        service = new PagoTarjetaService(repo, tarjetaCreditoRepository, monedaRepository, mapper, auditoria,
                asientoService, generator, recalculoSaldoService);

        CuentaContable cuentaTarjeta = new CuentaContable();
        cuentaTarjeta.setId(1L);
        cuentaTarjeta.setCodigo("2.1.2019");
        CuentaContable cuentaFondos = new CuentaContable();
        cuentaFondos.setId(2L);
        cuentaFondos.setCodigo("1.1.2001");
        CuentaBancaria cuentaBancaria = new CuentaBancaria();
        cuentaBancaria.setId(10L);
        cuentaBancaria.setCuentaContable(cuentaFondos);

        tarjeta = new TarjetaCredito();
        tarjeta.setId(5L);
        tarjeta.setCuentaContable(cuentaTarjeta);
        tarjeta.setCuentaBancariaDebito(cuentaBancaria);

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        lenient().when(tarjetaCreditoRepository.findById(5L)).thenReturn(Optional.of(tarjeta));
        lenient().when(monedaRepository.findById(1L)).thenReturn(Optional.of(ars));
        lenient().when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(mapper.aResponse(any())).thenReturn(null);
    }

    @Test
    void crearBorradorConImporteCeroLanzaError() {
        var req = new PagoTarjetaCrearRequest(5L, LocalDate.of(2026, 7, 5), BigDecimal.ZERO, 1L, BigDecimal.ONE, null);

        assertThatThrownBy(() -> service.crearBorrador(req))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("PAGO_TARJETA_SIN_IMPORTE");
    }

    @Test
    void crearBorradorEnArsFijaTipoDeCambioUnoEImporteArsIgualAlImporte() {
        var req = new PagoTarjetaCrearRequest(5L, LocalDate.of(2026, 7, 5), new BigDecimal("50000.00"), 1L, BigDecimal.ONE, null);

        PagoTarjeta creado = service.crearBorrador(req);

        assertThat(creado.getEstado()).isEqualTo(EstadoDocumento.BORRADOR);
        assertThat(creado.getTipoCambio()).isEqualByComparingTo("1");
        assertThat(creado.getImporteArs()).isEqualByComparingTo("50000.00");
    }

    @Test
    void confirmarGeneraElAsientoYRecalculaElSaldoDeLaTarjeta() {
        PagoTarjeta p = new PagoTarjeta();
        p.setId(1L);
        p.setTarjetaCredito(tarjeta);
        p.setFecha(LocalDate.of(2026, 7, 5));
        p.setImporte(new BigDecimal("50000.00"));
        p.setImporteArs(new BigDecimal("50000.00"));
        p.setMoneda(ars);
        p.setTipoCambio(BigDecimal.ONE);
        p.setEstado(EstadoDocumento.BORRADOR);
        when(repo.findById(1L)).thenReturn(Optional.of(p));

        Asiento asientoPersistido = new Asiento();
        asientoPersistido.setId(50L);
        asientoPersistido.setNumero(3L);
        when(asientoService.registrarAutomatico(any())).thenReturn(asientoPersistido);

        PagoTarjeta confirmado = service.confirmar(1L);

        assertThat(confirmado.getEstado()).isEqualTo(EstadoDocumento.CONFIRMADO);
        assertThat(confirmado.getAsiento()).isSameAs(asientoPersistido);
        verify(recalculoSaldoService).recalcular(tarjeta);
    }

    @Test
    void anularUnPagoConfirmadoAnulaElAsientoYRecalculaElSaldo() {
        Asiento asiento = new Asiento();
        asiento.setId(50L);

        PagoTarjeta p = new PagoTarjeta();
        p.setId(1L);
        p.setTarjetaCredito(tarjeta);
        p.setEstado(EstadoDocumento.CONFIRMADO);
        p.setAsiento(asiento);
        when(repo.findById(1L)).thenReturn(Optional.of(p));

        service.anular(1L, "Pago duplicado por error");

        assertThat(p.getEstado()).isEqualTo(EstadoDocumento.ANULADO);
        verify(asientoService).anularPorDocumento(50L, "Pago duplicado por error");
        verify(recalculoSaldoService).recalcular(tarjeta);
    }
}

package com.montanaritech.contable.common.saldo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.bancos.tarjetacredito.ConsumoTarjeta;
import com.montanaritech.contable.bancos.tarjetacredito.ConsumoTarjetaRepository;
import com.montanaritech.contable.bancos.tarjetacredito.PagoTarjeta;
import com.montanaritech.contable.bancos.tarjetacredito.PagoTarjetaRepository;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCredito;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecalculoSaldoServiceTest {

    @Mock private ConsumoTarjetaRepository consumoTarjetaRepository;
    @Mock private PagoTarjetaRepository pagoTarjetaRepository;

    // CuentaFalsa no es TarjetaCredito: la rama que usa estos repos nunca se ejecuta en esos tests.
    private final RecalculoSaldoService service = new RecalculoSaldoService(null, null);

    private static class CuentaFalsa implements CuentaConSaldo {
        private BigDecimal saldoInicial;
        private LocalDate fechaSaldoInicial;
        private BigDecimal saldoActual;

        CuentaFalsa(BigDecimal saldoInicial, LocalDate fechaSaldoInicial) {
            this.saldoInicial = saldoInicial;
            this.fechaSaldoInicial = fechaSaldoInicial;
        }

        @Override
        public BigDecimal getSaldoInicial() {
            return saldoInicial;
        }

        @Override
        public LocalDate getFechaSaldoInicial() {
            return fechaSaldoInicial;
        }

        @Override
        public BigDecimal getSaldoActual() {
            return saldoActual;
        }

        @Override
        public void setSaldoActual(BigDecimal saldoActual) {
            this.saldoActual = saldoActual;
        }
    }

    @Test
    void recalcularIgualaElSaldoActualAlInicial() {
        CuentaFalsa cuenta = new CuentaFalsa(new BigDecimal("1000.00"), LocalDate.of(2026, 1, 1));

        BigDecimal resultado = service.recalcular(cuenta);

        assertThat(resultado).isEqualByComparingTo("1000.00");
        assertThat(cuenta.getSaldoActual()).isEqualByComparingTo("1000.00");
    }

    @Test
    void modificarElSaldoInicialYRecalcularActualizaElSaldoActual() {
        CuentaFalsa cuenta = new CuentaFalsa(new BigDecimal("1000.00"), LocalDate.of(2026, 1, 1));
        service.recalcular(cuenta);
        assertThat(cuenta.getSaldoActual()).isEqualByComparingTo("1000.00");

        cuenta.saldoInicial = new BigDecimal("2500.00");
        cuenta.fechaSaldoInicial = LocalDate.of(2026, 2, 1);
        service.recalcular(cuenta);

        assertThat(cuenta.getSaldoActual()).isEqualByComparingTo("2500.00");
    }

    // ---- TarjetaCredito (F5.4): saldo inicial + consumos - pagos confirmados ----

    private TarjetaCredito tarjeta(Long id, BigDecimal saldoInicial, LocalDate fechaSaldoInicial) {
        TarjetaCredito t = new TarjetaCredito();
        t.setId(id);
        t.setSaldoInicial(saldoInicial);
        t.setFechaSaldoInicial(fechaSaldoInicial);
        return t;
    }

    private ConsumoTarjeta consumo(BigDecimal importeArs) {
        ConsumoTarjeta c = new ConsumoTarjeta();
        c.setImporteArs(importeArs);
        return c;
    }

    private PagoTarjeta pago(BigDecimal importeArs) {
        PagoTarjeta p = new PagoTarjeta();
        p.setImporteArs(importeArs);
        return p;
    }

    @Test
    void tarjetaSinIdTodaviaNoPersistidaNoConsultaRepositorios() {
        RecalculoSaldoService servicioConTarjeta = new RecalculoSaldoService(consumoTarjetaRepository, pagoTarjetaRepository);
        TarjetaCredito t = tarjeta(null, new BigDecimal("1000.00"), LocalDate.of(2026, 1, 1));

        BigDecimal resultado = servicioConTarjeta.recalcular(t);

        assertThat(resultado).isEqualByComparingTo("1000.00");
    }

    @Test
    void tarjetaSumaConsumosYSumaPagosConfirmadosPosterioresAlSaldoInicial() {
        RecalculoSaldoService servicioConTarjeta = new RecalculoSaldoService(consumoTarjetaRepository, pagoTarjetaRepository);
        TarjetaCredito t = tarjeta(1L, BigDecimal.ZERO, LocalDate.of(2026, 1, 1));

        // los consumos ya vienen negativos (egreso) por convención de ParserTarjeta
        when(consumoTarjetaRepository.findByTarjetaCredito_IdAndFechaAfter(eq(1L), eq(LocalDate.of(2026, 1, 1))))
                .thenReturn(List.of(consumo(new BigDecimal("-300.00")), consumo(new BigDecimal("-200.00"))));
        when(pagoTarjetaRepository.findByTarjetaCredito_IdAndEstadoAndFechaAfter(eq(1L), eq(EstadoDocumento.CONFIRMADO), eq(LocalDate.of(2026, 1, 1))))
                .thenReturn(List.of(pago(new BigDecimal("400.00"))));

        BigDecimal resultado = servicioConTarjeta.recalcular(t);

        // 0 - 300 - 200 + 400 = -100 (un pago parcial: queda saldo pendiente, negativo = deuda)
        assertThat(resultado).isEqualByComparingTo("-100.00");
        assertThat(t.getSaldoActual()).isEqualByComparingTo("-100.00");
    }

    @Test
    void tarjetaSinConsumosNiPagosMantieneElSaldoInicial() {
        RecalculoSaldoService servicioConTarjeta = new RecalculoSaldoService(consumoTarjetaRepository, pagoTarjetaRepository);
        TarjetaCredito t = tarjeta(2L, new BigDecimal("500.00"), LocalDate.of(2026, 1, 1));

        when(consumoTarjetaRepository.findByTarjetaCredito_IdAndFechaAfter(any(), any())).thenReturn(List.of());
        when(pagoTarjetaRepository.findByTarjetaCredito_IdAndEstadoAndFechaAfter(any(), any(), any())).thenReturn(List.of());

        assertThat(servicioConTarjeta.recalcular(t)).isEqualByComparingTo("500.00");
    }
}

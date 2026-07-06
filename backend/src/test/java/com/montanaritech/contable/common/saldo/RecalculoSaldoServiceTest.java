package com.montanaritech.contable.common.saldo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RecalculoSaldoServiceTest {

    private final RecalculoSaldoService service = new RecalculoSaldoService();

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
}

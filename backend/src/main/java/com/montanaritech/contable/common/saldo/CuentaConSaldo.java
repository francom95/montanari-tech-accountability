package com.montanaritech.contable.common.saldo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Contrato común de toda cuenta/tarjeta con saldo inicial (F2.4). Lo implementan
 * CuentaBancaria y TarjetaCredito para que {@link RecalculoSaldoService} pueda
 * recalcular ambas sin acoplarse a una entidad concreta.
 */
public interface CuentaConSaldo {
    BigDecimal getSaldoInicial();

    LocalDate getFechaSaldoInicial();

    BigDecimal getSaldoActual();

    void setSaldoActual(BigDecimal saldoActual);
}

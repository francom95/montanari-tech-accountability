package com.montanaritech.contable.common.saldo;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Recalcula el saldo actual de una cuenta/tarjeta a partir de su saldo inicial
 * (F2.4). Todavía no existe un libro de movimientos — llega recién en F5.1
 * ("movimientos bancarios") — así que hoy el saldo actual coincide siempre con
 * el inicial. Este es el único punto de extensión: cuando F5.1 exista, este
 * método pasa a sumar los movimientos posteriores a {@code fechaSaldoInicial}
 * en vez de devolver el saldo inicial tal cual, sin que conciliación (F5.x) ni
 * flujo de caja (F8.3) — que ya invocan este servicio — tengan que cambiar.
 */
@Service
public class RecalculoSaldoService {

    public BigDecimal recalcular(CuentaConSaldo cuenta) {
        BigDecimal saldo = cuenta.getSaldoInicial();
        cuenta.setSaldoActual(saldo);
        return saldo;
    }
}

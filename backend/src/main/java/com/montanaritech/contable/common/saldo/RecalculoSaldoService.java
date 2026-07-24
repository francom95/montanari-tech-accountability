package com.montanaritech.contable.common.saldo;

import com.montanaritech.contable.bancos.movimientobancario.EstadoMovimientoBancario;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancarioRepository;
import com.montanaritech.contable.bancos.tarjetacredito.ConsumoTarjetaRepository;
import com.montanaritech.contable.bancos.tarjetacredito.PagoTarjetaRepository;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCredito;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Recalcula el saldo actual de una cuenta/tarjeta a partir de su saldo inicial
 * (F2.4). Para {@link TarjetaCredito} (F5.4): saldo inicial + consumos
 * (F5.2/F5.4, {@code ConsumoTarjeta}, ya negativos por convención de
 * {@code ParserTarjeta}) + pagos confirmados del resumen ({@code PagoTarjeta},
 * importe positivo) posteriores a {@code fechaSaldoInicial} — un pago
 * parcial (pago mínimo) simplemente deja el resto como saldo pendiente, sin
 * lógica especial. Para {@link CuentaBancaria} (F7.5): saldo inicial + todo
 * {@code MovimientoBancario} no {@code DESCARTADO} (PENDIENTE y CONCILIADO
 * cuentan igual) desde {@code fechaSaldoInicial} hasta la fecha pedida,
 * ambos extremos inclusive — misma semántica que ya usaba
 * {@code ConciliacionService} (F5.3), ahora centralizada acá.
 */
@Service
@RequiredArgsConstructor
public class RecalculoSaldoService {

    private final ConsumoTarjetaRepository consumoTarjetaRepository;
    private final PagoTarjetaRepository pagoTarjetaRepository;
    private final MovimientoBancarioRepository movimientoBancarioRepository;

    public BigDecimal recalcular(CuentaConSaldo cuenta) {
        BigDecimal saldo;
        if (cuenta instanceof TarjetaCredito tarjeta) {
            saldo = recalcularTarjeta(tarjeta);
        } else if (cuenta instanceof CuentaBancaria cuentaBancaria) {
            saldo = recalcularCuentaBancariaHasta(cuentaBancaria, LocalDate.now());
        } else {
            saldo = cuenta.getSaldoInicial();
        }
        cuenta.setSaldoActual(saldo);
        return saldo;
    }

    private BigDecimal recalcularTarjeta(TarjetaCredito tarjeta) {
        BigDecimal saldo = tarjeta.getSaldoInicial();
        if (tarjeta.getId() == null) {
            return saldo; // todavía no persistida: no hay consumos/pagos que pudieran referenciarla
        }
        for (var consumo : consumoTarjetaRepository.findByTarjetaCredito_IdAndFechaAfter(tarjeta.getId(), tarjeta.getFechaSaldoInicial())) {
            saldo = saldo.add(consumo.getImporteArs());
        }
        for (var pago : pagoTarjetaRepository.findByTarjetaCredito_IdAndEstadoAndFechaAfter(
                tarjeta.getId(), EstadoDocumento.CONFIRMADO, tarjeta.getFechaSaldoInicial())) {
            saldo = saldo.add(pago.getImporteArs());
        }
        return saldo;
    }

    /** Saldo real de la cuenta bancaria a una fecha dada (usado por el dashboard y por la conciliación). */
    public BigDecimal recalcularCuentaBancariaHasta(CuentaBancaria cuenta, LocalDate fechaHasta) {
        BigDecimal saldo = cuenta.getSaldoInicial();
        if (cuenta.getId() == null) {
            return saldo; // todavía no persistida: no hay movimientos que pudieran referenciarla
        }
        for (var movimiento : movimientoBancarioRepository.buscarParaConciliacion(
                cuenta.getId(), cuenta.getFechaSaldoInicial(), fechaHasta)) {
            if (movimiento.getEstado() != EstadoMovimientoBancario.DESCARTADO) {
                saldo = saldo.add(movimiento.getImporte());
            }
        }
        return saldo;
    }
}

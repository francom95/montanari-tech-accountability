package com.montanaritech.contable.common.saldo;

import com.montanaritech.contable.bancos.tarjetacredito.ConsumoTarjetaRepository;
import com.montanaritech.contable.bancos.tarjetacredito.PagoTarjetaRepository;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCredito;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Recalcula el saldo actual de una cuenta/tarjeta a partir de su saldo inicial
 * (F2.4). Para {@link TarjetaCredito} (F5.4): saldo inicial + consumos
 * (F5.2/F5.4, {@code ConsumoTarjeta}, ya negativos por convención de
 * {@code ParserTarjeta}) + pagos confirmados del resumen ({@code PagoTarjeta},
 * importe positivo) posteriores a {@code fechaSaldoInicial} — un pago
 * parcial (pago mínimo) simplemente deja el resto como saldo pendiente, sin
 * lógica especial. Para {@code CuentaBancaria} todavía no se actualizó
 * (sigue devolviendo el saldo inicial tal cual) — pendiente para cuando se
 * aborde F8.3 (flujo de caja), que también invoca este servicio.
 */
@Service
@RequiredArgsConstructor
public class RecalculoSaldoService {

    private final ConsumoTarjetaRepository consumoTarjetaRepository;
    private final PagoTarjetaRepository pagoTarjetaRepository;

    public BigDecimal recalcular(CuentaConSaldo cuenta) {
        BigDecimal saldo = cuenta instanceof TarjetaCredito tarjeta
                ? recalcularTarjeta(tarjeta)
                : cuenta.getSaldoInicial();
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
}

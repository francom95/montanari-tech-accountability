package com.montanaritech.contable.bancos.tarjetacredito;

import com.montanaritech.contable.common.asiento.AsientoGenerado;
import com.montanaritech.contable.common.asiento.AsientoGenerator;
import com.montanaritech.contable.common.asiento.LineaAsientoGenerada;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCredito;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Generador de asiento para el pago de un resumen de tarjeta (F5.4 §3, molde
 * PL-4, {@code OrigenAsiento.RESUMEN_TARJETA} ya predeclarado desde F3.4).
 * Solo 2 líneas — a diferencia de {@code PagoAsientoGenerator} (F4.4), acá
 * no hay imputaciones contra facturas individuales ni anticipos: el pago
 * cancela directamente la cuenta pasiva de la tarjeta, sea total o parcial
 * (el saldo pendiente sale de {@code RecalculoSaldoService}, no de este
 * generador). <b>Haber</b> la cuenta contable espejo de
 * {@code cuentaBancariaDebito}, con {@code cuentaBancariaId} seteado —
 * eso alcanza para que F5.3 (conciliación bancaria) sugiera este asiento
 * como match contra el movimiento bancario real, sin ningún código nuevo
 * en F5.3. <b>Debe</b> la cuenta contable espejo de la tarjeta (F5.4,
 * {@code TarjetaCredito.cuentaContable}), reduciendo la deuda.
 */
@Component
public class PagoTarjetaAsientoGenerator implements AsientoGenerator<PagoTarjeta> {

    @Override
    public AsientoGenerado generar(PagoTarjeta pago) {
        if (pago.getImporte().compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("PAGO_TARJETA_SIN_IMPORTE", "El pago no tiene ningún importe a contabilizar");
        }

        TarjetaCredito tarjeta = pago.getTarjetaCredito();
        CuentaContable cuentaTarjeta = tarjeta.getCuentaContable();
        if (cuentaTarjeta == null) {
            throw new NegocioException("TARJETA_SIN_CUENTA_CONTABLE",
                    "La tarjeta " + tarjeta.getEntidad() + " no tiene una cuenta contable configurada — completala antes de pagar el resumen");
        }
        CuentaContable cuentaFondos = tarjeta.getCuentaBancariaDebito().getCuentaContable();

        Long monedaId = pago.getMoneda().getId();
        BigDecimal tc = pago.getTipoCambio();
        String fuenteTc = "MANUAL";

        List<LineaAsientoGenerada> lineas = List.of(
                new LineaAsientoGenerada(cuentaTarjeta.getCodigo(), pago.getImporteArs(), BigDecimal.ZERO,
                        "Pago resumen tarjeta " + tarjeta.getEntidad(), monedaId, pago.getImporte(), tc, fuenteTc,
                        null, null, null, null, null),
                new LineaAsientoGenerada(cuentaFondos.getCodigo(), BigDecimal.ZERO, pago.getImporteArs(),
                        "Pago resumen tarjeta " + tarjeta.getEntidad(), monedaId, pago.getImporte(), tc, fuenteTc,
                        null, null, null, null, tarjeta.getCuentaBancariaDebito().getId()));

        return new AsientoGenerado(pago.getFecha(), "Pago resumen tarjeta " + tarjeta.getEntidad(),
                "RESUMEN_TARJETA", lineas, "PagoTarjeta", pago.getId());
    }
}

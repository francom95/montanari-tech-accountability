package com.montanaritech.contable.common.asiento;

import com.montanaritech.contable.maestros.moneda.Moneda;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementación de prueba de {@link AsientoGenerator}, NO una regla de
 * negocio real (Moneda no genera asientos). Existe solo para demostrar el
 * contrato completo: línea debe/haber balanceada, numeración compartida,
 * vínculo al documento origen. F4.1 en adelante la reemplaza por
 * generadores reales (factura de venta, cobro, etc.) que copian esta forma.
 */
@Component
@RequiredArgsConstructor
public class GeneradorAsientoDePrueba implements AsientoGenerator<Moneda> {

    private final NumeradorAsiento numeradorAsiento;

    @Override
    public AsientoGenerado generar(Moneda evento) {
        List<LineaAsientoGenerada> lineas = List.of(
                new LineaAsientoGenerada("1.1.1.01", BigDecimal.TEN, BigDecimal.ZERO, "Línea de prueba (debe)"),
                new LineaAsientoGenerada("1.1.1.02", BigDecimal.ZERO, BigDecimal.TEN, "Línea de prueba (haber)"));

        ValidadorBalanceAsiento.validar(lineas);

        return new AsientoGenerado(numeradorAsiento.siguienteNumero(), lineas, "Moneda", evento.getId());
    }
}

package com.montanaritech.contable.impuestos.atribucion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Reparte un monto entre proyectos según un peso por proyecto (F6.3). La clave
 * es la <b>regla del residuo</b>: cada línea salvo la última se redondea a
 * centavos ({@code round2(total × peso / Σpesos)}), y la última toma el
 * remanente exacto ({@code total − acumulado}), absorbiendo el residuo de todos
 * los redondeos previos. Así la suma de las líneas es <b>siempre</b> igual al
 * total, sin errores de centavos — mismo principio que {@code CalculoImputacion}
 * (F4.4). Función pura, sin dependencias, para testear el reparto por separado.
 */
public final class ProrrateoCalculator {

    private ProrrateoCalculator() {
    }

    /** Un proyecto con su peso relativo (facturación, margen, o porcentaje manual). */
    public record Peso(Long proyectoId, BigDecimal peso) {
    }

    /** Resultado del reparto para un proyecto. */
    public record Reparto(Long proyectoId, BigDecimal porcentaje, BigDecimal monto) {
    }

    /**
     * @param total el monto a repartir (no negativo)
     * @param pesos proyectos con peso &gt; 0, en orden estable (el orden fija cuál es la última línea)
     * @throws IllegalArgumentException si no hay pesos o su suma no es positiva
     */
    public static List<Reparto> repartir(BigDecimal total, List<Peso> pesos) {
        if (pesos.isEmpty()) {
            throw new IllegalArgumentException("No hay proyectos con base para repartir");
        }
        BigDecimal sumaPesos = pesos.stream().map(Peso::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sumaPesos.signum() <= 0) {
            throw new IllegalArgumentException("La suma de los pesos debe ser positiva para poder repartir");
        }

        List<Reparto> out = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;
        for (int i = 0; i < pesos.size(); i++) {
            Peso p = pesos.get(i);
            BigDecimal porcentaje = p.peso().multiply(BigDecimal.valueOf(100))
                    .divide(sumaPesos, 6, RoundingMode.HALF_UP);
            BigDecimal monto;
            if (i == pesos.size() - 1) {
                monto = total.subtract(acumulado); // la última línea absorbe el residuo
            } else {
                monto = total.multiply(p.peso()).divide(sumaPesos, 2, RoundingMode.HALF_UP);
                acumulado = acumulado.add(monto);
            }
            out.add(new Reparto(p.proyectoId(), porcentaje, monto.setScale(2, RoundingMode.HALF_UP)));
        }
        return out;
    }
}

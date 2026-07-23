package com.montanaritech.contable.impuestos.atribucion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Regla del residuo (F6.3): el test central del plan —el prorrateo por
 * facturación con 3 proyectos suma exactamente el total, sin error de centavos—
 * más los casos borde de la función pura.
 */
class ProrrateoCalculatorTest {

    @Test
    void prorrateoPorFacturacionConTresProyectosSumaExactamenteElTotal() {
        // Un total y pesos elegidos para que el reparto proporcional NO dé exacto
        // en centavos: 100 / 3 = 33,33 tres veces = 99,99, falta 0,01.
        BigDecimal total = new BigDecimal("100.00");
        List<ProrrateoCalculator.Peso> pesos = List.of(
                new ProrrateoCalculator.Peso(1L, new BigDecimal("1")),
                new ProrrateoCalculator.Peso(2L, new BigDecimal("1")),
                new ProrrateoCalculator.Peso(3L, new BigDecimal("1")));

        List<ProrrateoCalculator.Reparto> reparto = ProrrateoCalculator.repartir(total, pesos);

        assertThat(reparto).hasSize(3);
        assertThat(reparto.get(0).monto()).isEqualByComparingTo("33.33");
        assertThat(reparto.get(1).monto()).isEqualByComparingTo("33.33");
        // la última línea absorbe el residuo: 100 - 66,66 = 33,34
        assertThat(reparto.get(2).monto()).isEqualByComparingTo("33.34");
        assertThat(sumar(reparto)).as("la suma es exactamente el total").isEqualByComparingTo("100.00");
    }

    @Test
    void repartoProporcionalPorFacturacionRealSumaExacto() {
        // ventas: 1.000.000 / 600.000 / 400.000 sobre un impuesto de 118.500,50
        BigDecimal total = new BigDecimal("118500.50");
        List<ProrrateoCalculator.Peso> pesos = List.of(
                new ProrrateoCalculator.Peso(1L, new BigDecimal("1000000")),
                new ProrrateoCalculator.Peso(2L, new BigDecimal("600000")),
                new ProrrateoCalculator.Peso(3L, new BigDecimal("400000")));

        List<ProrrateoCalculator.Reparto> reparto = ProrrateoCalculator.repartir(total, pesos);

        assertThat(sumar(reparto)).isEqualByComparingTo(total);
        assertThat(reparto.get(0).monto()).isEqualByComparingTo("59250.25"); // 50%
        assertThat(reparto.get(1).monto()).isEqualByComparingTo("35550.15"); // 30%
        assertThat(reparto.get(2).monto()).isEqualByComparingTo("23700.10"); // 20% (residuo)
    }

    @Test
    void unSoloProyectoRecibeElTotalCompleto() {
        List<ProrrateoCalculator.Reparto> reparto = ProrrateoCalculator.repartir(
                new BigDecimal("1234.56"), List.of(new ProrrateoCalculator.Peso(9L, BigDecimal.ONE)));

        assertThat(reparto).singleElement().satisfies(r -> {
            assertThat(r.monto()).isEqualByComparingTo("1234.56");
            assertThat(r.porcentaje()).isEqualByComparingTo("100.000000");
        });
    }

    @Test
    void totalCeroRepartaCeroSinRomper() {
        List<ProrrateoCalculator.Reparto> reparto = ProrrateoCalculator.repartir(
                new BigDecimal("0.00"),
                List.of(new ProrrateoCalculator.Peso(1L, new BigDecimal("3")),
                        new ProrrateoCalculator.Peso(2L, new BigDecimal("7"))));

        assertThat(sumar(reparto)).isEqualByComparingTo("0.00");
    }

    @Test
    void sinPesosOConSumaCeroSeRechaza() {
        assertThatThrownBy(() -> ProrrateoCalculator.repartir(new BigDecimal("100"), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProrrateoCalculator.repartir(new BigDecimal("100"),
                List.of(new ProrrateoCalculator.Peso(1L, BigDecimal.ZERO))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private BigDecimal sumar(List<ProrrateoCalculator.Reparto> reparto) {
        return reparto.stream().map(ProrrateoCalculator.Reparto::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

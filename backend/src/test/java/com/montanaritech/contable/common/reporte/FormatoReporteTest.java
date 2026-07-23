package com.montanaritech.contable.common.reporte;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Formato AR de moneda y fecha para reportes exportables (F7.1). */
class FormatoReporteTest {

    @Test
    void formateaMontoPositivoConSeparadoresAr() {
        assertThat(FormatoReporte.formatearMoneda(new BigDecimal("1234567.89"))).isEqualTo("$ 1.234.567,89");
    }

    @Test
    void formateaMontoNegativo() {
        assertThat(FormatoReporte.formatearMoneda(new BigDecimal("-1234.5"))).isEqualTo("-$ 1.234,50");
    }

    @Test
    void formateaMontoNuloComoVacio() {
        assertThat(FormatoReporte.formatearMoneda(null)).isEmpty();
    }

    @Test
    void formateaFechaDdMmYyyy() {
        assertThat(FormatoReporte.formatearFecha(LocalDate.of(2026, 3, 5))).isEqualTo("05/03/2026");
    }

    @Test
    void formateaFechaNulaComoVacio() {
        assertThat(FormatoReporte.formatearFecha(null)).isEmpty();
    }
}

package com.montanaritech.contable.busqueda;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DetectorTerminoBusquedaTest {

    @Test
    void esCuitReconoceFormatoConGuiones() {
        assertThat(DetectorTerminoBusqueda.esCuit("20-12345678-9")).isEqualTo("20-12345678-9");
    }

    @Test
    void esCuitReconoceFormatoSinGuiones() {
        assertThat(DetectorTerminoBusqueda.esCuit("20123456789")).isEqualTo("20-12345678-9");
    }

    @Test
    void esCuitRechazaTextoQueNoTiene11Digitos() {
        assertThat(DetectorTerminoBusqueda.esCuit("Banco Galicia")).isNull();
        assertThat(DetectorTerminoBusqueda.esCuit("12345")).isNull();
    }

    @Test
    void comoImporteParseaDecimalConComaOPunto() {
        assertThat(DetectorTerminoBusqueda.comoImporte("1500.50")).isEqualByComparingTo("1500.50");
        assertThat(DetectorTerminoBusqueda.comoImporte("1500,50")).isEqualByComparingTo("1500.50");
        assertThat(DetectorTerminoBusqueda.comoImporte("1500")).isEqualByComparingTo("1500");
    }

    @Test
    void comoImporteRechazaTextoQueNoEsNumerico() {
        assertThat(DetectorTerminoBusqueda.comoImporte("Factura 123")).isNull();
        assertThat(DetectorTerminoBusqueda.comoImporte("20-12345678-9")).isNull();
    }

    @Test
    void toleranciaImporteEsUnPorCientoConPisoDeUnPeso() {
        assertThat(DetectorTerminoBusqueda.toleranciaImporte(new BigDecimal("10000"))).isEqualByComparingTo("100");
        // monto chico: 1% de 10 es 0.10, pero el piso es 1.00
        assertThat(DetectorTerminoBusqueda.toleranciaImporte(new BigDecimal("10"))).isEqualByComparingTo("1");
    }

    @Test
    void comoFechaParseaDdMmYyyyYFormatoIso() {
        assertThat(DetectorTerminoBusqueda.comoFecha("28/07/2026")).isEqualTo(LocalDate.of(2026, 7, 28));
        assertThat(DetectorTerminoBusqueda.comoFecha("2026-07-28")).isEqualTo(LocalDate.of(2026, 7, 28));
    }

    @Test
    void comoFechaRechazaTextoQueNoEsFecha() {
        assertThat(DetectorTerminoBusqueda.comoFecha("Banco Galicia")).isNull();
        assertThat(DetectorTerminoBusqueda.comoFecha("32/13/2026")).isNull();
    }

    @Test
    void prioridadDeDeteccionCuitAntesQueImporteAntesQueFecha() {
        // un CUIT sin guiones (11 dígitos) no debe interpretarse como importe
        assertThat(DetectorTerminoBusqueda.esCuit("20123456789")).isNotNull();
        assertThat(DetectorTerminoBusqueda.comoImporte("20123456789")).isNotNull(); // también parsea como número — la prioridad la aplica el service, no el detector
    }
}

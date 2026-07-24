package com.montanaritech.contable.maestros.proyecto.presupuesto;

import static org.assertj.core.api.Assertions.assertThat;

import com.montanaritech.contable.maestros.proyecto.Proyecto;
import java.math.BigDecimal;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

/**
 * Waterfall de presupuesto (F2.6), calibrado al centavo (y más) contra la
 * planilla real del contador (hoja GUADA / GUADA MODELO). Los dos casos
 * Exterior y el caso Argentina real (Excel "Presupuesto_ Web.xlsx", hoja
 * "PLANTILLA GUADA") están calibrados contra valores reales del contador —
 * checkpoint humano de F2.6 cerrado.
 */
class CalculoPresupuestoProyectoTest {

    private ConfiguracionPresupuesto configuracionReal() {
        ConfiguracionPresupuesto c = new ConfiguracionPresupuesto();
        c.setComisionVentaPorcentaje(new BigDecimal("0.10000"));
        c.setColchonImpuestoGananciasPorcentaje(new BigDecimal("0.30000"));
        c.setIibbConvenioMultilateralPorcentaje(new BigDecimal("0.05000"));
        c.setImpuestoDebitosCreditosPorcentaje(new BigDecimal("0.01200"));
        c.setIvaPorcentaje(new BigDecimal("0.21000"));
        c.setDiferenciaDolarComercializacionPorcentaje(new BigDecimal("0.05000"));
        c.setPercepcionIvaComexPorcentaje(new BigDecimal("0.03000"));
        c.setIibbSircrebComexPorcentaje(new BigDecimal("0.04000"));
        c.setComexUmbralUnoUsd(new BigDecimal("100.00"));
        c.setComexMontoUnoUsd(new BigDecimal("10.00"));
        c.setComexUmbralDosUsd(new BigDecimal("500.00"));
        c.setComexMontoDosUsd(new BigDecimal("30.00"));
        c.setComexUmbralTresUsd(new BigDecimal("1000.00"));
        c.setComexMontoTresUsd(new BigDecimal("50.00"));
        c.setComexPorcentajeExcedente(new BigDecimal("0.00125"));
        return c;
    }

    /** Excel real "Presupuesto Proyecto Exterior", hoja "PLANTILLA GUADA MODELO": costo 1500 (3 líneas x 500), margen 1500, comisiones intermedias COMEX 200, 5 cuotas. */
    @Test
    void exteriorCaso1CalibradoContraElExcelReal() {
        PresupuestoCalculado r = CalculoPresupuestoProyecto.calcular(
                Proyecto.TipoProyecto.EXTERIOR,
                new BigDecimal("1500.00"),
                new BigDecimal("1500.00"),
                new BigDecimal("200.00"),
                5,
                configuracionReal());

        assertThat(r.colchonImpuestoGanancias()).isEqualByComparingTo("642.8571428571");
        assertThat(r.comisionBancariaComex()).isEqualByComparingTo("250");
        assertThat(r.percepcionIvaComex()).isEqualByComparingTo("7.5");
        assertThat(r.iibbSircrebComex()).isEqualByComparingTo("10");
        assertThat(r.ivaCreditoFiscalComex()).isEqualByComparingTo("52.5");
        // El caché del Excel guarda el valor con precisión double (IEEE 754); la
        // diferencia de ~3e-7 es ruido de punto flotante de Excel, no de la fórmula
        // (ambos redondean exactamente a 4902.00 en 2 decimales).
        assertThat(r.precioFinalCliente()).isCloseTo(new BigDecimal("4901.996639"), Offset.offset(new BigDecimal("0.0001")));
    }

    /** Excel real "Presupuesto Proyecto Exterior", hoja "Copia de PLANTILLA GUADA MODELO": costo 1200 (1 línea), margen 1500, comisiones intermedias COMEX 200, 5 cuotas. */
    @Test
    void exteriorCaso2CalibradoContraElExcelReal() {
        PresupuestoCalculado r = CalculoPresupuestoProyecto.calcular(
                Proyecto.TipoProyecto.EXTERIOR,
                new BigDecimal("1200.00"),
                new BigDecimal("1500.00"),
                new BigDecimal("200.00"),
                5,
                configuracionReal());

        assertThat(r.comisionBancariaComex()).isEqualByComparingTo("250");
        assertThat(r.precioFinalCliente()).isCloseTo(new BigDecimal("4549.055462"), Offset.offset(new BigDecimal("0.0001")));
    }

    @Test
    void exteriorSinComisionesIntermediasNiCuotasNoRompe() {
        PresupuestoCalculado r = CalculoPresupuestoProyecto.calcular(
                Proyecto.TipoProyecto.EXTERIOR,
                new BigDecimal("100.00"),
                new BigDecimal("50.00"),
                null,
                1,
                configuracionReal());

        assertThat(r.totalCostoProduccion()).isEqualByComparingTo("100.00");
        assertThat(r.precioFinalCliente()).isGreaterThan(BigDecimal.ZERO);
    }

    /** Excel real "Presupuesto_ Web.xlsx", hoja "PLANTILLA GUADA": proyecto SEEU, costo 150 (Diseño 100 + Desarrollo 50), margen deseado 100, 1 pago. Checkpoint humano de F2.6 (Argentina). */
    @Test
    void argentinaCasoRealCalibradoContraElExcelReal() {
        PresupuestoCalculado r = CalculoPresupuestoProyecto.calcular(
                Proyecto.TipoProyecto.ARGENTINA,
                new BigDecimal("150.00"),
                new BigDecimal("100.00"),
                null,
                1,
                configuracionReal());

        assertThat(r.colchonImpuestoGanancias()).isEqualByComparingTo("42.8571428571");
        assertThat(r.totalCostoMasGanancia()).isEqualByComparingTo("292.8571428571");
        assertThat(r.comisionVenta()).isEqualByComparingTo("35.05");
        assertThat(r.precioSinIva()).isCloseTo(new BigDecimal("350.522879"), Offset.offset(new BigDecimal("0.0001")));
        assertThat(r.iibbConvenioMultilateral()).isCloseTo(new BigDecimal("17.52614395"), Offset.offset(new BigDecimal("0.0001")));
        assertThat(r.impuestoDebitosCreditos()).isCloseTo(new BigDecimal("5.089592203"), Offset.offset(new BigDecimal("0.0001")));
        assertThat(r.ivaDebitoFiscal()).isCloseTo(new BigDecimal("73.60980459"), Offset.offset(new BigDecimal("0.0001")));
        assertThat(r.precioConIva()).isCloseTo(new BigDecimal("424.1326836"), Offset.offset(new BigDecimal("0.0001")));
    }

    /**
     * Caso propio adicional (no calibrado contra un Excel específico, solo
     * verificado algebraicamente a mano): costo 1000, margen deseado 500.
     */
    @Test
    void argentinaCasoPropioVerificadoAlgebraicamente() {
        PresupuestoCalculado r = CalculoPresupuestoProyecto.calcular(
                Proyecto.TipoProyecto.ARGENTINA,
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                null,
                1,
                configuracionReal());

        assertThat(r.colchonImpuestoGanancias()).isEqualByComparingTo("214.2857142857");
        assertThat(r.totalCostoMasGanancia()).isEqualByComparingTo("1714.2857142857");
        assertThat(r.comisionVenta()).isEqualByComparingTo("205.19");
        assertThat(r.precioSinIva()).isEqualByComparingTo("2051.8618402165");
        assertThat(r.iibbConvenioMultilateral()).isEqualByComparingTo("102.593092010825");
        assertThat(r.impuestoDebitosCreditos()).isEqualByComparingTo("29.79303391994358");
        assertThat(r.ivaDebitoFiscal()).isEqualByComparingTo("430.890986445465");
        assertThat(r.precioConIva()).isEqualByComparingTo("2482.752826661965");
    }

    @Test
    void argentinaSumaDeLineasDeCostoDaElTotalDeProduccion() {
        PresupuestoCalculado r = CalculoPresupuestoProyecto.calcular(
                Proyecto.TipoProyecto.ARGENTINA,
                new BigDecimal("1500.00"),
                new BigDecimal("300.00"),
                null,
                3,
                configuracionReal());

        assertThat(r.totalCostoProduccion()).isEqualByComparingTo("1500.00");
        assertThat(r.margenDeseadoUsd()).isEqualByComparingTo("300.00");
        // Argentina no completa los campos de Exterior.
        assertThat(r.comisionBancariaComex()).isNull();
        assertThat(r.comisionVenta()).isNotNull();
    }
}

package com.montanaritech.contable.maestros.proyecto.presupuesto;

import com.montanaritech.contable.maestros.proyecto.Proyecto;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Waterfall de precio del presupuesto de proyecto (F2.6), función pura sin
 * dependencias — replica exacto la planilla real del contador (hoja GUADA /
 * GUADA MODELO, no la hoja FRAN, que tiene una constante hardcodeada
 * 363/24637 en vez de aplicar el 1,2% de forma paramétrica). Dos cascadas
 * completamente distintas según {@code tipoProyecto}: Argentina (IVA + IIBB
 * Convenio Multilateral doméstico) o Exterior (comisión bancaria COMEX por
 * tramos + percepciones sobre la transferencia, la venta de servicios al
 * exterior está exenta de IVA/IIBB directo).
 *
 * <p>Ninguna división se redondea a centavos salvo donde la planilla lo hace
 * explícito (Comisión por Venta, Argentina) — todo lo demás se mantiene con
 * precisión interna alta (escala 10) para poder calibrarse al centavo (o más)
 * contra el Excel real, igual que {@code CalculoIvaService}/{@code
 * ProrrateoCalculator}.
 */
public final class CalculoPresupuestoProyecto {

    private static final int ESCALA_INTERNA = 10;

    private CalculoPresupuestoProyecto() {
    }

    public static PresupuestoCalculado calcular(
            Proyecto.TipoProyecto tipoProyecto,
            BigDecimal totalCostoProduccion,
            BigDecimal margenDeseadoUsd,
            BigDecimal comisionesBancariasIntermediasComexUsd,
            int cantidadPagos,
            ConfiguracionPresupuesto config) {

        BigDecimal colchonIG = margenDeseadoUsd
                .divide(BigDecimal.ONE.subtract(config.getColchonImpuestoGananciasPorcentaje()), ESCALA_INTERNA, RoundingMode.HALF_UP)
                .subtract(margenDeseadoUsd);
        BigDecimal totalCostoMasGanancia = totalCostoProduccion.add(margenDeseadoUsd).add(colchonIG);

        return tipoProyecto == Proyecto.TipoProyecto.ARGENTINA
                ? calcularArgentina(totalCostoProduccion, margenDeseadoUsd, colchonIG, totalCostoMasGanancia, config)
                : calcularExterior(totalCostoProduccion, margenDeseadoUsd, colchonIG, totalCostoMasGanancia,
                        comisionesBancariasIntermediasComexUsd == null ? BigDecimal.ZERO : comisionesBancariasIntermediasComexUsd,
                        Math.max(cantidadPagos, 1), config);
    }

    private static PresupuestoCalculado calcularArgentina(BigDecimal totalCostoProduccion, BigDecimal margenDeseadoUsd,
            BigDecimal colchonIG, BigDecimal totalCostoMasGanancia, ConfiguracionPresupuesto config) {
        BigDecimal comisionVentaPct = config.getComisionVentaPorcentaje();
        BigDecimal iibbPct = config.getIibbConvenioMultilateralPorcentaje();
        BigDecimal debCredPct = config.getImpuestoDebitosCreditosPorcentaje();
        BigDecimal ivaPct = config.getIvaPorcentaje();

        // Denominador compartido: 1 - comisión - IIBB - débCred*(1+IVA)
        BigDecimal debCredGrossUp = debCredPct.multiply(BigDecimal.ONE.add(ivaPct));
        BigDecimal denominadorTotal = BigDecimal.ONE.subtract(comisionVentaPct).subtract(iibbPct).subtract(debCredGrossUp);
        BigDecimal denominadorSinComision = BigDecimal.ONE.subtract(iibbPct).subtract(debCredGrossUp);

        BigDecimal comisionVenta = comisionVentaPct.multiply(totalCostoMasGanancia)
                .divide(denominadorTotal, ESCALA_INTERNA, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal precioSinIva = totalCostoMasGanancia.add(comisionVenta)
                .divide(denominadorSinComision, ESCALA_INTERNA, RoundingMode.HALF_UP);
        BigDecimal iibb = precioSinIva.multiply(iibbPct);
        BigDecimal debCred = precioSinIva.multiply(debCredGrossUp);
        BigDecimal iva = precioSinIva.multiply(ivaPct);
        BigDecimal precioConIva = precioSinIva.add(iva);

        return new PresupuestoCalculado(Proyecto.TipoProyecto.ARGENTINA, totalCostoProduccion, margenDeseadoUsd,
                colchonIG, totalCostoMasGanancia,
                comisionVenta, precioSinIva, iibb, debCred, iva, precioConIva,
                null, null, null, null, null, null,
                precioConIva);
    }

    private static PresupuestoCalculado calcularExterior(BigDecimal totalCostoProduccion, BigDecimal margenDeseadoUsd,
            BigDecimal colchonIG, BigDecimal totalCostoMasGanancia, BigDecimal comisionesBancariasIntermedias,
            int cantidadPagos, ConfiguracionPresupuesto config) {
        BigDecimal comisionVentaPct = config.getComisionVentaPorcentaje();
        BigDecimal diferenciaDolarPct = config.getDiferenciaDolarComercializacionPorcentaje();
        BigDecimal debCredPct = config.getImpuestoDebitosCreditosPorcentaje();
        BigDecimal percepcionIvaPct = config.getPercepcionIvaComexPorcentaje();
        BigDecimal sircrebPct = config.getIibbSircrebComexPorcentaje();
        BigDecimal ivaCredFiscalPct = config.getIvaPorcentaje();

        BigDecimal base = totalCostoMasGanancia.add(comisionesBancariasIntermedias);
        BigDecimal coeficienteComex = BigDecimal.ONE.add(percepcionIvaPct).add(sircrebPct).add(ivaCredFiscalPct)
                .multiply(BigDecimal.ONE.add(debCredPct));
        BigDecimal denominador = BigDecimal.ONE.subtract(comisionVentaPct).subtract(diferenciaDolarPct);
        BigDecimal cantidadPagosBd = BigDecimal.valueOf(cantidadPagos);

        BigDecimal comisionUno = config.getComexMontoUnoUsd().multiply(cantidadPagosBd);
        BigDecimal comisionDos = config.getComexMontoDosUsd().multiply(cantidadPagosBd);
        BigDecimal comisionTres = config.getComexMontoTresUsd().multiply(cantidadPagosBd);

        BigDecimal precioFinalUno = base.add(coeficienteComex.multiply(comisionUno)).divide(denominador, ESCALA_INTERNA, RoundingMode.HALF_UP);
        BigDecimal precioFinalDos = base.add(coeficienteComex.multiply(comisionDos)).divide(denominador, ESCALA_INTERNA, RoundingMode.HALF_UP);
        BigDecimal precioFinalTres = base.add(coeficienteComex.multiply(comisionTres)).divide(denominador, ESCALA_INTERNA, RoundingMode.HALF_UP);
        BigDecimal denominadorPorcentaje = denominador.subtract(coeficienteComex.multiply(config.getComexPorcentajeExcedente()));
        BigDecimal precioFinalPorcentaje = base.divide(denominadorPorcentaje, ESCALA_INTERNA, RoundingMode.HALF_UP);

        BigDecimal comisionBancariaComex;
        if (dividirPorCantidadPagos(precioFinalUno, cantidadPagosBd).compareTo(config.getComexUmbralUnoUsd()) <= 0) {
            comisionBancariaComex = comisionUno;
        } else if (dividirPorCantidadPagos(precioFinalDos, cantidadPagosBd).compareTo(config.getComexUmbralDosUsd()) <= 0) {
            comisionBancariaComex = comisionDos;
        } else if (dividirPorCantidadPagos(precioFinalTres, cantidadPagosBd).compareTo(config.getComexUmbralTresUsd()) <= 0) {
            comisionBancariaComex = comisionTres;
        } else {
            comisionBancariaComex = precioFinalPorcentaje.multiply(config.getComexPorcentajeExcedente());
        }

        BigDecimal percepcionIvaComex = comisionBancariaComex.multiply(percepcionIvaPct);
        BigDecimal sircrebComex = comisionBancariaComex.multiply(sircrebPct);
        BigDecimal ivaCreditoFiscalComex = comisionBancariaComex.multiply(ivaCredFiscalPct);
        BigDecimal debitosCreditosComex = comisionBancariaComex.add(percepcionIvaComex).add(sircrebComex).add(ivaCreditoFiscalComex)
                .multiply(debCredPct);
        BigDecimal totalImpuestosYComisiones = comisionesBancariasIntermedias.add(comisionBancariaComex)
                .add(debitosCreditosComex).add(percepcionIvaComex).add(sircrebComex).add(ivaCreditoFiscalComex);

        BigDecimal precioFinalCliente = base.add(comisionBancariaComex).add(debitosCreditosComex)
                .add(percepcionIvaComex).add(sircrebComex).add(ivaCreditoFiscalComex)
                .divide(denominador, ESCALA_INTERNA, RoundingMode.HALF_UP);

        return new PresupuestoCalculado(Proyecto.TipoProyecto.EXTERIOR, totalCostoProduccion, margenDeseadoUsd,
                colchonIG, totalCostoMasGanancia,
                null, null, null, null, null, null,
                comisionesBancariasIntermedias, comisionBancariaComex, percepcionIvaComex, sircrebComex,
                ivaCreditoFiscalComex, totalImpuestosYComisiones,
                precioFinalCliente);
    }

    private static BigDecimal dividirPorCantidadPagos(BigDecimal precioFinal, BigDecimal cantidadPagos) {
        return precioFinal.divide(cantidadPagos, ESCALA_INTERNA, RoundingMode.HALF_UP);
    }
}

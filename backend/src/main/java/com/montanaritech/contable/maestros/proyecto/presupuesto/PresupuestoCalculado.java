package com.montanaritech.contable.maestros.proyecto.presupuesto;

import com.montanaritech.contable.maestros.proyecto.Proyecto;
import java.math.BigDecimal;

/**
 * Resultado del waterfall de presupuesto (F2.6), en USD. Los campos
 * específicos de cada {@code tipoProyecto} quedan en {@code null} para el
 * otro tipo (dos cascadas de impuestos/comisiones distintas, no una unión
 * artificial). Ningún valor acá se persiste: se recalcula on-demand.
 */
public record PresupuestoCalculado(
        Proyecto.TipoProyecto tipoProyecto,
        BigDecimal totalCostoProduccion,
        BigDecimal margenDeseadoUsd,
        BigDecimal colchonImpuestoGanancias,
        BigDecimal totalCostoMasGanancia,

        // Argentina
        BigDecimal comisionVenta,
        BigDecimal precioSinIva,
        BigDecimal iibbConvenioMultilateral,
        BigDecimal impuestoDebitosCreditos,
        BigDecimal ivaDebitoFiscal,
        BigDecimal precioConIva,

        // Exterior
        BigDecimal comisionesBancariasIntermediasComex,
        BigDecimal comisionBancariaComex,
        BigDecimal percepcionIvaComex,
        BigDecimal iibbSircrebComex,
        BigDecimal ivaCreditoFiscalComex,
        BigDecimal totalImpuestosYComisionesBancariasComex,

        BigDecimal precioFinalCliente
) {}

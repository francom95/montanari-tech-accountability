package com.montanaritech.contable.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Los 12 indicadores del dashboard (F7.5) para un período (anio/mes), cada
 * uno con su ruta de drill-down. {@code alertas} es un slot vacío a
 * propósito — se conecta con F9.1.
 */
public record DashboardResponse(
        int anio,
        int mes,
        IndicadorMonto resultadoMensual,
        IndicadorMonto ventasDelPeriodo,
        IndicadorMonto cobrosDelPeriodo,
        IndicadorMonto cuentasPorCobrar,
        IndicadorMonto cuentasPorPagar,
        IndicadorMonto obligacionesProximas,
        IndicadorMonto saldoCaja,
        IndicadorMonto saldoBanco,
        IndicadorMonto margenEstimado,
        IndicadorMonto egresosProyectados,
        VencimientoImpuesto proximoVencimientoIva,
        VencimientoImpuesto proximoVencimientoIibb,
        List<String> alertas) {

    public record IndicadorMonto(BigDecimal valorArs, String ruta) {}

    public record VencimientoImpuesto(LocalDate fechaVencimiento, BigDecimal saldoAPagarArs, String ruta) {}
}

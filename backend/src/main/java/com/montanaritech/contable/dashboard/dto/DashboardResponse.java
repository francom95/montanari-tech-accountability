package com.montanaritech.contable.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Los 12 indicadores del dashboard (F7.5) para un período (anio/mes), cada
 * uno con su ruta de drill-down. {@code alertas} quedó como lo que ya era
 * antes de F9.1: advertencias ad-hoc del propio cálculo del período (hoy,
 * cuentas en moneda extranjera excluidas del saldo de caja/banco) — no es
 * el sistema real de alertas. Las alertas de F9.1 (vencimientos, saldos
 * bajos, etc., con lectura por usuario y auto-resolución) viven en
 * {@code GET /api/v1/alertas}; el widget de alertas del dashboard las
 * consume directamente de ahí, sin pasar por este DTO.
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

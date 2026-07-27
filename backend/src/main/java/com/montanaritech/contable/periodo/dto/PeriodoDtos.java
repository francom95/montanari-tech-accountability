package com.montanaritech.contable.periodo.dto;

import com.montanaritech.contable.bancos.conciliacion.dto.ConciliacionResumenResponse;
import com.montanaritech.contable.periodo.EstadoPeriodo;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

/** DTOs de Período (F9.3), agrupados por ser records chicos de un mismo flujo — mismo criterio que {@code LiquidacionIvaDtos}. */
public final class PeriodoDtos {

    private PeriodoDtos() {
    }

    public record PeriodoResponse(
            Long id,
            Integer anio,
            Integer mes,
            EstadoPeriodo estado,
            String motivoCierre,
            String motivoReapertura) {
    }

    public record GenerarAutomaticosResponse(int generados) {
    }

    /** Motivo obligatorio para cerrar/reabrir — mismo molde que {@code AsientoAnularRequest} y afines. */
    public record MotivoRequest(@NotBlank String motivo) {
    }

    public record LiquidacionResumenItem(
            String tipo,
            Long id,
            String estado,
            BigDecimal saldoAPagar) {
    }

    public record PeriodoResumenResponse(
            Long periodoId,
            Integer anio,
            Integer mes,
            EstadoPeriodo estado,
            List<LiquidacionResumenItem> liquidaciones,
            List<ConciliacionResumenResponse> conciliaciones) {
    }
}

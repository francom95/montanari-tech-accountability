package com.montanaritech.contable.dashboard.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class ConfiguracionDashboardDtos {

    private ConfiguracionDashboardDtos() {
    }

    public record Request(
            @NotNull @Min(1) @Max(28) Integer diaVencimientoIva,
            @NotNull @Min(1) @Max(28) Integer diaVencimientoIibb,
            @NotNull @Min(1) Integer ventanaObligacionesDias) {}

    public record Response(Integer diaVencimientoIva, Integer diaVencimientoIibb, Integer ventanaObligacionesDias) {}
}

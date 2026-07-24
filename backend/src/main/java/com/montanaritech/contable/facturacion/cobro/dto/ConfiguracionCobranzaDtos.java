package com.montanaritech.contable.facturacion.cobro.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public final class ConfiguracionCobranzaDtos {

    private ConfiguracionCobranzaDtos() {
    }

    public record Request(@NotNull Integer diasGraciaMora, @NotNull BigDecimal tasaMoraDiariaPorcentaje) {}

    public record Response(Integer diasGraciaMora, BigDecimal tasaMoraDiariaPorcentaje) {}
}

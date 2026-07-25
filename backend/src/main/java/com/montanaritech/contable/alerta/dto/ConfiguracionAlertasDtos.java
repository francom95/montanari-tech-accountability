package com.montanaritech.contable.alerta.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class ConfiguracionAlertasDtos {

    private ConfiguracionAlertasDtos() {
    }

    public record Request(@NotNull @Min(1) Integer diasAnticipacion, @NotNull @Min(0) Integer diasAtrasoCxc) {}

    public record Response(Integer diasAnticipacion, Integer diasAtrasoCxc) {}
}

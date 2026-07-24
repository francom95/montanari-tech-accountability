package com.montanaritech.contable.maestros.tipocambio.dto;

public final class ConfiguracionTipoCambioDtos {

    private ConfiguracionTipoCambioDtos() {
    }

    public record Request(String criterioPorDefecto) {}

    public record Response(String criterioPorDefecto) {}
}

package com.montanaritech.contable.maestros.proyecto.presupuesto.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public final class ConfiguracionPresupuestoDtos {

    private ConfiguracionPresupuestoDtos() {
    }

    public record Request(
            @NotNull BigDecimal comisionVentaPorcentaje,
            @NotNull BigDecimal colchonImpuestoGananciasPorcentaje,
            @NotNull BigDecimal iibbConvenioMultilateralPorcentaje,
            @NotNull BigDecimal impuestoDebitosCreditosPorcentaje,
            @NotNull BigDecimal ivaPorcentaje,
            @NotNull BigDecimal diferenciaDolarComercializacionPorcentaje,
            @NotNull BigDecimal percepcionIvaComexPorcentaje,
            @NotNull BigDecimal iibbSircrebComexPorcentaje,
            @NotNull BigDecimal comexUmbralUnoUsd,
            @NotNull BigDecimal comexMontoUnoUsd,
            @NotNull BigDecimal comexUmbralDosUsd,
            @NotNull BigDecimal comexMontoDosUsd,
            @NotNull BigDecimal comexUmbralTresUsd,
            @NotNull BigDecimal comexMontoTresUsd,
            @NotNull BigDecimal comexPorcentajeExcedente) {}

    public record Response(
            BigDecimal comisionVentaPorcentaje,
            BigDecimal colchonImpuestoGananciasPorcentaje,
            BigDecimal iibbConvenioMultilateralPorcentaje,
            BigDecimal impuestoDebitosCreditosPorcentaje,
            BigDecimal ivaPorcentaje,
            BigDecimal diferenciaDolarComercializacionPorcentaje,
            BigDecimal percepcionIvaComexPorcentaje,
            BigDecimal iibbSircrebComexPorcentaje,
            BigDecimal comexUmbralUnoUsd,
            BigDecimal comexMontoUnoUsd,
            BigDecimal comexUmbralDosUsd,
            BigDecimal comexMontoDosUsd,
            BigDecimal comexUmbralTresUsd,
            BigDecimal comexMontoTresUsd,
            BigDecimal comexPorcentajeExcedente) {}
}

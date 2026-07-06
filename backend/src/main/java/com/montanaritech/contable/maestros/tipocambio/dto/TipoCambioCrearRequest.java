package com.montanaritech.contable.maestros.tipocambio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TipoCambioCrearRequest(
        @NotNull LocalDate fecha,
        @NotNull Long monedaId,
        @NotNull String criterio,
        @NotNull @DecimalMin("0") BigDecimal valorCompra,
        @NotNull @DecimalMin("0") BigDecimal valorVenta,
        String fuente,
        String observaciones
) {
}

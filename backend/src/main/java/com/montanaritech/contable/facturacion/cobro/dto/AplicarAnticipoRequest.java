package com.montanaritech.contable.facturacion.cobro.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AplicarAnticipoRequest(
        @NotNull Long facturaVentaId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal monto,
        @NotNull LocalDate fecha
) {}

package com.montanaritech.contable.facturacion.cobro.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CobroImputacionRequest(
        @NotNull Long facturaVentaId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal montoImputadoOriginal,
        @DecimalMin(value = "0.00") BigDecimal recargoMoraOriginal
) {}

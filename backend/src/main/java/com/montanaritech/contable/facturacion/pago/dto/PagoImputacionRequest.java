package com.montanaritech.contable.facturacion.pago.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PagoImputacionRequest(
        @NotNull Long facturaCompraId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal montoImputadoOriginal
) {}

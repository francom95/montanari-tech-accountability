package com.montanaritech.contable.maestros.tipocambio.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
public record TipoCambioEditarRequest(
        @NotNull @DecimalMin("0") BigDecimal valorCompra,
        @NotNull @DecimalMin("0") BigDecimal valorVenta,
        String fuente,
        String observaciones
) {}

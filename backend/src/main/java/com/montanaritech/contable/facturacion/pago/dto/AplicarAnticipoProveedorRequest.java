package com.montanaritech.contable.facturacion.pago.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AplicarAnticipoProveedorRequest(
        @NotNull Long facturaCompraId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal monto,
        @NotNull LocalDate fecha
) {}

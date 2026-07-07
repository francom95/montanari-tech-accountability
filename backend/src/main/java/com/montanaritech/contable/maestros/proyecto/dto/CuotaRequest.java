package com.montanaritech.contable.maestros.proyecto.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CuotaRequest(
        @NotNull LocalDate fechaEstimadaCobro,
        @NotNull @DecimalMin(value = "0.00") BigDecimal importe
) {}

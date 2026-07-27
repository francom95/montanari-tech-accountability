package com.montanaritech.contable.maestros.proyecto.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** {@code fechaEstimadaCobro} es opcional desde F10.2: la carga histórica no siempre trae fecha pactada. */
public record CuotaRequest(
        LocalDate fechaEstimadaCobro,
        @NotNull @DecimalMin(value = "0.00") BigDecimal importe
) {}

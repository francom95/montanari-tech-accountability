package com.montanaritech.contable.maestros.proyecto.comision.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ComisionProyectoEditarRequest(
        @NotNull Long comisionistaId,
        @NotNull @DecimalMin(value = "0.00") @DecimalMax(value = "100.00") BigDecimal porcentajeComision,
        @NotBlank String baseCalculo,
        @NotNull Long monedaId,
        BigDecimal importeFinal,
        String estadoPago,
        LocalDate fechaEstimadaPago,
        String observaciones
) {}

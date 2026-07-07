package com.montanaritech.contable.maestros.proyecto.etapa.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record EtapaEditarRequest(
        @NotBlank String nombre,
        String descripcion,
        String estado,
        LocalDate fechaInicio,
        LocalDate fechaEstimadaFin,
        @Min(0) @Max(100) Integer porcentajeAvance,
        @DecimalMin(value = "0.00") BigDecimal montoPresupuestado,
        @DecimalMin(value = "0.00") BigDecimal costosEstimados,
        Set<Long> proveedoresIds,
        @DecimalMin(value = "0.00") BigDecimal pagosPrevistos,
        @DecimalMin(value = "0.00") BigDecimal cobrosPrevistos,
        String observaciones
) {}

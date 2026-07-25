package com.montanaritech.contable.inversion.dto;

import com.montanaritech.contable.inversion.TipoMovimientoInversion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoInversionCrearRequest(
        @NotNull Long inversionId,
        @NotNull TipoMovimientoInversion tipo,
        @NotNull LocalDate fecha,
        @NotNull @DecimalMin(value = "0.01") BigDecimal montoAplicado,
        @NotNull @DecimalMin(value = "0.000001") BigDecimal cuotapartes,
        @NotNull @DecimalMin(value = "0.000001") BigDecimal valorCuotaparte,
        LocalDate fechaLiquidacion,
        String observaciones
) {}

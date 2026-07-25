package com.montanaritech.contable.inversion.dto;

import com.montanaritech.contable.inversion.TipoMovimientoInversion;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoInversionResponse(
        Long id,
        Long inversionId,
        TipoMovimientoInversion tipo,
        LocalDate fecha,
        BigDecimal montoAplicado,
        BigDecimal cuotapartes,
        BigDecimal valorCuotaparte,
        LocalDate fechaLiquidacion,
        Long movimientoBancarioId,
        String observaciones
) {}

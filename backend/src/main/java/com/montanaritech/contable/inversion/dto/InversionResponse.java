package com.montanaritech.contable.inversion.dto;

import com.montanaritech.contable.inversion.EstadoInversion;
import com.montanaritech.contable.inversion.TipoVinculoInversion;
import java.math.BigDecimal;

/**
 * {@code cuotapartesAcumuladas}, {@code montoNetoAplicado}, {@code valuacionActual}
 * y {@code rendimiento} son calculados on-the-fly desde {@code MovimientoInversion}
 * (no hay columnas persistidas — evita staleness, ver {@code InversionService}).
 */
public record InversionResponse(
        Long id,
        String instrumento,
        Long cuentaOrigenId,
        String cuentaOrigenAlias,
        String objetivoDelDinero,
        TipoVinculoInversion vinculoTipo,
        Long vinculoRefId,
        EstadoInversion estado,
        boolean activo,
        BigDecimal cuotapartesAcumuladas,
        BigDecimal montoNetoAplicado,
        BigDecimal valuacionActual,
        BigDecimal rendimiento
) {}

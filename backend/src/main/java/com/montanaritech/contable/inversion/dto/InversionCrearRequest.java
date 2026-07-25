package com.montanaritech.contable.inversion.dto;

import com.montanaritech.contable.inversion.TipoVinculoInversion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InversionCrearRequest(
        @NotBlank String instrumento,
        @NotNull Long cuentaOrigenId,
        String objetivoDelDinero,
        TipoVinculoInversion vinculoTipo,
        Long vinculoRefId
) {}

package com.montanaritech.contable.inversion.dto;

import com.montanaritech.contable.inversion.EstadoInversion;
import com.montanaritech.contable.inversion.TipoVinculoInversion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InversionEditarRequest(
        @NotBlank String instrumento,
        @NotNull Long cuentaOrigenId,
        String objetivoDelDinero,
        TipoVinculoInversion vinculoTipo,
        Long vinculoRefId,
        @NotNull EstadoInversion estado
) {}

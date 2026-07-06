package com.montanaritech.contable.maestros.jurisdiccion.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
public record JurisdiccionEditarRequest(
        @NotBlank String nombre,
        @NotNull BigDecimal alicuotaIIBB
) {}

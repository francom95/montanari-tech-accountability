package com.montanaritech.contable.maestros.jurisdiccion.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
public record JurisdiccionCrearRequest(
        @NotBlank String nombre,
        @NotBlank String codigo,
        @NotNull BigDecimal alicuotaIIBB
) {}

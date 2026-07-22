package com.montanaritech.contable.facturacion.cobro.dto;

import jakarta.validation.constraints.NotBlank;

public record CobroAnularRequest(@NotBlank String motivo) {}

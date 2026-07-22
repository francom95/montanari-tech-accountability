package com.montanaritech.contable.bancos.movimientobancario.dto;

import jakarta.validation.constraints.NotBlank;

public record DescartarMovimientoBancarioRequest(@NotBlank String motivo) {}

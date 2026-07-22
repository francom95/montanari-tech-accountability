package com.montanaritech.contable.facturacion.pago.dto;

import jakarta.validation.constraints.NotBlank;

public record PagoAnularRequest(@NotBlank String motivo) {}

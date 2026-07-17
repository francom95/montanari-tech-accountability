package com.montanaritech.contable.facturacion.facturaventa.dto;

import jakarta.validation.constraints.NotBlank;

public record FacturaVentaAnularRequest(@NotBlank String motivo) {}

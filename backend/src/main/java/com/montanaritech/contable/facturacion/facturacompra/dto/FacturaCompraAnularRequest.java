package com.montanaritech.contable.facturacion.facturacompra.dto;

import jakarta.validation.constraints.NotBlank;

public record FacturaCompraAnularRequest(@NotBlank String motivo) {}

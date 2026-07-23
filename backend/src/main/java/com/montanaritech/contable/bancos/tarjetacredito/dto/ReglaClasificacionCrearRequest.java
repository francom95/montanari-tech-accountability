package com.montanaritech.contable.bancos.tarjetacredito.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReglaClasificacionCrearRequest(
        @NotBlank String patron,
        @NotNull Long cuentaContableId,
        Long proveedorId,
        Long proyectoId,
        Long conceptoId
) {}

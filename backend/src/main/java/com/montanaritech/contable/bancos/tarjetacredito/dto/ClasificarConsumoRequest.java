package com.montanaritech.contable.bancos.tarjetacredito.dto;

import jakarta.validation.constraints.NotNull;

public record ClasificarConsumoRequest(
        @NotNull Long cuentaContableId,
        Long proveedorId,
        Long proyectoId,
        Long conceptoId
) {}

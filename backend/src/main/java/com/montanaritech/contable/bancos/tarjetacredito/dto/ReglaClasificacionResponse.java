package com.montanaritech.contable.bancos.tarjetacredito.dto;

public record ReglaClasificacionResponse(
        Long id,
        String patron,
        Long cuentaContableId,
        String cuentaContableCodigo,
        String cuentaContableNombre,
        Long proveedorId,
        String proveedorNombre,
        Long proyectoId,
        String proyectoNombre,
        Long conceptoId,
        String conceptoNombre,
        boolean activo
) {}

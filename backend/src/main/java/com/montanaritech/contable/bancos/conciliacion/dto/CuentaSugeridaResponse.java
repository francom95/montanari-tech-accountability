package com.montanaritech.contable.bancos.conciliacion.dto;

/** Sugerencia de imputación rápida por descripción (F5.3), cuando no hay match contra un asiento existente. */
public record CuentaSugeridaResponse(
        Long cuentaContableId,
        String cuentaContableCodigo,
        String cuentaContableNombre,
        String concepto
) {}

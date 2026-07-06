package com.montanaritech.contable.maestros.moneda.dto;

public record MonedaResponse(
        Long id,
        String codigo,
        String nombre,
        String simbolo,
        boolean activo
) {
}

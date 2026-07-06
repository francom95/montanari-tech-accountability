package com.montanaritech.contable.maestros.tipocosto.dto;
public record TipoCostoResponse(
        Long id,
        String nombre,
        String descripcion,
        boolean activo
) {}

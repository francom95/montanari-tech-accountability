package com.montanaritech.contable.maestros.rubro.dto;
public record RubroResponse(
        Long id,
        String nombre,
        Long categoriaId,
        int orden,
        boolean activo
) {}

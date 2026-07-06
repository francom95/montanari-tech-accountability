package com.montanaritech.contable.maestros.categoria.dto;
public record CategoriaResponse(
        Long id,
        String nombre,
        String descripcion,
        String tipo,
        boolean activo
) {}

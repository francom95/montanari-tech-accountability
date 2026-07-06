package com.montanaritech.contable.maestros.categoria.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record CategoriaEditarRequest(
        @NotBlank String nombre,
        String descripcion,
        @NotNull String tipo
) {}

package com.montanaritech.contable.maestros.rubro.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record RubroCrearRequest(
        @NotBlank String nombre,
        @NotNull Long categoriaId,
        int orden
) {}

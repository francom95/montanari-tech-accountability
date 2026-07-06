package com.montanaritech.contable.maestros.tipocosto.dto;
import jakarta.validation.constraints.NotBlank;
public record TipoCostoEditarRequest(
        @NotBlank String nombre,
        String descripcion
) {}

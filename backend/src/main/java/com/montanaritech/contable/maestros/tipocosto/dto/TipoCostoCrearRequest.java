package com.montanaritech.contable.maestros.tipocosto.dto;
import jakarta.validation.constraints.NotBlank;
public record TipoCostoCrearRequest(
        @NotBlank String nombre,
        String descripcion
) {}

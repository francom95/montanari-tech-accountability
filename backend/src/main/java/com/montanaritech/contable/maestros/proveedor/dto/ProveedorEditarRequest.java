package com.montanaritech.contable.maestros.proveedor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record ProveedorEditarRequest(
        @NotBlank String nombre,
        @NotNull Long jurisdiccionId,
        Long monedaHabitualId,
        Set<Long> tiposCostoIds,
        String contacto,
        String email,
        String telefono
) {}

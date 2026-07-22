package com.montanaritech.contable.maestros.proveedor.dto;

import com.montanaritech.contable.common.validation.CuitValido;
import com.montanaritech.contable.maestros.proveedor.CondicionIva;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record ProveedorCrearRequest(
        @NotBlank String nombre,
        @NotBlank @CuitValido String cuit,
        @NotNull Long jurisdiccionId,
        Long monedaHabitualId,
        Set<Long> tiposCostoIds,
        String contacto,
        String email,
        String telefono,
        CondicionIva condicionIva,
        Long cuentaCxpId
) {}

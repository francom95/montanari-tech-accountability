package com.montanaritech.contable.maestros.proveedor.dto;

import com.montanaritech.contable.maestros.proveedor.CondicionIva;
import java.util.Set;

public record ProveedorResponse(
        Long id,
        String nombre,
        String cuit,
        Long jurisdiccionId,
        String jurisdiccionNombre,
        Long monedaHabitualId,
        String monedaHabitualCodigo,
        Set<TipoCostoDto> tiposCosto,
        String contacto,
        String email,
        String telefono,
        CondicionIva condicionIva,
        Long cuentaCxpId,
        String cuentaCxpCodigo,
        boolean activo
) {
    public record TipoCostoDto(Long id, String nombre) {}
}

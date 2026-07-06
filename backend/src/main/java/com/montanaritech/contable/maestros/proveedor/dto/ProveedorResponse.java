package com.montanaritech.contable.maestros.proveedor.dto;

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
        boolean activo
) {
    public record TipoCostoDto(Long id, String nombre) {}
}

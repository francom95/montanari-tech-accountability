package com.montanaritech.contable.maestros.proyecto.etapa.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record EtapaResponse(
        Long id,
        Long proyectoId,
        String nombre,
        String descripcion,
        String estado,
        LocalDate fechaInicio,
        LocalDate fechaEstimadaFin,
        Integer porcentajeAvance,
        BigDecimal montoPresupuestado,
        BigDecimal costosEstimados,
        Set<ProveedorDto> proveedores,
        BigDecimal pagosPrevistos,
        BigDecimal cobrosPrevistos,
        String observaciones,
        boolean activo
) {
    public record ProveedorDto(Long id, String nombre) {}
}

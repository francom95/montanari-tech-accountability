package com.montanaritech.contable.maestros.cliente.dto;

public record ClienteResponse(
        Long id,
        String nombre,
        String cuit,
        Long jurisdiccionId,
        String jurisdiccionNombre,
        String contacto,
        String email,
        String telefono,
        Long cuentaCxcId,
        String cuentaCxcCodigo,
        boolean activo
) {}

package com.montanaritech.contable.maestros.comisionista.dto;

public record ComisionistaResponse(
        Long id,
        String nombre,
        String cuit,
        String contacto,
        String email,
        String telefono,
        boolean activo
) {}

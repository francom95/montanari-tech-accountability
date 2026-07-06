package com.montanaritech.contable.maestros.jurisdiccion.dto;
import java.math.BigDecimal;
public record JurisdiccionResponse(
        Long id,
        String nombre,
        String codigo,
        BigDecimal alicuotaIIBB,
        boolean activo
) {}

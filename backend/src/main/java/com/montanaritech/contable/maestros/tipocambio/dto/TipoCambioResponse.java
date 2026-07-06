package com.montanaritech.contable.maestros.tipocambio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TipoCambioResponse(
        Long id,
        LocalDate fecha,
        Long monedaId,
        String criterio,
        BigDecimal valorCompra,
        BigDecimal valorVenta,
        String fuente,
        String observaciones,
        boolean activo
) {
}

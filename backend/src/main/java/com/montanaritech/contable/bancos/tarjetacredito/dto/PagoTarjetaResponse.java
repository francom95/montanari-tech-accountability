package com.montanaritech.contable.bancos.tarjetacredito.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoTarjetaResponse(
        Long id,
        Long tarjetaCreditoId,
        String tarjetaCreditoEntidad,
        LocalDate fecha,
        BigDecimal importe,
        Long monedaId,
        String monedaCodigo,
        BigDecimal tipoCambio,
        BigDecimal importeArs,
        String estado,
        Long asientoId,
        Long asientoNumero,
        String observaciones
) {}

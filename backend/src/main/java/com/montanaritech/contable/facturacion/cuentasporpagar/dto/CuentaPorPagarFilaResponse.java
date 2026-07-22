package com.montanaritech.contable.facturacion.cuentasporpagar.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuentaPorPagarFilaResponse(
        Long facturaCompraId,
        Long proveedorId,
        String proveedorNombre,
        Long proyectoId,
        String proyectoNombre,
        String numero,
        LocalDate fecha,
        LocalDate fechaVencimiento,
        Long monedaId,
        String monedaCodigo,
        BigDecimal total,
        BigDecimal totalArs,
        BigDecimal saldo,
        BigDecimal saldoArs,
        String estadoVencimiento
) {}

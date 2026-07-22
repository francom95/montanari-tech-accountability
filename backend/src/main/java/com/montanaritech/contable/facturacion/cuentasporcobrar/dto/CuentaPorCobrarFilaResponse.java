package com.montanaritech.contable.facturacion.cuentasporcobrar.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuentaPorCobrarFilaResponse(
        Long facturaVentaId,
        Long clienteId,
        String clienteNombre,
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

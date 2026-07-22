package com.montanaritech.contable.facturacion.cobro.dto;

import java.math.BigDecimal;

/** Saldo pendiente de cobro de una factura de venta (F4.4), calculado en el momento — no persistido. */
public record SaldoFacturaResponse(
        BigDecimal total,
        BigDecimal imputado,
        BigDecimal saldo,
        BigDecimal totalArs,
        BigDecimal imputadoArs,
        BigDecimal saldoArs
) {}

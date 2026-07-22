package com.montanaritech.contable.facturacion.pago.dto;

import java.math.BigDecimal;

/** Saldo pendiente de pago de una factura de compra (F4.4), calculado en el momento — no persistido. */
public record SaldoFacturaCompraResponse(
        BigDecimal total,
        BigDecimal imputado,
        BigDecimal saldo,
        BigDecimal totalArs,
        BigDecimal imputadoArs,
        BigDecimal saldoArs
) {}

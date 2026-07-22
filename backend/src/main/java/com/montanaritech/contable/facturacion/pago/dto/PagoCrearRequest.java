package com.montanaritech.contable.facturacion.pago.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Crea siempre un BORRADOR. {@code total} es el bruto pagado en moneda
 * original; {@code lineas} son las imputaciones contra facturas de compra
 * (Σ lineas ≤ total — el remanente es anticipo, F4.1 §6.5).
 */
public record PagoCrearRequest(
        @NotNull Long proveedorId,
        @NotNull LocalDate fecha,
        @NotNull Long monedaId,
        @NotNull BigDecimal tipoCambio,
        @NotNull Long cuentaBancariaId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal total,
        String observaciones,
        @Valid List<PagoImputacionRequest> lineas
) {}

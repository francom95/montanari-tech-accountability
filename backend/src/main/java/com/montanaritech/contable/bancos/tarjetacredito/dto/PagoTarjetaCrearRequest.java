package com.montanaritech.contable.bancos.tarjetacredito.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Pago del resumen de tarjeta (F5.4 §3): {@code importe} puede ser parcial (pago mínimo) — el resto queda como saldo pendiente. */
public record PagoTarjetaCrearRequest(
        @NotNull Long tarjetaCreditoId,
        @NotNull LocalDate fecha,
        @NotNull BigDecimal importe,
        @NotNull Long monedaId,
        @NotNull BigDecimal tipoCambio,
        String observaciones
) {}

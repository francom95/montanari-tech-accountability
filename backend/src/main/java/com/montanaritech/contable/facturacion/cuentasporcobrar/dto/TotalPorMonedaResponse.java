package com.montanaritech.contable.facturacion.cuentasporcobrar.dto;

import java.math.BigDecimal;

public record TotalPorMonedaResponse(Long monedaId, String monedaCodigo, BigDecimal totalSaldo, BigDecimal totalSaldoArs) {}

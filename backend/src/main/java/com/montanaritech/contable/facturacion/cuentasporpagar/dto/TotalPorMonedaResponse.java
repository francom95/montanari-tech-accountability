package com.montanaritech.contable.facturacion.cuentasporpagar.dto;

import java.math.BigDecimal;

public record TotalPorMonedaResponse(Long monedaId, String monedaCodigo, BigDecimal totalSaldo, BigDecimal totalSaldoArs) {}

package com.montanaritech.contable.facturacion.cuentasporpagar.dto;

import java.util.List;

public record CuentaPorPagarResponse(List<CuentaPorPagarFilaResponse> filas, List<TotalPorMonedaResponse> totalesPorMoneda) {}

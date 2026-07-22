package com.montanaritech.contable.facturacion.cuentasporcobrar.dto;

import java.util.List;

public record CuentaPorCobrarResponse(List<CuentaPorCobrarFilaResponse> filas, List<TotalPorMonedaResponse> totalesPorMoneda) {}

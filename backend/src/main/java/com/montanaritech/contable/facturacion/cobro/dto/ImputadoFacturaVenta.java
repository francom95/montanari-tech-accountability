package com.montanaritech.contable.facturacion.cobro.dto;

import java.math.BigDecimal;

/** Proyección agregada (F4.5): cuánto se imputó/aplicó contra una factura de venta puntual, sumado en una sola consulta para N facturas a la vez. */
public record ImputadoFacturaVenta(Long facturaVentaId, BigDecimal imputado, BigDecimal imputadoArs) {}

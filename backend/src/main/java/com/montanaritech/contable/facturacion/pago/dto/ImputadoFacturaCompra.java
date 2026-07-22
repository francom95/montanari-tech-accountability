package com.montanaritech.contable.facturacion.pago.dto;

import java.math.BigDecimal;

/** Proyección agregada (F4.5): cuánto se imputó/aplicó contra una factura de compra puntual, sumado en una sola consulta para N facturas a la vez. */
public record ImputadoFacturaCompra(Long facturaCompraId, BigDecimal imputado, BigDecimal imputadoArs) {}

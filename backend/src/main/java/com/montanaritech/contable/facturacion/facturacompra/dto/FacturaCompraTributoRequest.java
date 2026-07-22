package com.montanaritech.contable.facturacion.facturacompra.dto;

import com.montanaritech.contable.facturacion.comprobantetributo.TipoTributo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Percepción sufrida en la factura de compra (F4.1 §5). Solo
 * {@code PERCEPCION_IVA}/{@code PERCEPCION_IIBB} generan línea de asiento
 * ({@code FacturaCompraService} rechaza otros tipos con {@code
 * TRIBUTO_NO_APLICABLE_A_COMPRA} — Montanari no retiene, checkpoint F4.1 #3).
 */
public record FacturaCompraTributoRequest(
        @NotNull TipoTributo tipo,
        Long jurisdiccionId,
        BigDecimal base,
        BigDecimal alicuota,
        @NotNull @DecimalMin(value = "0.01") BigDecimal importe
) {}

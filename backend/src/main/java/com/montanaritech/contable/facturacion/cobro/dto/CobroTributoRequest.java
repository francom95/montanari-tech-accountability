package com.montanaritech.contable.facturacion.cobro.dto;

import com.montanaritech.contable.facturacion.comprobantetributo.TipoTributo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Retención sufrida por el cobro (F4.1 §6.1). Solo {@code RETENCION_GANANCIAS}/
 * {@code RETENCION_IVA} generan línea de asiento — son los únicos con cuenta
 * mapeada en {@code ConceptoContable} ({@code RETENCION_GANANCIAS_SUFRIDA}/
 * {@code RETENCION_IVA_SUFRIDA}); {@code CobroService} rechaza cualquier otro
 * tipo con {@code TRIBUTO_NO_APLICABLE_A_COBRO}. {@code importe} está en la
 * moneda original del cobro (F4.1 §6.1: "round2(retención × TC_cobro)").
 */
public record CobroTributoRequest(
        @NotNull TipoTributo tipo,
        @NotNull @DecimalMin(value = "0.01") BigDecimal importe
) {}

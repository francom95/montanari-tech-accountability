package com.montanaritech.contable.maestros.concepto.dto;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
public record ConceptoCrearRequest(
        @NotBlank String nombre,
        String descripcion,
        String cuentaSugerida,
        String periodicidad,
        BigDecimal importe,
        Long monedaId
) {}

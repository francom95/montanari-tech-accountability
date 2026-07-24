package com.montanaritech.contable.maestros.concepto.dto;
import com.montanaritech.contable.maestros.concepto.Periodicidad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
public record ConceptoCrearRequest(
        @NotBlank String nombre,
        String descripcion,
        String cuentaSugerida,
        @NotNull Periodicidad periodicidad,
        BigDecimal importe,
        Long monedaId
) {}

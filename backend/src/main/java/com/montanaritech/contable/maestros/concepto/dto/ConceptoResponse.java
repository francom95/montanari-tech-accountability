package com.montanaritech.contable.maestros.concepto.dto;
import java.math.BigDecimal;
public record ConceptoResponse(
        Long id,
        String nombre,
        String descripcion,
        String cuentaSugerida,
        String periodicidad,
        BigDecimal importe,
        Long monedaId,
        boolean activo
) {}

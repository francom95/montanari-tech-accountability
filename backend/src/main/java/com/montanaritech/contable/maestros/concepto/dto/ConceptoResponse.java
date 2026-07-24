package com.montanaritech.contable.maestros.concepto.dto;
import com.montanaritech.contable.maestros.concepto.Periodicidad;
import java.math.BigDecimal;
public record ConceptoResponse(
        Long id,
        String nombre,
        String descripcion,
        String cuentaSugerida,
        Periodicidad periodicidad,
        BigDecimal importe,
        Long monedaId,
        boolean activo
) {}

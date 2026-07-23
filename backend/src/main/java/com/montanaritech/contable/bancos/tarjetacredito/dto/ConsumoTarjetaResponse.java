package com.montanaritech.contable.bancos.tarjetacredito.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConsumoTarjetaResponse(
        Long id,
        Long tarjetaCreditoId,
        LocalDate fecha,
        String descripcion,
        String referencia,
        BigDecimal importe,
        Long monedaId,
        String monedaCodigo,
        BigDecimal tipoCambio,
        BigDecimal importeArs,
        Long cuentaContableId,
        String cuentaContableCodigo,
        String cuentaContableNombre,
        Long proveedorId,
        String proveedorNombre,
        Long proyectoId,
        String proyectoNombre,
        Long conceptoId,
        String conceptoNombre
) {}

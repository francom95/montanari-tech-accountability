package com.montanaritech.contable.bancos.movimientobancario.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoBancarioResponse(
        Long id,
        Long cuentaBancariaId,
        String cuentaBancariaAlias,
        LocalDate fecha,
        String descripcion,
        BigDecimal importe,
        Long monedaId,
        String monedaCodigo,
        BigDecimal tipoCambio,
        BigDecimal importeArs,
        String referencia,
        String origenImportacion,
        Long cuentaContableSugeridaId,
        String cuentaContableSugeridaCodigo,
        String estado,
        Long asientoId,
        Long asientoNumero,
        String motivoDescarte,
        String observaciones
) {}

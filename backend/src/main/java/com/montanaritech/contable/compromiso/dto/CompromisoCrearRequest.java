package com.montanaritech.contable.compromiso.dto;

import com.montanaritech.contable.compromiso.TipoCompromiso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CompromisoCrearRequest(
        @NotBlank String concepto,
        @NotNull TipoCompromiso tipo,
        @NotNull LocalDate fechaPrevista,
        @NotNull BigDecimal importe,
        @NotNull Long monedaId,
        Long proveedorId,
        Long proyectoId,
        String observaciones,
        /** Si es true, además crea un Vencimiento (F8.1) vinculado a este compromiso. */
        boolean generarVencimiento
) {}

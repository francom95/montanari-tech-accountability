package com.montanaritech.contable.compromiso.dto;

import com.montanaritech.contable.compromiso.EstadoCompromiso;
import com.montanaritech.contable.compromiso.TipoCompromiso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CompromisoEditarRequest(
        @NotBlank String concepto,
        @NotNull TipoCompromiso tipo,
        @NotNull LocalDate fechaPrevista,
        @NotNull BigDecimal importe,
        @NotNull Long monedaId,
        Long proveedorId,
        Long proyectoId,
        @NotNull EstadoCompromiso estado,
        String observaciones
) {}

package com.montanaritech.contable.vencimientos.dto;

import com.montanaritech.contable.impuestos.atribucion.TipoLiquidacion;
import com.montanaritech.contable.vencimientos.TipoRecurrencia;
import com.montanaritech.contable.vencimientos.TipoVencimiento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Solo aplicable mientras el vencimiento está PENDIENTE o REPROGRAMADO (F8.1). */
public record VencimientoEditarRequest(
        @NotBlank String descripcion,
        @NotNull TipoVencimiento tipo,
        @NotNull LocalDate fecha,
        BigDecimal importeEstimado,
        @NotNull Long monedaId,
        @NotNull TipoRecurrencia recurrencia,
        Integer intervaloDiasPersonalizado,
        Long cuentaContableId,
        Long proveedorId,
        TipoLiquidacion liquidacionTipo,
        Long liquidacionId,
        Long tarjetaCreditoId,
        Long proyectoId,
        Long conceptoRecurrenteId,
        String observaciones
) {}

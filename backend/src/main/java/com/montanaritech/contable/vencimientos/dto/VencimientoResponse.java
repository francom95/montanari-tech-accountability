package com.montanaritech.contable.vencimientos.dto;

import com.montanaritech.contable.impuestos.atribucion.TipoLiquidacion;
import com.montanaritech.contable.vencimientos.TipoRecurrencia;
import com.montanaritech.contable.vencimientos.TipoVencimiento;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * {@code estado} ya viene con VENCIDO aplicado (PENDIENTE + fecha pasada) —
 * ver {@code VencimientoMapper.aResponse}, mismo criterio que
 * {@code EstadoVencimiento} de F4.5.
 */
public record VencimientoResponse(
        Long id,
        String descripcion,
        TipoVencimiento tipo,
        LocalDate fecha,
        BigDecimal importeEstimado,
        Long monedaId,
        String monedaCodigo,
        TipoRecurrencia recurrencia,
        Integer intervaloDiasPersonalizado,
        String estado,
        Long cuentaContableId,
        String cuentaContableCodigo,
        Long proveedorId,
        String proveedorNombre,
        TipoLiquidacion liquidacionTipo,
        Long liquidacionId,
        Long tarjetaCreditoId,
        String tarjetaCreditoEntidad,
        Long proyectoId,
        String proyectoNombre,
        Long conceptoRecurrenteId,
        String conceptoRecurrenteNombre,
        Long asientoVinculadoId,
        Long asientoVinculadoNumero,
        String origenGeneracion,
        Long origenGeneracionRefId,
        String observaciones,
        String motivoCancelacion
) {}

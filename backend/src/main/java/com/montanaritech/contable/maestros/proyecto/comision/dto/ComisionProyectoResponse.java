package com.montanaritech.contable.maestros.proyecto.comision.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ComisionProyectoResponse(
        Long id,
        Long proyectoId,
        String proyectoNombre,
        Long comisionistaId,
        String comisionistaNombre,
        BigDecimal porcentajeComision,
        String baseCalculo,
        Long monedaId,
        String monedaCodigo,
        BigDecimal importeEstimado,
        BigDecimal importeFinal,
        String estadoPago,
        LocalDate fechaEstimadaPago,
        String observaciones,
        boolean activo
) {}

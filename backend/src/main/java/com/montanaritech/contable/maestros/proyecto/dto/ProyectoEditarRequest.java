package com.montanaritech.contable.maestros.proyecto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProyectoEditarRequest(
        @NotBlank String nombre,
        @NotNull Long clienteId,
        Long responsableId,
        String pais,
        String tipoProyecto,
        String estado,
        @NotNull Long monedaId,
        @NotNull @DecimalMin(value = "0.00") BigDecimal montoTotal,
        Integer cantidadPagosPactados,
        String comentarios,
        String estadoComercial,
        String estadoFacturacion,
        String estadoCobranza,
        LocalDate fechaEstimadaFinalizacion,
        LocalDate fechaRealFinalizacion,
        @Valid List<CuotaRequest> cuotas
) {}

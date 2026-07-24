package com.montanaritech.contable.maestros.proyecto.presupuesto.dto;

import com.montanaritech.contable.maestros.proyecto.presupuesto.PresupuestoCalculado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public final class PresupuestoProyectoDtos {

    private PresupuestoProyectoDtos() {
    }

    public record LineaCostoRequest(
            @NotBlank String nombre,
            @NotNull @DecimalMin(value = "0.00") BigDecimal importeUsd) {}

    public record GuardarRequest(
            @NotNull @DecimalMin(value = "0.00") BigDecimal margenDeseadoUsd,
            BigDecimal comisionesBancariasIntermediasComexUsd,
            String observaciones,
            @Valid List<LineaCostoRequest> lineasCosto) {}

    public record LineaCostoResponse(Long id, String nombre, BigDecimal importeUsd) {}

    public record Response(
            Long id,
            Long proyectoId,
            BigDecimal margenDeseadoUsd,
            BigDecimal comisionesBancariasIntermediasComexUsd,
            String observaciones,
            List<LineaCostoResponse> lineasCosto,
            PresupuestoCalculado calculado) {}
}

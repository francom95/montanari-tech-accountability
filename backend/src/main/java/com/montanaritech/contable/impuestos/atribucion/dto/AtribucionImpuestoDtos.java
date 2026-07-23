package com.montanaritech.contable.impuestos.atribucion.dto;

import com.montanaritech.contable.impuestos.atribucion.CriterioAtribucion;
import com.montanaritech.contable.impuestos.atribucion.TipoLiquidacion;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/** DTOs de la atribución de impuestos a proyectos (F6.3). */
public final class AtribucionImpuestoDtos {

    private AtribucionImpuestoDtos() {
    }

    /** Porcentaje manual de un proyecto (para criterio PORCENTAJE_MANUAL). */
    public record PorcentajeProyecto(@NotNull Long proyectoId, @NotNull BigDecimal porcentaje) {
    }

    /**
     * Pide calcular/guardar una atribución. {@code proyectoIdDirecto} solo se usa
     * con criterio DIRECTO; {@code porcentajes} solo con PORCENTAJE_MANUAL.
     */
    public record CalcularRequest(
            @NotNull CriterioAtribucion criterio,
            Long proyectoIdDirecto,
            List<PorcentajeProyecto> porcentajes) {
    }

    public record ConfiguracionRequest(@NotNull CriterioAtribucion criterioPorDefecto) {
    }

    public record ConfiguracionResponse(CriterioAtribucion criterioPorDefecto) {
    }

    public record LineaResponse(
            Long proyectoId,
            String proyectoNombre,
            BigDecimal porcentaje,
            BigDecimal monto) {
    }

    public record AtribucionResponse(
            TipoLiquidacion liquidacionTipo,
            Long liquidacionId,
            Integer anio,
            Integer mes,
            CriterioAtribucion criterio,
            BigDecimal montoTotal,
            boolean guardada,
            List<LineaResponse> lineas,
            List<String> advertencias) {
    }
}

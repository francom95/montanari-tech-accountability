package com.montanaritech.contable.impuestos.atribucion;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado del cálculo de una atribución (F6.3), guardada o previsualizada. La
 * suma de {@code lineas[].monto} es siempre exactamente {@code montoTotal}.
 */
public record CalculoAtribucion(
        TipoLiquidacion liquidacionTipo,
        Long liquidacionId,
        int anio,
        int mes,
        CriterioAtribucion criterio,
        BigDecimal montoTotal,
        boolean guardada,
        List<LineaCalculada> lineas,
        List<String> advertencias
) {

    public record LineaCalculada(
            Long proyectoId,
            String proyectoNombre,
            BigDecimal porcentaje,
            BigDecimal monto
    ) {
    }
}

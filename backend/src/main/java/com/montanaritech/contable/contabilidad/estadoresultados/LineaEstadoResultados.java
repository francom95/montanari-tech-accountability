package com.montanaritech.contable.contabilidad.estadoresultados;

/**
 * Las 9 líneas fijas del estado de resultados (F7.3), en el orden en que se
 * presentan. El agrupamiento en subtotales (resultado bruto/operativo/final)
 * es una fórmula fija sobre estas líneas, no configurable — lo único
 * configurable es qué rubro cae en cuál línea ({@code
 * MapeoRubroLineaEstadoResultados}).
 */
public enum LineaEstadoResultados {
    INGRESOS_POR_VENTAS,
    OTROS_INGRESOS_POR_VENTAS,
    COSTOS_DE_PRESTACION_DE_SERVICIOS,
    GASTOS_DE_COMERCIALIZACION,
    GASTOS_DE_ADMINISTRACION,
    GASTOS_FINANCIEROS,
    IMPUESTOS,
    OTROS_INGRESOS,
    OTROS_EGRESOS
}

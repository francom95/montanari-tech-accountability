package com.montanaritech.contable.contabilidad.estadoresultados.dto;

import com.montanaritech.contable.contabilidad.estadoresultados.LineaEstadoResultados;
import java.math.BigDecimal;
import java.util.List;

public final class EstadoResultadosDtos {

    private EstadoResultadosDtos() {
    }

    /** Drill-down: cada cuenta que compone una línea, con su monto ya signado en su dirección natural. */
    public record CuentaMonto(Long cuentaId, String codigo, String nombre, BigDecimal monto) {}

    public record LineaCalculada(LineaEstadoResultados linea, BigDecimal monto, List<CuentaMonto> cuentas) {}

    /**
     * {@code tieneMovimiento} distingue "sin ningún movimiento en el período"
     * (todas las líneas en cero porque no hubo nada) de "hubo movimiento pero
     * el neto dio cero" — relevante para la vista por proyecto, que omite
     * proyectos sin ningún movimiento en vez de listarlos todos en cero.
     */
    public record EstadoResultadosCalculado(
            List<LineaCalculada> lineas,
            BigDecimal resultadoBruto,
            BigDecimal resultadoOperativo,
            BigDecimal resultadoFinal,
            BigDecimal montoSinMapear,
            List<CuentaMonto> cuentasSinMapear,
            boolean tieneMovimiento) {}

    public record ComparativoMes(
            int anioAnterior,
            int mesAnterior,
            BigDecimal resultadoFinalAnterior,
            BigDecimal variacionAbsoluta,
            BigDecimal variacionPorcentual) {}

    /** {@code comparativoMesAnterior} solo se completa en la vista MES. */
    public record EstadoResultadosResponse(
            EstadoResultadosCalculado calculado,
            ComparativoMes comparativoMesAnterior) {}

    public record EstadoResultadosPorProyectoItem(Long proyectoId, String proyectoNombre, EstadoResultadosCalculado calculado) {}

    public record EstadoResultadosPorProyectoResponse(
            List<EstadoResultadosPorProyectoItem> porProyecto,
            EstadoResultadosCalculado sinProyecto) {}
}

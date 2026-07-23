package com.montanaritech.contable.impuestos.iva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resultado del motor de cálculo (F6.1 §1.2) antes de persistirse como
 * {@link LiquidacionIva}: los componentes automáticos con su detalle trazable
 * y las advertencias que la pantalla debe mostrar. No incluye los componentes
 * manuales — esos los agrega el usuario sobre el borrador ya creado.
 */
public record CalculoIva(
        int anio,
        int mes,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        List<ComponenteCalculado> componentes,
        List<String> advertencias
) {

    /** Un componente automático ya resuelto contra los asientos del período. */
    public record ComponenteCalculado(
            TipoComponenteIva tipo,
            String descripcion,
            BigDecimal importe,
            List<DetalleImputacion> detalle
    ) {
    }

    /**
     * Una línea de asiento que aportó al componente, para que la pantalla
     * pueda mostrar de dónde salió cada peso y detectar imputaciones erróneas
     * (el riesgo asumido en F6.1 §1.1 al calcular desde asientos).
     */
    public record DetalleImputacion(
            Long asientoId,
            Long asientoNumero,
            LocalDate fecha,
            String descripcion,
            String documentoOrigenTipo,
            Long documentoOrigenId,
            BigDecimal importe
    ) {
    }
}

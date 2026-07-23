package com.montanaritech.contable.impuestos.iibb;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resultado del motor de cálculo de IIBB (F6.2 §1.2) antes de persistirse como
 * {@link LiquidacionIibb}: la base total del período, su reparto sugerido por
 * jurisdicción y las advertencias que la pantalla debe mostrar.
 */
public record CalculoIibb(
        int anio,
        int mes,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        BigDecimal baseTotal,
        /** Total de deducciones de IIBB imputadas en el período (cuenta 1.1.2008), como ayuda para repartir a mano. */
        BigDecimal deduccionesDisponibles,
        List<JurisdiccionCalculada> jurisdicciones,
        List<String> advertencias
) {

    /** Determinación sugerida para una jurisdicción; las deducciones se cargan a mano encima. */
    public record JurisdiccionCalculada(
            Long jurisdiccionId,
            String jurisdiccionCodigo,
            String jurisdiccionNombre,
            BigDecimal ventasDestino,
            BigDecimal coeficiente,
            BigDecimal baseImponible,
            BigDecimal alicuota,
            BigDecimal impuestoDeterminado,
            BigDecimal saldoAFavorAnterior
    ) {
    }
}

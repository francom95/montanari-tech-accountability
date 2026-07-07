package com.montanaritech.contable.maestros.proyecto.comision;

import com.montanaritech.contable.maestros.proyecto.Proyecto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * Calcula {@code importeEstimado} = porcentaje% × base, y recalcula todas las
 * comisiones activas de un proyecto cuando ese proyecto cambia (F2.7:
 * "el cálculo del importe estimado se actualiza al cambiar el proyecto o la
 * base"). {@link ComisionProyecto.BaseCalculo#MONTO_SIN_IMPUESTOS} hoy usa el
 * monto total tal cual porque {@link Proyecto} todavía no discrimina IVA (eso
 * llega con la facturación real, F4.2/F6.1); {@link ComisionProyecto.BaseCalculo#MONTO_COBRADO}
 * da 0 porque los cobros reales recién existen en F4.4. Este es el único
 * punto de extensión: cuando esos módulos existan, {@link #resolverBase}
 * pasa a leer datos reales sin que el resto de este servicio cambie.
 */
@Service
public class ComisionCalculoService {

    public BigDecimal calcularEstimado(Proyecto proyecto, ComisionProyecto.BaseCalculo base, BigDecimal porcentaje) {
        BigDecimal montoBase = resolverBase(proyecto, base);
        return montoBase
                .multiply(porcentaje)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolverBase(Proyecto proyecto, ComisionProyecto.BaseCalculo base) {
        return switch (base) {
            case MONTO_TOTAL, MONTO_SIN_IMPUESTOS -> proyecto.getMontoTotal();
            case MONTO_COBRADO -> BigDecimal.ZERO;
        };
    }
}

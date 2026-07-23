package com.montanaritech.contable.impuestos.iva;

import com.montanaritech.contable.common.asiento.AsientoGenerado;
import com.montanaritech.contable.common.asiento.AsientoGenerator;
import com.montanaritech.contable.common.asiento.LineaAsientoGenerada;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ResolutorCuentas;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Asiento de la liquidación mensual de IVA (F6.1 §1.4, molde PL-4,
 * {@code OrigenAsiento.LIQUIDACION_IVA} ya predeclarado desde F3.4). Cancela
 * los movimientos de IVA del período contra el resultado.
 *
 * <p>El armado sigue una única regla, sin casos especiales por componente:
 * <b>un aporte positivo va al debe y uno negativo al haber</b>, y el resultado
 * se imputa del lado opuesto (saldo a pagar al haber creando el pasivo, saldo a
 * favor al debe creando el activo que arrastra el mes siguiente). Como la suma
 * de los aportes <i>es</i> el resultado por definición, el asiento balancea
 * algebraicamente siempre — incluyendo cuando hay ajustes manuales o
 * componentes agregados a mano, que simplemente entran como un aporte más.
 */
@Component
@RequiredArgsConstructor
public class LiquidacionIvaAsientoGenerator implements AsientoGenerator<LiquidacionIva> {

    /** El IVA es un impuesto argentino: la liquidación se contabiliza siempre en pesos. */
    private static final String MONEDA_LIQUIDACION = "ARS";

    private final ResolutorCuentas resolutorCuentas;
    private final MonedaRepository monedaRepository;

    @Override
    public AsientoGenerado generar(LiquidacionIva liquidacion) {
        String descripcion = "Liquidación de IVA %02d/%d".formatted(liquidacion.getMes(), liquidacion.getAnio());
        Moneda ars = monedaRepository.findByCodigo(MONEDA_LIQUIDACION)
                .orElseThrow(() -> new NegocioException("MONEDA_ARS_FALTANTE",
                        "No está configurada la moneda " + MONEDA_LIQUIDACION + ", necesaria para liquidar IVA"));
        List<LineaAsientoGenerada> lineas = new ArrayList<>();

        for (LiquidacionIvaComponente c : liquidacion.getComponentes()) {
            BigDecimal aporte = c.getAporte();
            if (aporte.signum() == 0) {
                continue; // un componente en cero no aporta línea: evita ruido en el asiento
            }
            lineas.add(linea(cuentaDe(c), aporte, c.getDescripcion(), ars));
        }

        // Los tres resultados se evalúan por separado, sin else: el saldo técnico
        // y el de libre disponibilidad pueden coexistir en un mismo mes (crédito
        // fiscal mayor al débito y además percepciones sufridas). Lo que sí es
        // excluyente es pagar: si hay saldo a pagar, no hay saldo a favor.
        agregarResultado(lineas, liquidacion.getSaldoAPagar().negate(),
                ConceptoContable.IVA_SALDO_A_PAGAR, descripcion + " — saldo a pagar", ars);
        agregarResultado(lineas, liquidacion.getSaldoAFavor(),
                ConceptoContable.IVA_SALDO_A_FAVOR, descripcion + " — saldo técnico a arrastrar", ars);
        agregarResultado(lineas, liquidacion.getSaldoLibreDisponibilidad(),
                ConceptoContable.IVA_SALDO_LIBRE_DISPONIBILIDAD,
                descripcion + " — saldo de libre disponibilidad", ars);

        if (lineas.isEmpty()) {
            throw new NegocioException("LIQUIDACION_IVA_SIN_MOVIMIENTOS",
                    "La liquidación de %02d/%d no tiene ningún importe a contabilizar"
                            .formatted(liquidacion.getMes(), liquidacion.getAnio()));
        }

        return new AsientoGenerado(liquidacion.getFechaHasta(), descripcion, "LIQUIDACION_IVA", lineas,
                "LiquidacionIva", liquidacion.getId());
    }

    private void agregarResultado(List<LineaAsientoGenerada> lineas, BigDecimal aporte,
                                  ConceptoContable concepto, String leyenda, Moneda ars) {
        if (aporte.signum() != 0) {
            lineas.add(linea(resolutorCuentas.resolver(concepto), aporte, leyenda, ars));
        }
    }

    /**
     * Los componentes automáticos resuelven su cuenta por el mapeo de F4.1 —
     * así siguen al usuario si reasigna el mapeo. Los manuales traen la suya, y
     * sin ella el asiento no podría balancear, por eso es un error de negocio y
     * no un {@code NullPointerException}.
     */
    private CuentaContable cuentaDe(LiquidacionIvaComponente c) {
        if (c.getTipo().esAutomatico()) {
            return resolutorCuentas.resolver(c.getTipo().getConcepto());
        }
        if (c.getCuentaContable() == null) {
            throw new NegocioException("COMPONENTE_IVA_SIN_CUENTA",
                    "El concepto \"%s\" no tiene cuenta contable asignada — sin ella el asiento no balancea"
                            .formatted(c.getDescripcion()));
        }
        return c.getCuentaContable();
    }

    /**
     * La liquidación es siempre en pesos: los importes de origen ya llegaron
     * convertidos a ARS en las líneas de asiento que alimentaron el cálculo, así
     * que acá el tipo de cambio es 1 y el importe original coincide con el
     * importe. Se pasa la moneda explícitamente porque {@code AsientoLinea.moneda}
     * es obligatoria — el constructor mínimo de {@code LineaAsientoGenerada}, que
     * la deja nula, sirve solo para validar balance en tests.
     */
    private LineaAsientoGenerada linea(CuentaContable cuenta, BigDecimal aporte, String leyenda, Moneda ars) {
        BigDecimal debe = aporte.signum() > 0 ? aporte : BigDecimal.ZERO;
        BigDecimal haber = aporte.signum() < 0 ? aporte.negate() : BigDecimal.ZERO;
        BigDecimal importe = aporte.abs();
        return new LineaAsientoGenerada(cuenta.getCodigo(), debe, haber, leyenda,
                ars.getId(), importe, BigDecimal.ONE, "MANUAL",
                null, null, null, null, null);
    }
}

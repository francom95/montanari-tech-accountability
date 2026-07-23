package com.montanaritech.contable.impuestos.iibb;

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
 * Asiento de la liquidación mensual de IIBB (F6.2 §1.5, molde PL-4,
 * {@code OrigenAsiento.LIQUIDACION_IIBB} ya predeclarado desde F3.4). Un solo
 * asiento cubre todas las jurisdicciones, con líneas por jurisdicción (misma
 * cuenta, jurisdicción en la leyenda) para trazabilidad.
 *
 * <p>Misma regla única que F6.1: aporte positivo → debe, negativo → haber, y el
 * resultado del lado opuesto. Por jurisdicción: el impuesto determinado es un
 * gasto (Debe 5.3.2009); las deducciones son créditos ya existentes en 1.1.2008
 * que se consumen (Haber); solo el neto va al pasivo (Haber 2.1.2010) o, si
 * sobra, al activo que arrastra el mes siguiente (Debe 1.1.2008). Balancea
 * algebraicamente por construcción.
 */
@Component
@RequiredArgsConstructor
public class LiquidacionIibbAsientoGenerator implements AsientoGenerator<LiquidacionIibb> {

    private static final String MONEDA_LIQUIDACION = "ARS";

    private final ResolutorCuentas resolutorCuentas;
    private final MonedaRepository monedaRepository;

    @Override
    public AsientoGenerado generar(LiquidacionIibb liquidacion) {
        String descripcion = "Liquidación de IIBB %02d/%d".formatted(liquidacion.getMes(), liquidacion.getAnio());
        Moneda ars = monedaRepository.findByCodigo(MONEDA_LIQUIDACION)
                .orElseThrow(() -> new NegocioException("MONEDA_ARS_FALTANTE",
                        "No está configurada la moneda " + MONEDA_LIQUIDACION + ", necesaria para liquidar IIBB"));
        List<LineaAsientoGenerada> lineas = new ArrayList<>();

        for (LiquidacionIibbJurisdiccion j : liquidacion.getJurisdicciones()) {
            String cod = j.getJurisdiccion().getCodigo();

            // Impuesto determinado del período (gasto). Es un campo, no un componente.
            if (j.getImpuestoDeterminado().signum() != 0) {
                lineas.add(linea(resolutorCuentas.resolver(ConceptoContable.IMPUESTO_IIBB_DETERMINADO),
                        j.getImpuestoDeterminado(), "Impuesto determinado IIBB " + cod, ars));
            }

            // Deducciones y ajustes de la jurisdicción.
            for (LiquidacionIibbComponente c : j.getComponentes()) {
                BigDecimal aporte = c.getAporte();
                if (aporte.signum() == 0) {
                    continue;
                }
                lineas.add(linea(cuentaDe(c), aporte, c.getDescripcion() + " " + cod, ars));
            }

            // Resultado de la jurisdicción, excluyente (pagar o a favor).
            agregar(lineas, j.getSaldoAPagar().negate(), ConceptoContable.IIBB_A_PAGAR,
                    "IIBB a pagar " + cod, ars);
            agregar(lineas, j.getSaldoAFavor(), ConceptoContable.IIBB_SALDO_A_FAVOR,
                    "Saldo a favor de IIBB a arrastrar " + cod, ars);
        }

        if (lineas.isEmpty()) {
            throw new NegocioException("LIQUIDACION_IIBB_SIN_MOVIMIENTOS",
                    "La liquidación de IIBB %02d/%d no tiene ningún importe a contabilizar"
                            .formatted(liquidacion.getMes(), liquidacion.getAnio()));
        }

        return new AsientoGenerado(liquidacion.getFechaHasta(), descripcion, "LIQUIDACION_IIBB", lineas,
                "LiquidacionIibb", liquidacion.getId());
    }

    private void agregar(List<LineaAsientoGenerada> lineas, BigDecimal aporte,
                         ConceptoContable concepto, String leyenda, Moneda ars) {
        if (aporte.signum() != 0) {
            lineas.add(linea(resolutorCuentas.resolver(concepto), aporte, leyenda, ars));
        }
    }

    private CuentaContable cuentaDe(LiquidacionIibbComponente c) {
        if (c.getTipo().getConcepto() != null) {
            return resolutorCuentas.resolver(c.getTipo().getConcepto());
        }
        if (c.getCuentaContable() == null) {
            throw new NegocioException("COMPONENTE_IIBB_SIN_CUENTA",
                    "El concepto \"%s\" no tiene cuenta contable asignada — sin ella el asiento no balancea"
                            .formatted(c.getDescripcion()));
        }
        return c.getCuentaContable();
    }

    private LineaAsientoGenerada linea(CuentaContable cuenta, BigDecimal aporte, String leyenda, Moneda ars) {
        BigDecimal debe = aporte.signum() > 0 ? aporte : BigDecimal.ZERO;
        BigDecimal haber = aporte.signum() < 0 ? aporte.negate() : BigDecimal.ZERO;
        BigDecimal importe = aporte.abs();
        return new LineaAsientoGenerada(cuenta.getCodigo(), debe, haber, leyenda,
                ars.getId(), importe, BigDecimal.ONE, "MANUAL",
                null, null, null, null, null);
    }
}

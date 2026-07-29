package com.montanaritech.contable.impuestos.iibb;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLinea;
import com.montanaritech.contable.contabilidad.asiento.AsientoLineaRepository;
import com.montanaritech.contable.contabilidad.asiento.OrigenAsiento;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ResolutorCuentas;
import com.montanaritech.contable.facturacion.TipoComprobante;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaRepository;
import com.montanaritech.contable.maestros.jurisdiccion.Jurisdiccion;
import com.montanaritech.contable.maestros.jurisdiccion.JurisdiccionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Motor de cálculo de la liquidación de IIBB (F6.2 §1.2). Determina, por
 * jurisdicción activa del maestro, la base imponible (base total del período ×
 * coeficiente de Convenio Multilateral) y el impuesto determinado (base ×
 * alícuota). Las deducciones no se calculan acá: son manuales (§1.3).
 *
 * <p>La base total sale de las <b>facturas de venta</b>, no de los asientos como
 * en IVA, porque la jurisdicción de destino vive en la factura y la línea de
 * asiento no la lleva. El coeficiente por defecto es la participación de cada
 * jurisdicción por destino, de modo que sin tocar nada la liquidación reproduce
 * la atribución directa; el usuario lo reemplaza por el coeficiente real de CM.
 */
@Service
@RequiredArgsConstructor
public class CalculoIibbService {

    private final FacturaVentaRepository facturaVentaRepository;
    private final JurisdiccionRepository jurisdiccionRepository;
    private final LiquidacionIibbRepository liquidacionIibbRepository;
    private final AsientoLineaRepository asientoLineaRepository;
    private final ResolutorCuentas resolutorCuentas;

    @Transactional(readOnly = true)
    public CalculoIibb calcular(int anio, int mes) {
        YearMonth periodo = YearMonth.of(anio, mes);
        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.atEndOfMonth();
        List<String> advertencias = new ArrayList<>();

        // Ventas netas del período agrupadas por jurisdicción de destino. Las notas
        // de crédito restan; el resto (facturas, notas de débito) suma. F11.2 B8: el
        // neto de cada factura está en su propia moneda (netoGravado), así que hay que
        // convertirlo a ARS con su propio tipoCambio antes de sumarlo — antes se sumaba
        // en moneda original sin convertir, y una venta local en USD quedaba declarada
        // por una fracción de su valor real. Las exportaciones (FACTURA_E) se excluyen
        // por completo: están exentas de IIBB en la generalidad de las jurisdicciones
        // argentinas (decisión confirmada con el usuario).
        Map<Long, BigDecimal> ventasPorJurisdiccion = new HashMap<>();
        BigDecimal baseTotal = BigDecimal.ZERO;
        BigDecimal sinJurisdiccion = BigDecimal.ZERO;
        BigDecimal baseExportadaExcluida = BigDecimal.ZERO;
        for (FacturaVenta f : facturaVentaRepository.buscarConfirmadasParaBaseIibb(desde, hasta)) {
            if (f.getTipoComprobante() == TipoComprobante.FACTURA_E) {
                baseExportadaExcluida = baseExportadaExcluida.add(f.getNetoGravado().multiply(f.getTipoCambio()));
                continue;
            }
            BigDecimal neto = f.getNetoGravado().multiply(f.getTipoCambio());
            if (f.getTipoComprobante().name().startsWith("NOTA_CREDITO")) {
                neto = neto.negate();
            }
            baseTotal = baseTotal.add(neto);
            if (f.getJurisdiccionDestino() == null) {
                sinJurisdiccion = sinJurisdiccion.add(neto);
            } else {
                ventasPorJurisdiccion.merge(f.getJurisdiccionDestino().getId(), neto, BigDecimal::add);
            }
        }
        baseTotal = escalar(baseTotal);

        if (sinJurisdiccion.signum() != 0) {
            advertencias.add(("Hay ventas por %s sin jurisdicción de destino: no se atribuyen a ninguna "
                    + "jurisdicción. Asignales la jurisdicción en la factura o ajustá el coeficiente a mano.")
                    .formatted(escalar(sinJurisdiccion).toPlainString()));
        }
        if (baseExportadaExcluida.signum() != 0) {
            advertencias.add(("Se excluyeron %s de ventas de exportación (FACTURA_E) de la base de IIBB "
                    + "por estar exentas.").formatted(escalar(baseExportadaExcluida).toPlainString()));
        }

        // La liquidación confirmada del mes anterior es la fuente tanto del arrastre
        // (saldo a favor por jurisdicción) como del coeficiente de Convenio Multilateral:
        // el coeficiente sale de la determinación anual CM05, así que es estable mes a
        // mes y conviene traerlo del mes anterior para poder confirmar sin recargarlo.
        YearMonth anterior = periodo.minusMonths(1);
        Optional<LiquidacionIibb> previa = liquidacionIibbRepository.findFirstByAnioAndMesAndEstado(
                anterior.getYear(), anterior.getMonthValue(), EstadoDocumento.CONFIRMADO);
        Map<Long, BigDecimal> arrastrePorJurisdiccion = new HashMap<>();
        Map<Long, BigDecimal> coeficienteAnterior = new HashMap<>();
        if (previa.isEmpty()) {
            advertencias.add(("No hay una liquidación de IIBB confirmada de %02d/%d, así que los saldos a favor "
                    + "arrastrados entran en cero y el coeficiente por defecto se toma de la participación por "
                    + "destino. Cargalos a mano si venías con saldo a favor o con un coeficiente distinto.")
                    .formatted(anterior.getMonthValue(), anterior.getYear()));
        } else {
            for (LiquidacionIibbJurisdiccion lj : previa.get().getJurisdicciones()) {
                if (lj.getSaldoAFavor().signum() != 0) {
                    arrastrePorJurisdiccion.put(lj.getJurisdiccion().getId(), lj.getSaldoAFavor());
                }
                coeficienteAnterior.put(lj.getJurisdiccion().getId(), lj.getCoeficiente());
            }
        }

        List<CalculoIibb.JurisdiccionCalculada> jurisdicciones = new ArrayList<>();
        List<Jurisdiccion> activas = jurisdiccionRepository.findByActivoTrueOrderByCodigoAsc();
        if (activas.isEmpty()) {
            advertencias.add("No hay jurisdicciones activas en el maestro: cargá al menos una para liquidar IIBB.");
        }

        // F11.2 A19: ventas dirigidas a una jurisdicción que existe pero está inactiva en el
        // maestro entraban a baseTotal (arriba) sin repartirse a ninguna sub-liquidación —
        // ni tributaban en ningún lado ni se avisaba, y de paso inflaban el denominador de
        // los coeficientes por defecto del resto.
        Set<Long> idsActivas = activas.stream().map(Jurisdiccion::getId).collect(java.util.stream.Collectors.toSet());
        BigDecimal ventasAJurisdiccionInactiva = ventasPorJurisdiccion.entrySet().stream()
                .filter(e -> !idsActivas.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (ventasAJurisdiccionInactiva.signum() != 0) {
            advertencias.add(("Hay ventas por %s dirigidas a jurisdicciones inactivas en el maestro: no se "
                    + "reparten a ninguna sub-liquidación. Reactivá la jurisdicción o reasigná el destino de esas facturas.")
                    .formatted(escalar(ventasAJurisdiccionInactiva).toPlainString()));
        }

        // F11.2 A18: la herencia del coeficiente del mes anterior es una decisión de la
        // LIQUIDACIÓN completa, no de cada jurisdicción por separado — antes, una jurisdicción
        // nueva (sin fila en la liquidación anterior) caía sola al criterio "participación por
        // destino" mientras el resto conservaba el coeficiente real de CM, y la mezcla de los
        // dos criterios no suma 1 por construcción. Si hay liquidación anterior, la jurisdicción
        // sin coeficiente propio entra en CERO (con advertencia), nunca a un criterio distinto.
        boolean hayLiquidacionAnterior = previa.isPresent();
        List<String> jurisdiccionesSinCoeficienteAnterior = new ArrayList<>();
        BigDecimal sumaCoeficientes = BigDecimal.ZERO;
        for (Jurisdiccion j : activas) {
            BigDecimal ventasDestino = escalar(ventasPorJurisdiccion.getOrDefault(j.getId(), BigDecimal.ZERO));
            BigDecimal coeficiente;
            if (hayLiquidacionAnterior) {
                BigDecimal deLaAnterior = coeficienteAnterior.get(j.getId());
                if (deLaAnterior != null) {
                    coeficiente = deLaAnterior;
                } else {
                    coeficiente = BigDecimal.ZERO;
                    jurisdiccionesSinCoeficienteAnterior.add(j.getNombre());
                }
            } else {
                // Sin liquidación anterior alguna: todas las jurisdicciones usan el mismo
                // criterio (participación por destino), nunca una mezcla.
                coeficiente = baseTotal.signum() == 0
                        ? BigDecimal.ZERO
                        : ventasDestino.divide(baseTotal, 6, RoundingMode.HALF_UP);
            }
            sumaCoeficientes = sumaCoeficientes.add(coeficiente);
            BigDecimal baseImponible = escalar(baseTotal.multiply(coeficiente));
            BigDecimal alicuota = j.getAlicuotaIIBB();
            BigDecimal impuestoDeterminado = escalar(baseImponible.multiply(alicuota).divide(BigDecimal.valueOf(100)));
            jurisdicciones.add(new CalculoIibb.JurisdiccionCalculada(
                    j.getId(), j.getCodigo(), j.getNombre(),
                    ventasDestino, coeficiente, baseImponible, alicuota, impuestoDeterminado,
                    escalar(arrastrePorJurisdiccion.getOrDefault(j.getId(), BigDecimal.ZERO))));
        }
        if (!jurisdiccionesSinCoeficienteAnterior.isEmpty()) {
            advertencias.add(("Las jurisdicciones %s no tenían coeficiente en la liquidación de %02d/%d: entran en 0 — "
                    + "cargalo a mano.").formatted(String.join(", ", jurisdiccionesSinCoeficienteAnterior),
                    anterior.getMonthValue(), anterior.getYear()));
        }
        // F11.2 A17: nada validaba que los coeficientes de Convenio Multilateral sumaran 1 —
        // un typo (o una edición manual mal hecha) sobre/sub-declaraba la base total sin ningún
        // aviso. No bloquea (mismo criterio que el resto de las advertencias de esta liquidación).
        if (!activas.isEmpty() && sumaCoeficientes.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.00005")) > 0) {
            advertencias.add(("La suma de los coeficientes de Convenio Multilateral es %s, no 1 — revisá los "
                    + "coeficientes de cada jurisdicción antes de confirmar.").formatted(sumaCoeficientes.toPlainString()));
        }

        BigDecimal deduccionesDisponibles = totalDeduccionesDelPeriodo(desde, hasta);
        if (deduccionesDisponibles.signum() != 0) {
            long conBase = jurisdicciones.stream().filter(jc -> jc.baseImponible().signum() != 0).count();
            advertencias.add(conBase == 1
                    ? ("Se trajeron %s de percepciones/SIRCREB de IIBB de contabilidad (cuenta 1.1.2008) a la única "
                        + "jurisdicción con base. Revisá el reparto si corresponde a otra.").formatted(deduccionesDisponibles.toPlainString())
                    : ("El período tiene %s de percepciones/SIRCREB de IIBB imputadas en contabilidad (cuenta 1.1.2008). "
                        + "Repartilas entre las jurisdicciones como deducciones.").formatted(deduccionesDisponibles.toPlainString()));
        }

        return new CalculoIibb(anio, mes, desde, hasta, baseTotal, deduccionesDisponibles, jurisdicciones, advertencias);
    }

    /**
     * Total imputado en la cuenta de deducciones de IIBB (1.1.2008) en el período,
     * como ayuda para el reparto manual. Es una cuenta deudora: acumula por el
     * debe. Excluye los asientos de la propia liquidación de IIBB.
     */
    private BigDecimal totalDeduccionesDelPeriodo(LocalDate desde, LocalDate hasta) {
        CuentaContable cuenta = resolutorCuentas.resolver(ConceptoContable.PERCEPCION_IIBB_SUFRIDA);
        BigDecimal total = BigDecimal.ZERO;
        for (AsientoLinea l : asientoLineaRepository.buscarParaLiquidacionImpositiva(
                Set.of(cuenta.getId()), desde, hasta, EstadoDocumento.CONFIRMADO, OrigenAsiento.LIQUIDACION_IIBB)) {
            total = total.add(l.getDebe().subtract(l.getHaber()));
        }
        return escalar(total);
    }

    private BigDecimal escalar(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}

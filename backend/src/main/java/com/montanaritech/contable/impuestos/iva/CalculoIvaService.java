package com.montanaritech.contable.impuestos.iva;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLinea;
import com.montanaritech.contable.contabilidad.asiento.AsientoLineaRepository;
import com.montanaritech.contable.contabilidad.asiento.OrigenAsiento;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ResolutorCuentas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Motor de cálculo de la liquidación mensual de IVA (F6.1 §1.1/§1.2).
 *
 * <p>Lee de {@code AsientoLinea} filtrando por cuenta contable, <b>no</b> de
 * {@code factura_venta}/{@code factura_compra}. Eso hereda gratis la regla de
 * crédito fiscal condicional de F4.3 (si una compra no computaba crédito, su
 * IVA se absorbió en el costo y nunca tocó la cuenta 1.1.2006), unifica en una
 * sola consulta las tres fuentes de crédito fiscal que pide el plan
 * (compras + comisiones bancarias de F5.3 + asientos manuales), y hace que las
 * notas de crédito neteen solas, porque los generadores ya les invierten los
 * lados.
 *
 * <p>La cuenta de cada componente se resuelve por {@link ResolutorCuentas} y no
 * por código hardcodeado: si el usuario reasigna un mapeo, la liquidación lo
 * sigue. Y la dirección de acumulación sale de la naturaleza de esa cuenta
 * ({@code saldoEsperado}), no de una constante — una cuenta acreedora acumula
 * por el haber, una deudora por el debe.
 */
@Service
@RequiredArgsConstructor
public class CalculoIvaService {

    private final AsientoLineaRepository asientoLineaRepository;
    private final ResolutorCuentas resolutorCuentas;
    private final LiquidacionIvaRepository liquidacionIvaRepository;

    @Transactional(readOnly = true)
    public CalculoIva calcular(int anio, int mes) {
        YearMonth periodo = YearMonth.of(anio, mes);
        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.atEndOfMonth();

        List<CalculoIva.ComponenteCalculado> componentes = new ArrayList<>();
        List<String> advertencias = new ArrayList<>();

        for (TipoComponenteIva tipo : List.of(TipoComponenteIva.DEBITO_FISCAL,
                TipoComponenteIva.CREDITO_FISCAL, TipoComponenteIva.PERCEPCIONES)) {
            componentes.add(calcularDesdeAsientos(tipo, desde, hasta));
        }

        componentes.add(calcularArrastre(periodo, advertencias));
        return new CalculoIva(anio, mes, desde, hasta, componentes, advertencias);
    }

    private CalculoIva.ComponenteCalculado calcularDesdeAsientos(TipoComponenteIva tipo, LocalDate desde, LocalDate hasta) {
        CuentaContable cuenta = resolutorCuentas.resolver(tipo.getConcepto());
        List<AsientoLinea> lineas = asientoLineaRepository.buscarParaLiquidacionImpositiva(
                Set.of(cuenta.getId()), desde, hasta, EstadoDocumento.CONFIRMADO, OrigenAsiento.LIQUIDACION_IVA);

        boolean acumulaPorHaber = cuenta.getSaldoEsperado() == CuentaContable.SaldoEsperado.ACREEDOR;
        List<CalculoIva.DetalleImputacion> detalle = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (AsientoLinea l : lineas) {
            BigDecimal aporte = acumulaPorHaber
                    ? l.getHaber().subtract(l.getDebe())
                    : l.getDebe().subtract(l.getHaber());
            total = total.add(aporte);
            detalle.add(new CalculoIva.DetalleImputacion(
                    l.getAsiento().getId(), l.getAsiento().getNumero(), l.getAsiento().getFecha(),
                    l.getLeyenda() != null ? l.getLeyenda() : l.getAsiento().getDescripcion(),
                    l.getAsiento().getOrigenTipo(), l.getAsiento().getOrigenId(), aporte));
        }

        String descripcion = "%s (%s %s)".formatted(
                tipo.getDescripcionPorDefecto(), cuenta.getCodigo(), cuenta.getNombre());
        return new CalculoIva.ComponenteCalculado(tipo, descripcion, total, detalle);
    }

    /**
     * Arrastre del saldo a favor del mes anterior. Se lee de la liquidación
     * anterior confirmada y no de los asientos: es un dato explícito, auditable
     * y ya ajustado a mano si hizo falta.
     *
     * <p>No liquidar el mes anterior <b>no</b> bloquea: el arrastre entra en
     * cero y queda una advertencia visible (F6.1 §1.6). Bloquear impediría
     * arrancar el sistema —el primer mes no tiene anterior— y chocaría con la
     * importación histórica de F4.6.
     */
    private CalculoIva.ComponenteCalculado calcularArrastre(YearMonth periodo, List<String> advertencias) {
        YearMonth anterior = periodo.minusMonths(1);
        Optional<LiquidacionIva> previa = liquidacionIvaRepository.findFirstByAnioAndMesAndEstado(
                anterior.getYear(), anterior.getMonthValue(), EstadoDocumento.CONFIRMADO);

        BigDecimal arrastre = previa.map(LiquidacionIva::getSaldoAFavor).orElse(BigDecimal.ZERO);
        if (previa.isEmpty()) {
            advertencias.add(("No hay una liquidación confirmada de %02d/%d, así que el saldo técnico anterior "
                    + "entra en cero. Si venías con saldo a favor, cargalo como ajuste manual en ese componente.")
                    .formatted(anterior.getMonthValue(), anterior.getYear()));
        }

        return new CalculoIva.ComponenteCalculado(TipoComponenteIva.SALDO_TECNICO_ANTERIOR,
                "%s (%02d/%d)".formatted(TipoComponenteIva.SALDO_TECNICO_ANTERIOR.getDescripcionPorDefecto(),
                        anterior.getMonthValue(), anterior.getYear()),
                arrastre, List.of());
    }
}

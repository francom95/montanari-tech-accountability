package com.montanaritech.contable.impuestos.iva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.asiento.AsientoGenerado;
import com.montanaritech.contable.common.asiento.LineaAsientoGenerada;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ResolutorCuentas;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Asiento de la liquidación de IVA (F6.1 §1.4). Lo que más importa verificar es
 * que <b>balancee en los tres escenarios</b> —a pagar, a favor y con arrastre—
 * porque el generador no arma las líneas caso por caso sino con una regla única
 * (aporte positivo al debe, negativo al haber) que se apoya en que la suma de
 * los aportes sea exactamente el resultado.
 */
@ExtendWith(MockitoExtension.class)
// lenient: en el caso "período sin movimientos" no se resuelve ninguna cuenta —
// que el stub quede sin usar es justamente la señal de que no se armó ninguna línea.
@MockitoSettings(strictness = Strictness.LENIENT)
class LiquidacionIvaAsientoGeneratorTest {

    @Mock
    private ResolutorCuentas resolutorCuentas;
    @Mock
    private MonedaRepository monedaRepository;

    private LiquidacionIvaAsientoGenerator generator;
    private final Map<ConceptoContable, CuentaContable> cuentas = new HashMap<>();

    @BeforeEach
    void setUp() {
        generator = new LiquidacionIvaAsientoGenerator(resolutorCuentas, monedaRepository);
        Moneda ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");
        when(monedaRepository.findByCodigo("ARS")).thenReturn(Optional.of(ars));
        cuentas.put(ConceptoContable.IVA_DEBITO_FISCAL, cuenta(1L, "2.1.2008"));
        cuentas.put(ConceptoContable.IVA_CREDITO_FISCAL, cuenta(2L, "1.1.2006"));
        cuentas.put(ConceptoContable.PERCEPCION_IVA_SUFRIDA, cuenta(3L, "1.1.2007"));
        cuentas.put(ConceptoContable.IVA_SALDO_A_FAVOR, cuenta(4L, "1.1.2014"));
        cuentas.put(ConceptoContable.IVA_SALDO_A_PAGAR, cuenta(5L, "2.1.2009"));
        when(resolutorCuentas.resolver(any(ConceptoContable.class)))
                .thenAnswer(inv -> cuentas.get(inv.getArgument(0)));
    }

    @Test
    void mesConSaldoAPagarBalanceaYAcreditaLaCuentaDePasivoFiscal() {
        // 1000 de débito, 300 de crédito, 100 de percepciones -> 600 a pagar
        LiquidacionIva l = liquidacion("1000.00", "300.00", "100.00", "0.00");

        AsientoGenerado a = generator.generar(l);

        assertBalancea(a);
        assertThat(lineaDe(a, "2.1.2008").debe()).isEqualByComparingTo("1000.00");
        assertThat(lineaDe(a, "1.1.2006").haber()).isEqualByComparingTo("300.00");
        assertThat(lineaDe(a, "1.1.2007").haber()).isEqualByComparingTo("100.00");
        assertThat(lineaDe(a, "2.1.2009").haber()).isEqualByComparingTo("600.00");
        assertThat(a.origen()).isEqualTo("LIQUIDACION_IVA");
        assertThat(a.documentoOrigenTipo()).isEqualTo("LiquidacionIva");
    }

    /**
     * {@code AsientoLinea.moneda} es obligatoria y {@code registrarAutomatico} la
     * resuelve sin guard de null (a diferencia de proyecto/cliente/etc.), así que
     * una línea sin moneda revienta recién al persistir. Este test cubre ese hueco.
     */
    @Test
    void todasLasLineasLlevanMonedaTipoDeCambioEImporteOriginal() {
        AsientoGenerado a = generator.generar(liquidacion("1000.00", "300.00", "100.00", "50.00"));

        assertThat(a.lineas()).isNotEmpty().allSatisfy(l -> {
            assertThat(l.monedaId()).as("moneda de %s", l.cuentaCodigo()).isEqualTo(1L);
            assertThat(l.tipoCambio()).isEqualByComparingTo("1");
            assertThat(l.importeOriginal()).isEqualByComparingTo(l.debe().max(l.haber()));
            assertThat(l.fuenteTc()).isNotNull();
        });
    }

    @Test
    void mesConSaldoAFavorBalanceaYDebitaLaCuentaQueArrastraElProximoMes() {
        // 200 de débito contra 500 de crédito y 100 de percepciones -> 400 a favor
        LiquidacionIva l = liquidacion("200.00", "500.00", "100.00", "0.00");

        AsientoGenerado a = generator.generar(l);

        assertBalancea(a);
        assertThat(lineaDe(a, "1.1.2014").debe()).isEqualByComparingTo("400.00");
        assertThat(a.lineas()).noneMatch(x -> x.cuentaCodigo().equals("2.1.2009"));
    }

    @Test
    void mesQueConsumeElSaldoAFavorArrastradoBalanceaAcreditandoEsaMismaCuenta() {
        // 1000 de débito, 200 de crédito, 300 arrastrado -> 500 a pagar
        LiquidacionIva l = liquidacion("1000.00", "200.00", "0.00", "300.00");

        AsientoGenerado a = generator.generar(l);

        assertBalancea(a);
        assertThat(lineaDe(a, "1.1.2014").haber()).isEqualByComparingTo("300.00");
        assertThat(lineaDe(a, "2.1.2009").haber()).isEqualByComparingTo("500.00");
    }

    @Test
    void componenteEnCeroNoGeneraLinea() {
        LiquidacionIva l = liquidacion("1000.00", "300.00", "0.00", "0.00");

        AsientoGenerado a = generator.generar(l);

        assertThat(a.lineas()).noneMatch(x -> x.cuentaCodigo().equals("1.1.2007"));
    }

    @Test
    void unAjusteManualSeAbsorbeEnElResultadoYElAsientoSigueBalanceando() {
        LiquidacionIva l = liquidacion("1000.00", "300.00", "100.00", "0.00");
        // el contador corrige el débito fiscal a 1200: el resultado pasa de 600 a 800
        l.getComponentes().getFirst().setImporteAjuste(new BigDecimal("200.00"));
        recalcular(l);

        AsientoGenerado a = generator.generar(l);

        assertBalancea(a);
        assertThat(lineaDe(a, "2.1.2008").debe()).isEqualByComparingTo("1200.00");
        assertThat(lineaDe(a, "2.1.2009").haber()).isEqualByComparingTo("800.00");
    }

    @Test
    void componenteManualAportaSuPropiaLineaYNoRompeElBalance() {
        LiquidacionIva l = liquidacion("1000.00", "300.00", "100.00", "0.00");
        LiquidacionIvaComponente restitucion = new LiquidacionIvaComponente();
        restitucion.setLiquidacionIva(l);
        restitucion.setTipo(TipoComponenteIva.RESTITUCIONES);
        restitucion.setDescripcion("Restitución de crédito fiscal");
        restitucion.setImporteCalculado(new BigDecimal("50.00"));
        restitucion.setCuentaContable(cuenta(9L, "5.3.2007"));
        restitucion.setManual(true);
        restitucion.setOrden(5);
        l.getComponentes().add(restitucion);
        recalcular(l);

        AsientoGenerado a = generator.generar(l);

        assertBalancea(a);
        assertThat(lineaDe(a, "5.3.2007").debe()).isEqualByComparingTo("50.00");
        assertThat(lineaDe(a, "2.1.2009").haber()).isEqualByComparingTo("650.00");
    }

    @Test
    void componenteManualSinCuentaContableNoSePuedeConfirmar() {
        LiquidacionIva l = liquidacion("1000.00", "0.00", "0.00", "0.00");
        LiquidacionIvaComponente huerfano = new LiquidacionIvaComponente();
        huerfano.setLiquidacionIva(l);
        huerfano.setTipo(TipoComponenteIva.OTRO);
        huerfano.setDescripcion("Ajuste sin cuenta");
        huerfano.setImporteCalculado(new BigDecimal("10.00"));
        huerfano.setManual(true);
        huerfano.setOrden(5);
        l.getComponentes().add(huerfano);
        recalcular(l);

        assertThatThrownBy(() -> generator.generar(l))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("no tiene cuenta contable asignada");
    }

    @Test
    void periodoSinNingunMovimientoNoGeneraAsientoVacio() {
        LiquidacionIva l = liquidacion("0.00", "0.00", "0.00", "0.00");

        assertThatThrownBy(() -> generator.generar(l))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("no tiene ningún importe a contabilizar");
    }

    // --- helpers ---

    private void assertBalancea(AsientoGenerado a) {
        BigDecimal debe = a.lineas().stream().map(LineaAsientoGenerada::debe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal haber = a.lineas().stream().map(LineaAsientoGenerada::haber).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debe).as("Σ debe == Σ haber").isEqualByComparingTo(haber);
    }

    private LineaAsientoGenerada lineaDe(AsientoGenerado a, String codigo) {
        return a.lineas().stream()
                .filter(l -> l.cuentaCodigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No hay línea para la cuenta " + codigo));
    }

    private CuentaContable cuenta(Long id, String codigo) {
        CuentaContable c = new CuentaContable();
        c.setId(id);
        c.setCodigo(codigo);
        c.setNombre("Cuenta " + codigo);
        return c;
    }

    private LiquidacionIva liquidacion(String debito, String credito, String percepciones, String arrastre) {
        LiquidacionIva l = new LiquidacionIva();
        l.setAnio(2026);
        l.setMes(3);
        l.setFechaDesde(LocalDate.of(2026, 3, 1));
        l.setFechaHasta(LocalDate.of(2026, 3, 31));
        agregar(l, TipoComponenteIva.DEBITO_FISCAL, debito, 1);
        agregar(l, TipoComponenteIva.CREDITO_FISCAL, credito, 2);
        agregar(l, TipoComponenteIva.PERCEPCIONES, percepciones, 3);
        agregar(l, TipoComponenteIva.SALDO_TECNICO_ANTERIOR, arrastre, 4);
        recalcular(l);
        return l;
    }

    private void agregar(LiquidacionIva l, TipoComponenteIva tipo, String importe, int orden) {
        LiquidacionIvaComponente c = new LiquidacionIvaComponente();
        c.setLiquidacionIva(l);
        c.setTipo(tipo);
        c.setDescripcion(tipo.getDescripcionPorDefecto());
        c.setImporteCalculado(new BigDecimal(importe));
        c.setOrden(orden);
        l.getComponentes().add(c);
    }

    /** Réplica del cálculo de resultado del service, para armar el escenario del generador. */
    private void recalcular(LiquidacionIva l) {
        BigDecimal r = l.getComponentes().stream()
                .map(LiquidacionIvaComponente::getAporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        l.setSaldoAPagar(r.signum() > 0 ? r : BigDecimal.ZERO);
        l.setSaldoAFavor(r.signum() < 0 ? r.negate() : BigDecimal.ZERO);
    }
}

package com.montanaritech.contable.impuestos.iibb;

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
import com.montanaritech.contable.maestros.jurisdiccion.Jurisdiccion;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import java.math.BigDecimal;
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
 * Asiento de la liquidación de IIBB (F6.2 §1.5). Lo central es que <b>balancee
 * con varias jurisdicciones a la vez</b>: cada una determina su impuesto y
 * consume sus deducciones por separado, y el asiento único las agrega.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LiquidacionIibbAsientoGeneratorTest {

    @Mock
    private ResolutorCuentas resolutorCuentas;
    @Mock
    private MonedaRepository monedaRepository;

    private LiquidacionIibbAsientoGenerator generator;
    private final Map<ConceptoContable, CuentaContable> cuentas = new HashMap<>();

    @BeforeEach
    void setUp() {
        generator = new LiquidacionIibbAsientoGenerator(resolutorCuentas, monedaRepository);
        Moneda ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");
        when(monedaRepository.findByCodigo("ARS")).thenReturn(Optional.of(ars));
        cuentas.put(ConceptoContable.IMPUESTO_IIBB_DETERMINADO, cuenta(1L, "5.3.2009"));
        cuentas.put(ConceptoContable.PERCEPCION_IIBB_SUFRIDA, cuenta(2L, "1.1.2008"));
        cuentas.put(ConceptoContable.IIBB_A_PAGAR, cuenta(3L, "2.1.2010"));
        cuentas.put(ConceptoContable.IIBB_SALDO_A_FAVOR, cuenta(4L, "1.1.2008"));
        when(resolutorCuentas.resolver(any(ConceptoContable.class)))
                .thenAnswer(inv -> cuentas.get(inv.getArgument(0)));
    }

    @Test
    void dosJurisdiccionesUnaAPagarYOtraAFavorElAsientoBalancea() {
        LiquidacionIibb l = liquidacion();
        // CABA: determinado 10.000, percepciones 3.000 -> a pagar 7.000
        agregarJurisdiccion(l, "CABA", "10000.00", "3000.00", 1);
        // Buenos Aires: determinado 5.000, percepciones 8.000 -> a favor 3.000
        agregarJurisdiccion(l, "BA", "5000.00", "8000.00", 2);
        recalcular(l);

        AsientoGenerado a = generator.generar(l);

        assertBalancea(a);
        // determinado total al debe de 5.3.2009 = 15.000
        assertThat(sumaDebe(a, "5.3.2009")).isEqualByComparingTo("15000.00");
        // deducciones al haber de 1.1.2008 = 11.000; saldo a favor al debe de 1.1.2008 = 3.000
        assertThat(sumaHaber(a, "1.1.2008")).isEqualByComparingTo("11000.00");
        assertThat(sumaDebe(a, "1.1.2008")).isEqualByComparingTo("3000.00");
        // a pagar de CABA al haber de 2.1.2010 = 7.000
        assertThat(sumaHaber(a, "2.1.2010")).isEqualByComparingTo("7000.00");
        assertThat(a.origen()).isEqualTo("LIQUIDACION_IIBB");
        assertThat(a.documentoOrigenTipo()).isEqualTo("LiquidacionIibb");
    }

    @Test
    void cadaJurisdiccionAportaSusPropiasLineasConSuCodigoEnLaLeyenda() {
        LiquidacionIibb l = liquidacion();
        agregarJurisdiccion(l, "CABA", "10000.00", "0.00", 1);
        agregarJurisdiccion(l, "BA", "5000.00", "0.00", 2);
        recalcular(l);

        AsientoGenerado a = generator.generar(l);

        assertThat(a.lineas()).anyMatch(x -> x.descripcion().contains("CABA"));
        assertThat(a.lineas()).anyMatch(x -> x.descripcion().contains("BA"));
    }

    @Test
    void todasLasLineasLlevanMonedaYTipoDeCambio() {
        LiquidacionIibb l = liquidacion();
        agregarJurisdiccion(l, "CABA", "10000.00", "3000.00", 1);
        recalcular(l);

        AsientoGenerado a = generator.generar(l);

        assertThat(a.lineas()).isNotEmpty().allSatisfy(x -> {
            assertThat(x.monedaId()).isEqualTo(1L);
            assertThat(x.tipoCambio()).isEqualByComparingTo("1");
            assertThat(x.fuenteTc()).isNotNull();
        });
    }

    @Test
    void unaLiquidacionSinNingunImporteNoGeneraAsientoVacio() {
        LiquidacionIibb l = liquidacion();
        agregarJurisdiccion(l, "CABA", "0.00", "0.00", 1);
        recalcular(l);

        assertThatThrownBy(() -> generator.generar(l))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("no tiene ningún importe");
    }

    // --- helpers ---

    private void assertBalancea(AsientoGenerado a) {
        BigDecimal debe = a.lineas().stream().map(LineaAsientoGenerada::debe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal haber = a.lineas().stream().map(LineaAsientoGenerada::haber).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debe).as("Σ debe == Σ haber").isEqualByComparingTo(haber);
    }

    private BigDecimal sumaDebe(AsientoGenerado a, String codigo) {
        return a.lineas().stream().filter(l -> l.cuentaCodigo().equals(codigo))
                .map(LineaAsientoGenerada::debe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumaHaber(AsientoGenerado a, String codigo) {
        return a.lineas().stream().filter(l -> l.cuentaCodigo().equals(codigo))
                .map(LineaAsientoGenerada::haber).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CuentaContable cuenta(Long id, String codigo) {
        CuentaContable c = new CuentaContable();
        c.setId(id);
        c.setCodigo(codigo);
        c.setNombre("Cuenta " + codigo);
        return c;
    }

    private LiquidacionIibb liquidacion() {
        LiquidacionIibb l = new LiquidacionIibb();
        l.setAnio(2026);
        l.setMes(3);
        l.setFechaDesde(java.time.LocalDate.of(2026, 3, 1));
        l.setFechaHasta(java.time.LocalDate.of(2026, 3, 31));
        return l;
    }

    private void agregarJurisdiccion(LiquidacionIibb l, String codigo, String determinado, String percepciones, int orden) {
        Jurisdiccion j = new Jurisdiccion();
        j.setId((long) orden);
        j.setCodigo(codigo);
        j.setNombre(codigo);

        LiquidacionIibbJurisdiccion lj = new LiquidacionIibbJurisdiccion();
        lj.setLiquidacionIibb(l);
        lj.setJurisdiccion(j);
        lj.setImpuestoDeterminado(new BigDecimal(determinado));
        lj.setOrden(orden);

        if (new BigDecimal(percepciones).signum() != 0) {
            LiquidacionIibbComponente c = new LiquidacionIibbComponente();
            c.setLiquidacionIibbJurisdiccion(lj);
            c.setTipo(TipoComponenteIibb.PERCEPCIONES);
            c.setDescripcion("Percepciones de IIBB sufridas");
            c.setImporteCalculado(new BigDecimal(percepciones));
            c.setOrden(1);
            lj.getComponentes().add(c);
        }
        l.getJurisdicciones().add(lj);
    }

    /** Réplica del cálculo de resultado del service (una etapa por jurisdicción). */
    private void recalcular(LiquidacionIibb l) {
        for (LiquidacionIibbJurisdiccion lj : l.getJurisdicciones()) {
            BigDecimal neto = lj.getImpuestoDeterminado();
            for (LiquidacionIibbComponente c : lj.getComponentes()) {
                neto = neto.add(c.getAporte());
            }
            lj.setSaldoAPagar(neto.max(BigDecimal.ZERO));
            lj.setSaldoAFavor(neto.min(BigDecimal.ZERO).negate());
        }
    }
}

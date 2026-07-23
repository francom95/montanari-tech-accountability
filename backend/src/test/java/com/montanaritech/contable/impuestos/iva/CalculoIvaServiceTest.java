package com.montanaritech.contable.impuestos.iva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLinea;
import com.montanaritech.contable.contabilidad.asiento.AsientoLineaRepository;
import com.montanaritech.contable.contabilidad.asiento.OrigenAsiento;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ResolutorCuentas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Motor de cálculo de IVA (F6.1 §1.1/§1.2): lee de los asientos, no de las facturas. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CalculoIvaServiceTest {

    @Mock
    private AsientoLineaRepository asientoLineaRepository;
    @Mock
    private ResolutorCuentas resolutorCuentas;
    @Mock
    private LiquidacionIvaRepository liquidacionIvaRepository;

    private CalculoIvaService service;

    private CuentaContable debitoFiscal;
    private CuentaContable creditoFiscal;
    private CuentaContable percepciones;

    @BeforeEach
    void setUp() {
        service = new CalculoIvaService(asientoLineaRepository, resolutorCuentas, liquidacionIvaRepository);

        debitoFiscal = cuenta(1L, "2.1.2008", CuentaContable.SaldoEsperado.ACREEDOR);
        creditoFiscal = cuenta(2L, "1.1.2006", CuentaContable.SaldoEsperado.DEUDOR);
        percepciones = cuenta(3L, "1.1.2007", CuentaContable.SaldoEsperado.DEUDOR);

        when(resolutorCuentas.resolver(ConceptoContable.IVA_DEBITO_FISCAL)).thenReturn(debitoFiscal);
        when(resolutorCuentas.resolver(ConceptoContable.IVA_CREDITO_FISCAL)).thenReturn(creditoFiscal);
        when(resolutorCuentas.resolver(ConceptoContable.PERCEPCION_IVA_SUFRIDA)).thenReturn(percepciones);
        when(asientoLineaRepository.buscarParaLiquidacionImpositiva(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(liquidacionIvaRepository.findFirstByAnioAndMesAndEstado(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void cuentaAcreedoraAcumulaPorElHaberYLaNotaDeCreditoNetea() {
        // dos ventas al haber (210 + 105) y una nota de crédito al debe (21)
        mockLineas(debitoFiscal, linea("210.00", true), linea("105.00", true), linea("21.00", false));

        var componente = componenteDe(service.calcular(2026, 3), TipoComponenteIva.DEBITO_FISCAL);

        assertThat(componente.importe()).isEqualByComparingTo("294.00");
        assertThat(componente.detalle()).hasSize(3);
    }

    @Test
    void cuentaDeudoraAcumulaPorElDebe() {
        mockLineas(creditoFiscal, linea("100.00", false), linea("50.00", false));

        var componente = componenteDe(service.calcular(2026, 3), TipoComponenteIva.CREDITO_FISCAL);

        assertThat(componente.importe()).isEqualByComparingTo("150.00");
    }

    @Test
    void percepcionesDeCompraDeCobroYBancariasSeSumanEnUnSoloComponente() {
        // las tres imputan a la misma cuenta 1.1.2007 (mapeo de F4.3), así que una
        // sola consulta las captura sin importar de qué documento vinieron
        mockLineas(percepciones, linea("30.00", false), linea("12.50", false), linea("7.25", false));

        var componente = componenteDe(service.calcular(2026, 3), TipoComponenteIva.PERCEPCIONES);

        assertThat(componente.importe()).isEqualByComparingTo("49.75");
    }

    @Test
    void elArrastreSaleDelSaldoAFavorDeLaLiquidacionAnteriorConfirmada() {
        LiquidacionIva febrero = new LiquidacionIva();
        febrero.setSaldoAFavor(new BigDecimal("1234.56"));
        when(liquidacionIvaRepository.findFirstByAnioAndMesAndEstado(2026, 2, EstadoDocumento.CONFIRMADO))
                .thenReturn(Optional.of(febrero));

        CalculoIva calculo = service.calcular(2026, 3);

        assertThat(componenteDe(calculo, TipoComponenteIva.SALDO_TECNICO_ANTERIOR).importe())
                .isEqualByComparingTo("1234.56");
        assertThat(calculo.advertencias()).isEmpty();
    }

    @Test
    void sinLiquidacionAnteriorElArrastreEsCeroPeroAvisa() {
        CalculoIva calculo = service.calcular(2026, 3);

        assertThat(componenteDe(calculo, TipoComponenteIva.SALDO_TECNICO_ANTERIOR).importe())
                .isEqualByComparingTo("0.00");
        assertThat(calculo.advertencias()).singleElement().asString().contains("02/2026");
    }

    @Test
    void enEneroElArrastreMiraDiciembreDelAnioAnterior() {
        service.calcular(2026, 1);

        assertThat(componenteDe(service.calcular(2026, 1), TipoComponenteIva.SALDO_TECNICO_ANTERIOR).descripcion())
                .contains("12/2025");
    }

    @Test
    void elPeriodoCubreElMesCompletoIncluyendoAniosBisiestos() {
        CalculoIva febrero2028 = service.calcular(2028, 2);

        assertThat(febrero2028.fechaDesde()).isEqualTo(LocalDate.of(2028, 2, 1));
        assertThat(febrero2028.fechaHasta()).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void losAsientosDeLiquidacionSeExcluyenParaQueUnPeriodoNoSeCuenteASiMismo() {
        service.calcular(2026, 3);

        org.mockito.Mockito.verify(asientoLineaRepository, org.mockito.Mockito.atLeastOnce())
                .buscarParaLiquidacionImpositiva(any(), any(), any(),
                        eq(EstadoDocumento.CONFIRMADO), eq(OrigenAsiento.LIQUIDACION_IVA));
    }

    // --- helpers ---

    private void mockLineas(CuentaContable cuenta, AsientoLinea... lineas) {
        when(asientoLineaRepository.buscarParaLiquidacionImpositiva(
                eq(Set.of(cuenta.getId())), any(), any(), any(), any()))
                .thenReturn(List.of(lineas));
    }

    private CalculoIva.ComponenteCalculado componenteDe(CalculoIva calculo, TipoComponenteIva tipo) {
        return calculo.componentes().stream()
                .filter(c -> c.tipo() == tipo)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Falta el componente " + tipo));
    }

    private CuentaContable cuenta(Long id, String codigo, CuentaContable.SaldoEsperado saldo) {
        CuentaContable c = new CuentaContable();
        c.setId(id);
        c.setCodigo(codigo);
        c.setNombre("Cuenta " + codigo);
        c.setSaldoEsperado(saldo);
        return c;
    }

    private AsientoLinea linea(String importe, boolean alHaber) {
        Asiento a = new Asiento();
        a.setId(1L);
        a.setNumero(1L);
        a.setFecha(LocalDate.of(2026, 3, 15));
        a.setDescripcion("Asiento de prueba");

        AsientoLinea l = new AsientoLinea();
        l.setAsiento(a);
        l.setOrden(1);
        l.setDebe(alHaber ? BigDecimal.ZERO : new BigDecimal(importe));
        l.setHaber(alHaber ? new BigDecimal(importe) : BigDecimal.ZERO);
        return l;
    }
}

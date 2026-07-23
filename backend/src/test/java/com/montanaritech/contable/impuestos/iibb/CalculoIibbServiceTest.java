package com.montanaritech.contable.impuestos.iibb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.contabilidad.asiento.AsientoLineaRepository;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ResolutorCuentas;
import com.montanaritech.contable.facturacion.TipoComprobante;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaRepository;
import com.montanaritech.contable.maestros.jurisdiccion.Jurisdiccion;
import com.montanaritech.contable.maestros.jurisdiccion.JurisdiccionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Motor de cálculo de IIBB (F6.2 §1.2): base total desde facturas, reparto por
 * coeficiente con default por participación de destino, impuesto determinado por
 * jurisdicción.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CalculoIibbServiceTest {

    @Mock
    private FacturaVentaRepository facturaVentaRepository;
    @Mock
    private JurisdiccionRepository jurisdiccionRepository;
    @Mock
    private LiquidacionIibbRepository liquidacionIibbRepository;
    @Mock
    private AsientoLineaRepository asientoLineaRepository;
    @Mock
    private ResolutorCuentas resolutorCuentas;

    private CalculoIibbService service;
    private Jurisdiccion caba;
    private Jurisdiccion ba;

    @BeforeEach
    void setUp() {
        service = new CalculoIibbService(facturaVentaRepository, jurisdiccionRepository,
                liquidacionIibbRepository, asientoLineaRepository, resolutorCuentas);

        caba = jurisdiccion(1L, "CABA", "3.00");
        ba = jurisdiccion(2L, "BA", "4.00");
        when(jurisdiccionRepository.findByActivoTrueOrderByCodigoAsc()).thenReturn(List.of(ba, caba));
        when(liquidacionIibbRepository.findFirstByAnioAndMesAndEstado(any(), any(), any())).thenReturn(Optional.empty());
        when(facturaVentaRepository.buscarConfirmadasParaBaseIibb(any(), any())).thenReturn(List.of());
        when(asientoLineaRepository.buscarParaLiquidacionImpositiva(any(), any(), any(), any(), any())).thenReturn(List.of());
        CuentaContable percep = new CuentaContable();
        percep.setId(8L);
        when(resolutorCuentas.resolver(ConceptoContable.PERCEPCION_IIBB_SUFRIDA)).thenReturn(percep);
    }

    @Test
    void elCoeficientePorDefectoEsLaParticipacionDeCadaJurisdiccionPorDestino() {
        // 600.000 a CABA + 400.000 a BA = base total 1.000.000
        when(facturaVentaRepository.buscarConfirmadasParaBaseIibb(any(), any()))
                .thenReturn(List.of(venta("600000.00", caba, false), venta("400000.00", ba, false)));

        CalculoIibb c = service.calcular(2026, 3);

        assertThat(c.baseTotal()).isEqualByComparingTo("1000000.00");
        CalculoIibb.JurisdiccionCalculada jCaba = de(c, "CABA");
        CalculoIibb.JurisdiccionCalculada jBa = de(c, "BA");
        assertThat(jCaba.coeficiente()).isEqualByComparingTo("0.600000");
        assertThat(jBa.coeficiente()).isEqualByComparingTo("0.400000");
        // baseImponible con el coeficiente default reproduce la atribución por destino
        assertThat(jCaba.baseImponible()).isEqualByComparingTo("600000.00");
        assertThat(jBa.baseImponible()).isEqualByComparingTo("400000.00");
        // impuesto determinado = base × alícuota
        assertThat(jCaba.impuestoDeterminado()).isEqualByComparingTo("18000.00"); // 600.000 × 3%
        assertThat(jBa.impuestoDeterminado()).isEqualByComparingTo("16000.00");   // 400.000 × 4%
    }

    @Test
    void lasNotasDeCreditoRestanDeLaBase() {
        when(facturaVentaRepository.buscarConfirmadasParaBaseIibb(any(), any()))
                .thenReturn(List.of(venta("600000.00", caba, false), venta("100000.00", caba, true)));

        CalculoIibb c = service.calcular(2026, 3);

        assertThat(c.baseTotal()).isEqualByComparingTo("500000.00");
        assertThat(de(c, "CABA").baseImponible()).isEqualByComparingTo("500000.00");
    }

    @Test
    void lasVentasSinJurisdiccionNoSeAtribuyenYAvisan() {
        when(facturaVentaRepository.buscarConfirmadasParaBaseIibb(any(), any()))
                .thenReturn(List.of(venta("600000.00", caba, false), venta("200000.00", null, false)));

        CalculoIibb c = service.calcular(2026, 3);

        // la base total incluye todo, pero el reparto por destino solo asigna lo que tiene jurisdicción
        assertThat(c.baseTotal()).isEqualByComparingTo("800000.00");
        assertThat(c.advertencias()).anyMatch(a -> a.contains("sin jurisdicción"));
    }

    @Test
    void sinLiquidacionAnteriorLosArrastresSonCeroYAvisa() {
        CalculoIibb c = service.calcular(2026, 3);

        assertThat(de(c, "CABA").saldoAFavorAnterior()).isEqualByComparingTo("0.00");
        assertThat(c.advertencias()).anyMatch(a -> a.contains("02/2026"));
    }

    @Test
    void elArrastreSaleDelSaldoAFavorDeCadaJurisdiccionDeLaLiquidacionAnterior() {
        LiquidacionIibb previa = new LiquidacionIibb();
        LiquidacionIibbJurisdiccion ljCaba = new LiquidacionIibbJurisdiccion();
        ljCaba.setJurisdiccion(caba);
        ljCaba.setSaldoAFavor(new BigDecimal("5000.00"));
        previa.getJurisdicciones().add(ljCaba);
        when(liquidacionIibbRepository.findFirstByAnioAndMesAndEstado(2026, 2,
                com.montanaritech.contable.common.estado.EstadoDocumento.CONFIRMADO)).thenReturn(Optional.of(previa));

        CalculoIibb c = service.calcular(2026, 3);

        assertThat(de(c, "CABA").saldoAFavorAnterior()).isEqualByComparingTo("5000.00");
        assertThat(de(c, "BA").saldoAFavorAnterior()).isEqualByComparingTo("0.00");
    }

    @Test
    void elPeriodoCubreElMesCompleto() {
        CalculoIibb c = service.calcular(2028, 2);
        assertThat(c.fechaDesde()).isEqualTo(java.time.LocalDate.of(2028, 2, 1));
        assertThat(c.fechaHasta()).isEqualTo(java.time.LocalDate.of(2028, 2, 29));
    }

    // --- helpers ---

    private CalculoIibb.JurisdiccionCalculada de(CalculoIibb c, String codigo) {
        return c.jurisdicciones().stream().filter(j -> j.jurisdiccionCodigo().equals(codigo)).findFirst()
                .orElseThrow(() -> new AssertionError("Falta la jurisdicción " + codigo));
    }

    private Jurisdiccion jurisdiccion(Long id, String codigo, String alicuota) {
        Jurisdiccion j = new Jurisdiccion();
        j.setId(id);
        j.setCodigo(codigo);
        j.setNombre(codigo);
        j.setAlicuotaIIBB(new BigDecimal(alicuota));
        return j;
    }

    private FacturaVenta venta(String neto, Jurisdiccion jur, boolean notaCredito) {
        FacturaVenta f = new FacturaVenta();
        f.setNetoGravado(new BigDecimal(neto));
        f.setJurisdiccionDestino(jur);
        f.setTipoComprobante(notaCredito ? TipoComprobante.NOTA_CREDITO_A : TipoComprobante.FACTURA_A);
        return f;
    }
}

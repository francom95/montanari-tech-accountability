package com.montanaritech.contable.impuestos.iva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoService;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.impuestos.iva.dto.LiquidacionIvaDtos.AgregarComponenteRequest;
import com.montanaritech.contable.impuestos.iva.dto.LiquidacionIvaDtos.AjustarComponenteRequest;
import com.montanaritech.contable.impuestos.iva.dto.LiquidacionIvaDtos.CrearRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Casos numéricos exigidos por el plan de F6.1: mes con saldo a pagar, mes con
 * saldo a favor arrastrado y mes con percepciones — más el ciclo de vida
 * (ajuste con motivo, confirmación, anulación).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LiquidacionIvaServiceTest {

    @Mock
    private LiquidacionIvaRepository repo;
    @Mock
    private CalculoIvaService calculoIvaService;
    @Mock
    private CuentaContableRepository cuentaContableRepository;
    @Mock
    private LiquidacionIvaAsientoGenerator asientoGenerator;
    @Mock
    private AsientoService asientoService;
    @Mock
    private com.montanaritech.contable.common.audit.AuditoriaService auditoria;

    private LiquidacionIvaService service;

    @BeforeEach
    void setUp() {
        service = new LiquidacionIvaService(repo, calculoIvaService, cuentaContableRepository,
                asientoGenerator, asientoService, new LiquidacionIvaMapper(), auditoria);

        when(repo.findByAnioAndMesAndEstadoIn(any(), any(), any())).thenReturn(List.of());
        // el save devuelve la misma instancia con ids asignados a los componentes,
        // para poder ajustarlos después sin una BD real
        AtomicLong seq = new AtomicLong(1);
        when(repo.save(any(LiquidacionIva.class))).thenAnswer(inv -> {
            LiquidacionIva l = inv.getArgument(0);
            if (l.getId() == null) {
                l.setId(100L);
            }
            l.getComponentes().stream()
                    .filter(c -> c.getId() == null)
                    .forEach(c -> c.setId(seq.getAndIncrement()));
            return l;
        });
    }

    @Test
    void mesConSaldoAPagar() {
        // débito 210.000, crédito 84.000, sin percepciones ni arrastre
        mockCalculo("210000.00", "84000.00", "0.00", "0.00");

        LiquidacionIva l = service.crearBorrador(new CrearRequest(2026, 3));

        assertThat(l.getSaldoAPagar()).isEqualByComparingTo("126000.00");
        assertThat(l.getSaldoAFavor()).isEqualByComparingTo("0.00");
    }

    @Test
    void mesConSaldoAFavorArrastradoDelPeriodoAnterior() {
        // el arrastre de 50.000 da vuelta el resultado: sin él serían 30.000 a pagar
        mockCalculo("120000.00", "90000.00", "0.00", "50000.00");

        LiquidacionIva l = service.crearBorrador(new CrearRequest(2026, 4));

        assertThat(l.getSaldoAPagar()).isEqualByComparingTo("0.00");
        assertThat(l.getSaldoAFavor()).isEqualByComparingTo("20000.00");
    }

    @Test
    void mesConPercepcionesQueReducenElSaldoAPagar() {
        mockCalculo("210000.00", "84000.00", "15500.50", "0.00");

        LiquidacionIva l = service.crearBorrador(new CrearRequest(2026, 5));

        assertThat(l.getSaldoAPagar()).isEqualByComparingTo("110499.50");
    }

    @Test
    void percepcionesQueSuperanElImpuestoDeterminadoDejanSaldoAFavor() {
        mockCalculo("100000.00", "40000.00", "75000.00", "0.00");

        LiquidacionIva l = service.crearBorrador(new CrearRequest(2026, 6));

        assertThat(l.getSaldoAPagar()).isEqualByComparingTo("0.00");
        assertThat(l.getSaldoAFavor()).isEqualByComparingTo("15000.00");
    }

    @Test
    void unAjusteManualRecalculaElResultado() {
        mockCalculo("210000.00", "84000.00", "0.00", "0.00");
        LiquidacionIva l = service.crearBorrador(new CrearRequest(2026, 3));
        when(repo.findById(100L)).thenReturn(Optional.of(l));
        Long idDebitoFiscal = componente(l, TipoComponenteIva.DEBITO_FISCAL).getId();

        service.ajustarComponente(100L, idDebitoFiscal,
                new AjustarComponenteRequest(new BigDecimal("10000.00"), "Factura 0001-00000123 cargada tarde"));

        assertThat(l.getSaldoAPagar()).isEqualByComparingTo("136000.00");
        assertThat(componente(l, TipoComponenteIva.DEBITO_FISCAL).getImporteFinal()).isEqualByComparingTo("220000.00");
    }

    @Test
    void unAjusteSinMotivoSeRechaza() {
        mockCalculo("210000.00", "84000.00", "0.00", "0.00");
        LiquidacionIva l = service.crearBorrador(new CrearRequest(2026, 3));
        when(repo.findById(100L)).thenReturn(Optional.of(l));
        Long id = componente(l, TipoComponenteIva.DEBITO_FISCAL).getId();

        assertThatThrownBy(() -> service.ajustarComponente(100L, id,
                new AjustarComponenteRequest(new BigDecimal("10000.00"), "   ")))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("necesita un motivo");
    }

    @Test
    void unAjusteEnCeroNoNecesitaMotivo() {
        mockCalculo("210000.00", "84000.00", "0.00", "0.00");
        LiquidacionIva l = service.crearBorrador(new CrearRequest(2026, 3));
        when(repo.findById(100L)).thenReturn(Optional.of(l));
        Long id = componente(l, TipoComponenteIva.DEBITO_FISCAL).getId();

        service.ajustarComponente(100L, id, new AjustarComponenteRequest(BigDecimal.ZERO, null));

        assertThat(componente(l, TipoComponenteIva.DEBITO_FISCAL).getMotivoAjuste()).isNull();
    }

    @Test
    void noSePuedeAgregarAManoUnComponenteQueElSistemaYaCalcula() {
        mockCalculo("100.00", "0.00", "0.00", "0.00");
        LiquidacionIva l = service.crearBorrador(new CrearRequest(2026, 3));
        when(repo.findById(100L)).thenReturn(Optional.of(l));

        assertThatThrownBy(() -> service.agregarComponente(100L, new AgregarComponenteRequest(
                TipoComponenteIva.DEBITO_FISCAL, "Otro débito", new BigDecimal("50.00"), 1L, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("se calcula automáticamente");
    }

    @Test
    void unComponenteAutomaticoNoSePuedeEliminar() {
        mockCalculo("100.00", "0.00", "0.00", "0.00");
        LiquidacionIva l = service.crearBorrador(new CrearRequest(2026, 3));
        when(repo.findById(100L)).thenReturn(Optional.of(l));
        Long id = componente(l, TipoComponenteIva.DEBITO_FISCAL).getId();

        assertThatThrownBy(() -> service.eliminarComponente(100L, id))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("no se puede eliminar");
    }

    @Test
    void unaRestitucionManualAumentaElSaldoAPagar() {
        mockCalculo("100000.00", "0.00", "0.00", "0.00");
        LiquidacionIva l = service.crearBorrador(new CrearRequest(2026, 3));
        when(repo.findById(100L)).thenReturn(Optional.of(l));
        CuentaContable cuenta = new CuentaContable();
        cuenta.setId(9L);
        cuenta.setCodigo("5.3.2007");
        when(cuentaContableRepository.findById(9L)).thenReturn(Optional.of(cuenta));

        service.agregarComponente(100L, new AgregarComponenteRequest(
                TipoComponenteIva.RESTITUCIONES, "Restitución", new BigDecimal("5000.00"), 9L, "Ajuste del contador"));

        assertThat(l.getSaldoAPagar()).isEqualByComparingTo("105000.00");
    }

    @Test
    void confirmarGeneraElAsientoYDejaLaLiquidacionConfirmada() {
        mockCalculo("210000.00", "84000.00", "0.00", "0.00");
        LiquidacionIva l = service.crearBorrador(new CrearRequest(2026, 3));
        when(repo.findById(100L)).thenReturn(Optional.of(l));
        Asiento asiento = new Asiento();
        asiento.setId(77L);
        asiento.setNumero(12L);
        when(asientoService.registrarAutomatico(any())).thenReturn(asiento);

        service.confirmar(100L);

        assertThat(l.getEstado()).isEqualTo(EstadoDocumento.CONFIRMADO);
        assertThat(l.getAsiento().getId()).isEqualTo(77L);
    }

    @Test
    void unaLiquidacionConfirmadaYaNoSePuedeAjustar() {
        mockCalculo("210000.00", "84000.00", "0.00", "0.00");
        LiquidacionIva l = service.crearBorrador(new CrearRequest(2026, 3));
        l.setEstado(EstadoDocumento.CONFIRMADO);
        when(repo.findById(100L)).thenReturn(Optional.of(l));
        Long id = componente(l, TipoComponenteIva.DEBITO_FISCAL).getId();

        assertThatThrownBy(() -> service.ajustarComponente(100L, id,
                new AjustarComponenteRequest(new BigDecimal("1.00"), "tarde")))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("solo se puede ajustar una liquidación en borrador".substring(5));
    }

    @Test
    void desconfirmarAnulaElAsientoYLiberaElPeriodo() {
        mockCalculo("210000.00", "84000.00", "0.00", "0.00");
        LiquidacionIva l = service.crearBorrador(new CrearRequest(2026, 3));
        Asiento asiento = new Asiento();
        asiento.setId(77L);
        l.setAsiento(asiento);
        l.setEstado(EstadoDocumento.CONFIRMADO);
        when(repo.findById(100L)).thenReturn(Optional.of(l));

        service.anular(100L, "El contador rehace el mes");

        assertThat(l.getEstado()).isEqualTo(EstadoDocumento.ANULADO);
        verify(asientoService).anularPorDocumento(77L, "El contador rehace el mes");
    }

    @Test
    void anularUnBorradorNoIntentaAnularUnAsientoInexistente() {
        mockCalculo("210000.00", "84000.00", "0.00", "0.00");
        LiquidacionIva l = service.crearBorrador(new CrearRequest(2026, 3));
        l.setEstado(EstadoDocumento.CONFIRMADO); // PL-5 exige pasar por confirmado
        when(repo.findById(100L)).thenReturn(Optional.of(l));

        service.anular(100L, "sin asiento");

        verify(asientoService, never()).anularPorDocumento(any(), any());
    }

    @Test
    void noSePuedenTenerDosLiquidacionesVivasDelMismoPeriodo() {
        when(repo.findByAnioAndMesAndEstadoIn(any(), any(), any()))
                .thenReturn(List.of(new LiquidacionIva()));

        assertThatThrownBy(() -> service.crearBorrador(new CrearRequest(2026, 3)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Ya existe una liquidación");
    }

    @Test
    void recalcularConservaLosAjustesManualesYLosConceptosAgregados() {
        mockCalculo("100000.00", "0.00", "0.00", "0.00");
        LiquidacionIva l = service.crearBorrador(new CrearRequest(2026, 3));
        when(repo.findById(100L)).thenReturn(Optional.of(l));
        Long id = componente(l, TipoComponenteIva.DEBITO_FISCAL).getId();
        service.ajustarComponente(100L, id, new AjustarComponenteRequest(new BigDecimal("500.00"), "ajuste"));

        // se confirmaron más facturas del período: el débito calculado sube a 150.000
        mockCalculo("150000.00", "0.00", "0.00", "0.00");
        service.recalcular(100L);

        LiquidacionIvaComponente debito = componente(l, TipoComponenteIva.DEBITO_FISCAL);
        assertThat(debito.getImporteCalculado()).isEqualByComparingTo("150000.00");
        assertThat(debito.getImporteAjuste()).as("el ajuste manual sobrevive al recálculo")
                .isEqualByComparingTo("500.00");
        assertThat(l.getSaldoAPagar()).isEqualByComparingTo("150500.00");
    }

    // --- helpers ---

    private void mockCalculo(String debito, String credito, String percepciones, String arrastre) {
        when(calculoIvaService.calcular(any(Integer.class), any(Integer.class))).thenAnswer(inv -> new CalculoIva(
                inv.getArgument(0), inv.getArgument(1),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                List.of(
                        comp(TipoComponenteIva.DEBITO_FISCAL, debito),
                        comp(TipoComponenteIva.CREDITO_FISCAL, credito),
                        comp(TipoComponenteIva.PERCEPCIONES, percepciones),
                        comp(TipoComponenteIva.SALDO_TECNICO_ANTERIOR, arrastre)),
                List.of()));
    }

    private CalculoIva.ComponenteCalculado comp(TipoComponenteIva tipo, String importe) {
        return new CalculoIva.ComponenteCalculado(tipo, tipo.getDescripcionPorDefecto(),
                new BigDecimal(importe), List.of());
    }

    private LiquidacionIvaComponente componente(LiquidacionIva l, TipoComponenteIva tipo) {
        return l.getComponentes().stream()
                .filter(c -> c.getTipo() == tipo)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Falta el componente " + tipo));
    }
}

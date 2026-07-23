package com.montanaritech.contable.impuestos.iibb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoService;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.AjustarComponenteRequest;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.CrearRequest;
import com.montanaritech.contable.impuestos.iibb.dto.LiquidacionIibbDtos.EditarJurisdiccionRequest;
import com.montanaritech.contable.maestros.jurisdiccion.Jurisdiccion;
import com.montanaritech.contable.maestros.jurisdiccion.JurisdiccionRepository;
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
 * Ciclo de vida de IIBB (F6.2): el caso que pide el plan es un mes con al menos
 * dos jurisdicciones, cada una con su base, alícuota, impuesto y deducciones.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LiquidacionIibbServiceTest {

    @Mock
    private LiquidacionIibbRepository repo;
    @Mock
    private CalculoIibbService calculoIibbService;
    @Mock
    private JurisdiccionRepository jurisdiccionRepository;
    @Mock
    private CuentaContableRepository cuentaContableRepository;
    @Mock
    private LiquidacionIibbAsientoGenerator asientoGenerator;
    @Mock
    private AsientoService asientoService;
    @Mock
    private com.montanaritech.contable.common.audit.AuditoriaService auditoria;

    private LiquidacionIibbService service;
    private Jurisdiccion caba;
    private Jurisdiccion ba;

    @BeforeEach
    void setUp() {
        service = new LiquidacionIibbService(repo, calculoIibbService, jurisdiccionRepository,
                cuentaContableRepository, asientoGenerator, asientoService, new LiquidacionIibbMapper(), auditoria);

        caba = jurisdiccion(1L, "CABA", "3.00");
        ba = jurisdiccion(2L, "BA", "4.00");
        when(jurisdiccionRepository.findById(1L)).thenReturn(Optional.of(caba));
        when(jurisdiccionRepository.findById(2L)).thenReturn(Optional.of(ba));
        when(repo.findByAnioAndMesAndEstadoIn(any(), any(), any())).thenReturn(List.of());

        AtomicLong seq = new AtomicLong(1);
        when(repo.save(any(LiquidacionIibb.class))).thenAnswer(inv -> {
            LiquidacionIibb l = inv.getArgument(0);
            if (l.getId() == null) {
                l.setId(100L);
            }
            for (LiquidacionIibbJurisdiccion j : l.getJurisdicciones()) {
                if (j.getId() == null) {
                    j.setId(seq.getAndIncrement());
                }
                j.getComponentes().stream().filter(c -> c.getId() == null).forEach(c -> c.setId(seq.getAndIncrement()));
            }
            return l;
        });
    }

    @Test
    void mesConDosJurisdiccionesCadaUnaConSuImpuestoDeterminado() {
        mockCalculo(base(caba, "600000.00", "0.600000", "18000.00"),
                base(ba, "400000.00", "0.400000", "16000.00"));

        LiquidacionIibb l = service.crearBorrador(new CrearRequest(2026, 3));

        assertThat(l.getJurisdicciones()).hasSize(2);
        assertThat(jur(l, "CABA").getSaldoAPagar()).isEqualByComparingTo("18000.00");
        assertThat(jur(l, "BA").getSaldoAPagar()).isEqualByComparingTo("16000.00");
        assertThat(l.getSaldoAPagarTotal()).isEqualByComparingTo("34000.00");
    }

    @Test
    void cargarUnaDeduccionReduceElSaldoAPagarDeEsaJurisdiccion() {
        mockCalculo(base(caba, "600000.00", "0.600000", "18000.00"));
        LiquidacionIibb l = service.crearBorrador(new CrearRequest(2026, 3));
        when(repo.findById(100L)).thenReturn(Optional.of(l));
        Long jurId = jur(l, "CABA").getId();
        Long percepId = componente(l, "CABA", TipoComponenteIibb.PERCEPCIONES).getId();

        // cargar 5.000 de percepciones — no necesita motivo (dato, no corrección)
        service.ajustarComponente(100L, jurId, percepId, new AjustarComponenteRequest(new BigDecimal("5000.00"), null));

        assertThat(jur(l, "CABA").getSaldoAPagar()).isEqualByComparingTo("13000.00");
    }

    @Test
    void unaJurisdiccionConMasDeduccionesQueImpuestoQuedaConSaldoAFavor() {
        mockCalculo(base(caba, "600000.00", "0.600000", "18000.00"));
        LiquidacionIibb l = service.crearBorrador(new CrearRequest(2026, 3));
        when(repo.findById(100L)).thenReturn(Optional.of(l));
        Long jurId = jur(l, "CABA").getId();
        Long percepId = componente(l, "CABA", TipoComponenteIibb.PERCEPCIONES).getId();

        service.ajustarComponente(100L, jurId, percepId, new AjustarComponenteRequest(new BigDecimal("25000.00"), null));

        assertThat(jur(l, "CABA").getSaldoAPagar()).isEqualByComparingTo("0.00");
        assertThat(jur(l, "CABA").getSaldoAFavor()).isEqualByComparingTo("7000.00");
    }

    @Test
    void editarElCoeficienteRecalculaLaBaseYElImpuestoDeterminado() {
        mockCalculo(base(caba, "600000.00", "0.600000", "18000.00"));
        LiquidacionIibb l = service.crearBorrador(new CrearRequest(2026, 3));
        l.setBaseTotal(new BigDecimal("1000000.00"));
        when(repo.findById(100L)).thenReturn(Optional.of(l));
        Long jurId = jur(l, "CABA").getId();

        // el contador carga el coeficiente CM real: 0.5 en vez del 0.6 por destino
        service.editarJurisdiccion(100L, jurId, new EditarJurisdiccionRequest(new BigDecimal("0.500000"), new BigDecimal("3.00")));

        assertThat(jur(l, "CABA").getBaseImponible()).isEqualByComparingTo("500000.00");
        assertThat(jur(l, "CABA").getImpuestoDeterminado()).isEqualByComparingTo("15000.00");
        assertThat(jur(l, "CABA").getSaldoAPagar()).isEqualByComparingTo("15000.00");
    }

    @Test
    void corregirElArrastreNecesitaMotivo() {
        mockCalculo(baseConArrastre(caba, "600000.00", "0.600000", "18000.00", "5000.00"));
        LiquidacionIibb l = service.crearBorrador(new CrearRequest(2026, 3));
        when(repo.findById(100L)).thenReturn(Optional.of(l));
        Long jurId = jur(l, "CABA").getId();
        Long arrastreId = componente(l, "CABA", TipoComponenteIibb.SALDO_A_FAVOR_ANTERIOR).getId();

        assertThatThrownBy(() -> service.ajustarComponente(100L, jurId, arrastreId,
                new AjustarComponenteRequest(new BigDecimal("1000.00"), null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("necesita un motivo");
    }

    @Test
    void noSePuedenTenerDosLiquidacionesVivasDelMismoPeriodo() {
        when(repo.findByAnioAndMesAndEstadoIn(any(), any(), any())).thenReturn(List.of(new LiquidacionIibb()));

        assertThatThrownBy(() -> service.crearBorrador(new CrearRequest(2026, 3)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Ya existe una liquidación");
    }

    @Test
    void confirmarGeneraElAsientoYDejaLaLiquidacionConfirmada() {
        mockCalculo(base(caba, "600000.00", "0.600000", "18000.00"));
        LiquidacionIibb l = service.crearBorrador(new CrearRequest(2026, 3));
        when(repo.findById(100L)).thenReturn(Optional.of(l));
        Asiento asiento = new Asiento();
        asiento.setId(55L);
        asiento.setNumero(7L);
        when(asientoService.registrarAutomatico(any())).thenReturn(asiento);

        service.confirmar(100L);

        assertThat(l.getEstado()).isEqualTo(EstadoDocumento.CONFIRMADO);
        assertThat(l.getAsiento().getId()).isEqualTo(55L);
    }

    @Test
    void desconfirmarAnulaElAsientoYLiberaElPeriodo() {
        mockCalculo(base(caba, "600000.00", "0.600000", "18000.00"));
        LiquidacionIibb l = service.crearBorrador(new CrearRequest(2026, 3));
        Asiento asiento = new Asiento();
        asiento.setId(55L);
        l.setAsiento(asiento);
        l.setEstado(EstadoDocumento.CONFIRMADO);
        when(repo.findById(100L)).thenReturn(Optional.of(l));

        service.anular(100L, "El contador rehace el mes");

        assertThat(l.getEstado()).isEqualTo(EstadoDocumento.ANULADO);
        verify(asientoService).anularPorDocumento(55L, "El contador rehace el mes");
    }

    // --- helpers ---

    private record BaseJur(Jurisdiccion jur, String base, String coef, String determinado, String arrastre) {
    }

    private BaseJur base(Jurisdiccion jur, String base, String coef, String determinado) {
        return new BaseJur(jur, base, coef, determinado, "0.00");
    }

    private BaseJur baseConArrastre(Jurisdiccion jur, String base, String coef, String determinado, String arrastre) {
        return new BaseJur(jur, base, coef, determinado, arrastre);
    }

    private void mockCalculo(BaseJur... jurs) {
        when(calculoIibbService.calcular(any(Integer.class), any(Integer.class))).thenAnswer(inv -> {
            List<CalculoIibb.JurisdiccionCalculada> lista = new java.util.ArrayList<>();
            for (BaseJur b : jurs) {
                lista.add(new CalculoIibb.JurisdiccionCalculada(b.jur().getId(), b.jur().getCodigo(), b.jur().getNombre(),
                        new BigDecimal(b.base()), new BigDecimal(b.coef()), new BigDecimal(b.base()),
                        b.jur().getAlicuotaIIBB(), new BigDecimal(b.determinado()), new BigDecimal(b.arrastre())));
            }
            return new CalculoIibb(inv.getArgument(0), inv.getArgument(1),
                    LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                    new BigDecimal("1000000.00"), BigDecimal.ZERO, lista, List.of());
        });
    }

    private LiquidacionIibbJurisdiccion jur(LiquidacionIibb l, String codigo) {
        return l.getJurisdicciones().stream().filter(j -> j.getJurisdiccion().getCodigo().equals(codigo)).findFirst()
                .orElseThrow(() -> new AssertionError("Falta la jurisdicción " + codigo));
    }

    private LiquidacionIibbComponente componente(LiquidacionIibb l, String codigo, TipoComponenteIibb tipo) {
        return jur(l, codigo).getComponentes().stream().filter(c -> c.getTipo() == tipo).findFirst()
                .orElseThrow(() -> new AssertionError("Falta el componente " + tipo + " en " + codigo));
    }

    private Jurisdiccion jurisdiccion(Long id, String codigo, String alicuota) {
        Jurisdiccion j = new Jurisdiccion();
        j.setId(id);
        j.setCodigo(codigo);
        j.setNombre(codigo);
        j.setAlicuotaIIBB(new BigDecimal(alicuota));
        return j;
    }
}

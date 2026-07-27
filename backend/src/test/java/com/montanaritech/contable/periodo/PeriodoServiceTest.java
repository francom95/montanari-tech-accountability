package com.montanaritech.contable.periodo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.bancos.conciliacion.ConciliacionService;
import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.PeriodoCerradoException;
import com.montanaritech.contable.contabilidad.asiento.AsientoRepository;
import com.montanaritech.contable.impuestos.iibb.LiquidacionIibbRepository;
import com.montanaritech.contable.impuestos.iva.LiquidacionIvaRepository;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Gate único de escritura de F9.3: {@link PeriodoService#estaCerrado} (abierto
 * implícito sin fila / ABIERTO / EN_REVISION, cerrado solo con fila CERRADO),
 * {@link PeriodoService#verificarEscritura} (carga bloqueada, admin sin motivo
 * bloqueado, admin con motivo pasa) y el motor on-demand de generación
 * (idempotente, molde de {@code VencimientoService.generarAutomaticos()}).
 */
@ExtendWith(MockitoExtension.class)
class PeriodoServiceTest {

    @Mock private PeriodoRepository repo;
    @Mock private PeriodoMapper mapper;
    @Mock private AsientoRepository asientoRepo;
    @Mock private LiquidacionIvaRepository liquidacionIvaRepo;
    @Mock private LiquidacionIibbRepository liquidacionIibbRepo;
    @Mock private CuentaBancariaRepository cuentaBancariaRepo;
    @Mock private ConciliacionService conciliacionService;
    @Mock private AuditoriaService auditoria;

    private PeriodoService service;

    @BeforeEach
    void setUp() {
        service = new PeriodoService(repo, mapper, asientoRepo, liquidacionIvaRepo, liquidacionIibbRepo,
                cuentaBancariaRepo, conciliacionService, auditoria);
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String rol) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }

    private Periodo periodo(EstadoPeriodo estado) {
        Periodo p = new Periodo();
        p.setId(1L);
        p.setAnio(2026);
        p.setMes(6);
        p.setEstado(estado);
        return p;
    }

    // ---- estaCerrado ----

    @Test
    void estaCerradoSinFilaEsAbiertoImplicito() {
        when(repo.findByAnioAndMes(2026, 6)).thenReturn(Optional.empty());

        assertThat(service.estaCerrado(LocalDate.of(2026, 6, 15))).isFalse();
    }

    @Test
    void estaCerradoConFilaAbiertaNoEstaCerrado() {
        when(repo.findByAnioAndMes(2026, 6)).thenReturn(Optional.of(periodo(EstadoPeriodo.ABIERTO)));

        assertThat(service.estaCerrado(LocalDate.of(2026, 6, 15))).isFalse();
    }

    @Test
    void estaCerradoConFilaEnRevisionNoEstaCerrado() {
        when(repo.findByAnioAndMes(2026, 6)).thenReturn(Optional.of(periodo(EstadoPeriodo.EN_REVISION)));

        assertThat(service.estaCerrado(LocalDate.of(2026, 6, 15))).isFalse();
    }

    @Test
    void estaCerradoConFilaCerradaEstaCerrado() {
        when(repo.findByAnioAndMes(2026, 6)).thenReturn(Optional.of(periodo(EstadoPeriodo.CERRADO)));

        assertThat(service.estaCerrado(LocalDate.of(2026, 6, 15))).isTrue();
    }

    // ---- verificarEscritura ----

    @Test
    void verificarEscrituraConPeriodoAbiertoEsNoOp() {
        when(repo.findByAnioAndMes(2026, 6)).thenReturn(Optional.empty());

        assertThat(service.verificarEscritura(LocalDate.of(2026, 6, 15), false, null)).isFalse();
    }

    @Test
    void verificarEscrituraConPeriodoCerradoYUsuarioCargaLanzaPeriodoCerrado() {
        lenient().when(repo.findByAnioAndMes(2026, 6)).thenReturn(Optional.of(periodo(EstadoPeriodo.CERRADO)));
        autenticarComo("CARGA");

        assertThatThrownBy(() -> service.verificarEscritura(LocalDate.of(2026, 6, 15), true, "motivo cualquiera"))
                .isInstanceOf(PeriodoCerradoException.class)
                .extracting(e -> ((PeriodoCerradoException) e).getCodigo())
                .isEqualTo("PERIODO_CERRADO");
    }

    @Test
    void verificarEscrituraConPeriodoCerradoYAdminSinConfirmarLanzaPeriodoCerrado() {
        lenient().when(repo.findByAnioAndMes(2026, 6)).thenReturn(Optional.of(periodo(EstadoPeriodo.CERRADO)));
        autenticarComo("ADMINISTRADOR");

        assertThatThrownBy(() -> service.verificarEscritura(LocalDate.of(2026, 6, 15), false, null))
                .isInstanceOf(PeriodoCerradoException.class);
    }

    @Test
    void verificarEscrituraConPeriodoCerradoYAdminSinMotivoLanzaPeriodoCerrado() {
        lenient().when(repo.findByAnioAndMes(2026, 6)).thenReturn(Optional.of(periodo(EstadoPeriodo.CERRADO)));
        autenticarComo("ADMINISTRADOR");

        assertThatThrownBy(() -> service.verificarEscritura(LocalDate.of(2026, 6, 15), true, "  "))
                .isInstanceOf(PeriodoCerradoException.class);
    }

    @Test
    void verificarEscrituraConPeriodoCerradoYAdminConMotivoPasaYRetornaTrue() {
        lenient().when(repo.findByAnioAndMes(2026, 6)).thenReturn(Optional.of(periodo(EstadoPeriodo.CERRADO)));
        autenticarComo("ADMINISTRADOR");

        boolean sobrePeriodoCerrado = service.verificarEscritura(LocalDate.of(2026, 6, 15), true, "corrección autorizada");

        assertThat(sobrePeriodoCerrado).isTrue();
    }

    // ---- generarAutomaticos: idempotente ----

    @Test
    void generarAutomaticosCreaUnAbiertoPorCadaAnioMesSinFilaTodavia() {
        List<Object[]> filas = List.of(new Object[][] {{2026, 5}, {2026, 6}});
        when(asientoRepo.findDistinctAnioMesConAsientos()).thenReturn(filas);
        when(repo.existsByAnioAndMes(2026, 5)).thenReturn(false);
        when(repo.existsByAnioAndMes(2026, 6)).thenReturn(true);

        int generados = service.generarAutomaticos();

        assertThat(generados).isEqualTo(1);
        verify(repo, times(1)).save(any(Periodo.class));
    }

    @Test
    void generarAutomaticosLlamadoDosVecesNoDuplica() {
        List<Object[]> filas = List.of(new Object[][] {{2026, 5}});
        when(asientoRepo.findDistinctAnioMesConAsientos()).thenReturn(filas);
        when(repo.existsByAnioAndMes(2026, 5)).thenReturn(false).thenReturn(true);

        assertThat(service.generarAutomaticos()).isEqualTo(1);
        assertThat(service.generarAutomaticos()).isEqualTo(0);
        verify(repo, times(1)).save(any(Periodo.class));
    }

    // ---- cerrar / reabrir: auditoría reforzada ----

    @Test
    void cerrarMarcaEstadoYAuditaConCerrarPeriodo() {
        when(repo.findById(1L)).thenReturn(Optional.of(periodo(EstadoPeriodo.ABIERTO)));

        Periodo cerrado = service.cerrar(1L, "cierre de junio");

        assertThat(cerrado.getEstado()).isEqualTo(EstadoPeriodo.CERRADO);
        assertThat(cerrado.getMotivoCierre()).isEqualTo("cierre de junio");
        verify(auditoria).registrar(eq(AccionAuditoria.CERRAR_PERIODO), eq("Periodo"), eq(1L), any(), any());
    }

    @Test
    void reabrirMarcaEstadoYAuditaConReabrirPeriodo() {
        when(repo.findById(1L)).thenReturn(Optional.of(periodo(EstadoPeriodo.CERRADO)));

        Periodo reabierto = service.reabrir(1L, "corrección necesaria");

        assertThat(reabierto.getEstado()).isEqualTo(EstadoPeriodo.ABIERTO);
        assertThat(reabierto.getMotivoReapertura()).isEqualTo("corrección necesaria");
        verify(auditoria).registrar(eq(AccionAuditoria.REABRIR_PERIODO), eq("Periodo"), eq(1L), any(), any());
    }

    @Test
    void reabrirUnPeriodoNoCerradoLanzaError() {
        when(repo.findById(1L)).thenReturn(Optional.of(periodo(EstadoPeriodo.ABIERTO)));

        assertThatThrownBy(() -> service.reabrir(1L, "motivo"))
                .hasFieldOrPropertyWithValue("codigo", "PERIODO_NO_CERRADO");
    }
}

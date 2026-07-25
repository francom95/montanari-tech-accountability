package com.montanaritech.contable.alerta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.bancos.conciliacion.ConciliacionService;
import com.montanaritech.contable.bancos.conciliacion.dto.ConciliacionResumenResponse;
import com.montanaritech.contable.bancos.movimientobancario.EstadoMovimientoBancario;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancarioRepository;
import com.montanaritech.contable.common.reporte.EstadoVencimiento;
import com.montanaritech.contable.common.saldo.RecalculoSaldoService;
import com.montanaritech.contable.compromiso.CompromisoService;
import com.montanaritech.contable.facturacion.cuentasporcobrar.CuentaPorCobrarService;
import com.montanaritech.contable.facturacion.cuentasporcobrar.dto.CuentaPorCobrarFilaResponse;
import com.montanaritech.contable.facturacion.cuentasporcobrar.dto.CuentaPorCobrarResponse;
import com.montanaritech.contable.facturacion.cuentasporpagar.CuentaPorPagarService;
import com.montanaritech.contable.facturacion.cuentasporpagar.dto.CuentaPorPagarResponse;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.pendiente.EstadoPendiente;
import com.montanaritech.contable.pendiente.PendienteAdministrativo;
import com.montanaritech.contable.pendiente.PendienteAdministrativoService;
import com.montanaritech.contable.pendiente.PrioridadPendiente;
import com.montanaritech.contable.vencimientos.VencimientoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cubre el diff genérico de {@code sincronizarTipo} (crear/reactivar/no-duplicar/auto-resolver)
 * a través de PENDIENTE_ADMINISTRATIVO_PROXIMO (la fuente más simple de estabilizar), y la
 * aplicación de {@code diasAnticipacion}/{@code diasAtrasoCxc} de {@link ConfiguracionAlertas}.
 */
@ExtendWith(MockitoExtension.class)
class MotorAlertasServiceTest {

    @Mock private AlertaRepository alertaRepo;
    @Mock private ConfiguracionAlertasRepository configuracionRepo;
    @Mock private VencimientoService vencimientoService;
    @Mock private CompromisoService compromisoService;
    @Mock private CuentaPorCobrarService cuentaPorCobrarService;
    @Mock private CuentaPorPagarService cuentaPorPagarService;
    @Mock private CuentaBancariaRepository cuentaBancariaRepo;
    @Mock private RecalculoSaldoService recalculoSaldoService;
    @Mock private MovimientoBancarioRepository movimientoBancarioRepo;
    @Mock private ConciliacionService conciliacionService;
    @Mock private PendienteAdministrativoService pendienteAdministrativoService;
    @Mock private AlertChannel alertChannel;

    private MotorAlertasService motor;

    @BeforeEach
    void setUp() {
        motor = new MotorAlertasService(alertaRepo, configuracionRepo, vencimientoService, compromisoService,
                cuentaPorCobrarService, cuentaPorPagarService, cuentaBancariaRepo, recalculoSaldoService,
                movimientoBancarioRepo, conciliacionService, pendienteAdministrativoService, alertChannel);

        lenient().when(configuracionRepo.findFirstByOrderByIdAsc()).thenReturn(Optional.of(nuevaConfiguracion(7, 0)));
        lenient().when(vencimientoService.proximos(anyInt())).thenReturn(List.of());
        lenient().when(vencimientoService.vencidos()).thenReturn(List.of());
        lenient().when(compromisoService.porRangoDeFechas(any(), any())).thenReturn(List.of());
        lenient().when(cuentaPorPagarService.calcular(any(), any(), any(), any(), any(), eq(EstadoVencimiento.POR_VENCER)))
                .thenReturn(new CuentaPorPagarResponse(List.of(), List.of()));
        lenient().when(cuentaPorCobrarService.calcular(any(), any(), any(), any(), any(), eq(EstadoVencimiento.VENCIDO)))
                .thenReturn(new CuentaPorCobrarResponse(List.of(), List.of()));
        lenient().when(cuentaBancariaRepo.findByActivoTrue()).thenReturn(List.of());
        lenient().when(movimientoBancarioRepo.buscar(any(), eq(EstadoMovimientoBancario.PENDIENTE), any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        lenient().when(pendienteAdministrativoService.proximosAVencer(anyInt())).thenReturn(List.of());
        lenient().when(alertaRepo.findByTipoAndEntidadTipoAndEntidadRefId(any(), any(), any())).thenReturn(Optional.empty());
        lenient().when(alertaRepo.findByTipoAndEstado(any(), any())).thenReturn(List.of());
        lenient().when(alertaRepo.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ConfiguracionAlertas nuevaConfiguracion(int diasAnticipacion, int diasAtrasoCxc) {
        ConfiguracionAlertas c = new ConfiguracionAlertas();
        c.setDiasAnticipacion(diasAnticipacion);
        c.setDiasAtrasoCxc(diasAtrasoCxc);
        return c;
    }

    private PendienteAdministrativo pendiente(Long id, LocalDate fecha) {
        PendienteAdministrativo p = new PendienteAdministrativo();
        p.setId(id);
        p.setTitulo("Revisar IVA");
        p.setFechaEstimadaResolucion(fecha);
        p.setPrioridad(PrioridadPendiente.MEDIA);
        p.setEstado(EstadoPendiente.PENDIENTE);
        return p;
    }

    @Test
    void sincronizarCreaAlertaNuevaCuandoHayCondicionVigente() {
        when(pendienteAdministrativoService.proximosAVencer(7)).thenReturn(List.of(pendiente(1L, LocalDate.now().plusDays(3))));

        motor.sincronizar();

        var captor = org.mockito.ArgumentCaptor.forClass(Alerta.class);
        verify(alertaRepo).save(captor.capture());
        Alerta guardada = captor.getValue();
        assertThat(guardada.getTipo()).isEqualTo(TipoAlerta.PENDIENTE_ADMINISTRATIVO_PROXIMO);
        assertThat(guardada.getEntidadTipo()).isEqualTo(TipoEntidadAlerta.PENDIENTE_ADMINISTRATIVO);
        assertThat(guardada.getEntidadRefId()).isEqualTo(1L);
        assertThat(guardada.getEstado()).isEqualTo(EstadoAlerta.ACTIVA);
        verify(alertChannel).notificar(guardada);
    }

    @Test
    void sincronizarReactivaAlertaResueltaCuandoLaCondicionReaparece() {
        Alerta resuelta = new Alerta();
        resuelta.setTipo(TipoAlerta.PENDIENTE_ADMINISTRATIVO_PROXIMO);
        resuelta.setEntidadTipo(TipoEntidadAlerta.PENDIENTE_ADMINISTRATIVO);
        resuelta.setEntidadRefId(1L);
        resuelta.setEstado(EstadoAlerta.RESUELTA);
        resuelta.setResueltaEn(java.time.Instant.now());
        when(alertaRepo.findByTipoAndEntidadTipoAndEntidadRefId(
                TipoAlerta.PENDIENTE_ADMINISTRATIVO_PROXIMO, TipoEntidadAlerta.PENDIENTE_ADMINISTRATIVO, 1L))
                .thenReturn(Optional.of(resuelta));
        when(pendienteAdministrativoService.proximosAVencer(7)).thenReturn(List.of(pendiente(1L, LocalDate.now().plusDays(2))));

        motor.sincronizar();

        assertThat(resuelta.getEstado()).isEqualTo(EstadoAlerta.ACTIVA);
        assertThat(resuelta.getResueltaEn()).isNull();
        verify(alertaRepo, never()).save(any(Alerta.class));
        verify(alertChannel, never()).notificar(any());
    }

    @Test
    void sincronizarNoDuplicaAlertaActivaExistenteYActualizaMensaje() {
        Alerta activa = new Alerta();
        activa.setTipo(TipoAlerta.PENDIENTE_ADMINISTRATIVO_PROXIMO);
        activa.setEntidadTipo(TipoEntidadAlerta.PENDIENTE_ADMINISTRATIVO);
        activa.setEntidadRefId(1L);
        activa.setEstado(EstadoAlerta.ACTIVA);
        activa.setMensaje("mensaje viejo");
        when(alertaRepo.findByTipoAndEntidadTipoAndEntidadRefId(
                TipoAlerta.PENDIENTE_ADMINISTRATIVO_PROXIMO, TipoEntidadAlerta.PENDIENTE_ADMINISTRATIVO, 1L))
                .thenReturn(Optional.of(activa));
        LocalDate nuevaFecha = LocalDate.now().plusDays(5);
        when(pendienteAdministrativoService.proximosAVencer(7)).thenReturn(List.of(pendiente(1L, nuevaFecha)));

        motor.sincronizar();

        assertThat(activa.getEstado()).isEqualTo(EstadoAlerta.ACTIVA);
        assertThat(activa.getFecha()).isEqualTo(nuevaFecha);
        assertThat(activa.getMensaje()).doesNotContain("mensaje viejo");
        verify(alertaRepo, never()).save(any(Alerta.class));
        verify(alertChannel, never()).notificar(any());
    }

    @Test
    void sincronizarAutoResuelveAlertaActivaCuandoLaCondicionDesaparece() {
        Alerta activa = new Alerta();
        activa.setTipo(TipoAlerta.PENDIENTE_ADMINISTRATIVO_PROXIMO);
        activa.setEntidadTipo(TipoEntidadAlerta.PENDIENTE_ADMINISTRATIVO);
        activa.setEntidadRefId(1L);
        activa.setEstado(EstadoAlerta.ACTIVA);
        when(alertaRepo.findByTipoAndEstado(TipoAlerta.PENDIENTE_ADMINISTRATIVO_PROXIMO, EstadoAlerta.ACTIVA))
                .thenReturn(List.of(activa));
        // proximosAVencer(7) sigue devolviendo List.of() del setUp: la condición ya no está vigente.

        motor.sincronizar();

        assertThat(activa.getEstado()).isEqualTo(EstadoAlerta.RESUELTA);
        assertThat(activa.getResueltaEn()).isNotNull();
    }

    @Test
    void sincronizarUsaDiasAnticipacionDeLaConfiguracion() {
        when(configuracionRepo.findFirstByOrderByIdAsc()).thenReturn(Optional.of(nuevaConfiguracion(3, 0)));

        motor.sincronizar();

        verify(vencimientoService).proximos(3);
        verify(pendienteAdministrativoService).proximosAVencer(3);
        verify(compromisoService).porRangoDeFechas(LocalDate.now(), LocalDate.now().plusDays(3));
    }

    @Test
    void cxcAtrasadaRespetaElUmbralDeDiasAtrasoDeLaConfiguracion() {
        CuentaPorCobrarFilaResponse fila = new CuentaPorCobrarFilaResponse(
                50L, 1L, "Cliente X", null, null, "FV-1", LocalDate.now().minusDays(20),
                LocalDate.now().minusDays(5), 1L, "ARS", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, "VENCIDO");
        when(cuentaPorCobrarService.calcular(any(), any(), any(), any(), any(), eq(EstadoVencimiento.VENCIDO)))
                .thenReturn(new CuentaPorCobrarResponse(List.of(fila), List.of()));

        // diasAtrasoCxc=10: la factura vencida hace 5 días todavía no llega al umbral -> sin alerta.
        when(configuracionRepo.findFirstByOrderByIdAsc()).thenReturn(Optional.of(nuevaConfiguracion(7, 10)));
        motor.sincronizar();
        verify(alertaRepo, never()).save(any(Alerta.class));

        // diasAtrasoCxc=3: ya superó el umbral -> genera alerta.
        when(configuracionRepo.findFirstByOrderByIdAsc()).thenReturn(Optional.of(nuevaConfiguracion(7, 3)));
        motor.sincronizar();
        var captor = org.mockito.ArgumentCaptor.forClass(Alerta.class);
        verify(alertaRepo).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoAlerta.CXC_ATRASADA);
        assertThat(captor.getValue().getEntidadRefId()).isEqualTo(50L);
    }
}

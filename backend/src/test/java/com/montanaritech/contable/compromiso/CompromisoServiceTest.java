package com.montanaritech.contable.compromiso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.ConflictoException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.compromiso.dto.CompromisoCrearRequest;
import com.montanaritech.contable.compromiso.dto.CompromisoEditarRequest;
import com.montanaritech.contable.compromiso.dto.CompromisoResponse;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.vencimientos.OrigenGeneracionVencimiento;
import com.montanaritech.contable.vencimientos.TipoVencimiento;
import com.montanaritech.contable.vencimientos.Vencimiento;
import com.montanaritech.contable.vencimientos.VencimientoService;
import com.montanaritech.contable.vencimientos.dto.VencimientoCrearRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompromisoServiceTest {

    @Mock
    private CompromisoRepository repo;
    @Mock
    private MonedaRepository monedaRepository;
    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private CompromisoMapper mapper;
    @Mock
    private AuditoriaService auditoria;
    @Mock
    private VencimientoService vencimientoService;

    private CompromisoService service;
    private Moneda ars;

    @BeforeEach
    void setUp() {
        service = new CompromisoService(repo, monedaRepository, proveedorRepository, proyectoRepository, mapper,
                auditoria, vencimientoService);
        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");
    }

    private Compromiso nuevoCompromiso() {
        Compromiso c = new Compromiso();
        c.setId(1L);
        c.setConcepto("Cuota AFIP");
        c.setTipo(TipoCompromiso.CUOTA_PLAN_DE_PAGOS);
        c.setFechaPrevista(LocalDate.of(2026, 8, 10));
        c.setImporte(new BigDecimal("3000"));
        c.setMoneda(ars);
        c.setEstado(EstadoCompromiso.PENDIENTE);
        return c;
    }

    @Test
    void crearSinGenerarVencimientoNoLlamaAVencimientoService() {
        when(monedaRepository.findById(1L)).thenReturn(Optional.of(ars));
        when(repo.save(any(Compromiso.class))).thenAnswer(inv -> {
            Compromiso c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        Compromiso creado = service.crear(new CompromisoCrearRequest("Cuota AFIP", TipoCompromiso.CUOTA_PLAN_DE_PAGOS,
                LocalDate.of(2026, 8, 10), new BigDecimal("3000"), 1L, null, null, null, false));

        assertThat(creado.getVencimientoGeneradoId()).isNull();
        verify(vencimientoService, never()).crearDesdeOrigen(any(), any(), any());
    }

    @Test
    void crearConGenerarVencimientoLoVinculaConOrigenCompromiso() {
        when(monedaRepository.findById(1L)).thenReturn(Optional.of(ars));
        when(repo.save(any(Compromiso.class))).thenAnswer(inv -> {
            Compromiso c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });
        Vencimiento generado = new Vencimiento();
        generado.setId(99L);
        when(vencimientoService.crearDesdeOrigen(any(VencimientoCrearRequest.class),
                eq(OrigenGeneracionVencimiento.COMPROMISO), eq(10L))).thenReturn(generado);

        Compromiso creado = service.crear(new CompromisoCrearRequest("Cuota AFIP", TipoCompromiso.CUOTA_PLAN_DE_PAGOS,
                LocalDate.of(2026, 8, 10), new BigDecimal("3000"), 1L, null, null, null, true));

        assertThat(creado.getVencimientoGeneradoId()).isEqualTo(99L);
        ArgumentCaptor<VencimientoCrearRequest> captor = ArgumentCaptor.forClass(VencimientoCrearRequest.class);
        verify(vencimientoService).crearDesdeOrigen(captor.capture(), eq(OrigenGeneracionVencimiento.COMPROMISO), eq(10L));
        assertThat(captor.getValue().tipo()).isEqualTo(TipoVencimiento.PLAN_DE_PAGO);
        assertThat(captor.getValue().fecha()).isEqualTo(LocalDate.of(2026, 8, 10));
    }

    @Test
    void crearConMonedaInexistenteLanzaNoEncontrado() {
        when(monedaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(new CompromisoCrearRequest("X", TipoCompromiso.OTRO_EGRESO,
                LocalDate.now(), BigDecimal.TEN, 99L, null, null, null, false)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void obtenerConIdInexistenteLanzaNoEncontrado() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void editarActualizaYAudita() {
        Compromiso existente = nuevoCompromiso();
        when(repo.findById(1L)).thenReturn(Optional.of(existente));
        when(monedaRepository.findById(1L)).thenReturn(Optional.of(ars));
        when(mapper.aResponse(any(Compromiso.class))).thenReturn(
                new CompromisoResponse(1L, "x", TipoCompromiso.OTRO_EGRESO, LocalDate.now(), BigDecimal.ONE, 1L,
                        "ARS", null, null, null, null, EstadoCompromiso.PENDIENTE, null, null, true));

        service.editar(1L, new CompromisoEditarRequest("Cuota AFIP editada", TipoCompromiso.CUOTA_PLAN_DE_PAGOS,
                LocalDate.of(2026, 9, 10), new BigDecimal("3500"), 1L, null, null, EstadoCompromiso.RESUELTO, "ok"));

        assertThat(existente.getConcepto()).isEqualTo("Cuota AFIP editada");
        assertThat(existente.getEstado()).isEqualTo(EstadoCompromiso.RESUELTO);
        verify(auditoria).registrar(eq(com.montanaritech.contable.common.audit.AccionAuditoria.EDITAR),
                eq("Compromiso"), eq(1L), any(), any());
    }

    @Test
    void eliminarConVencimientoGeneradoLanzaConflicto() {
        Compromiso existente = nuevoCompromiso();
        existente.setVencimientoGeneradoId(99L);
        when(repo.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.eliminar(1L)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void eliminarSinVencimientoGeneradoFunciona() {
        Compromiso existente = nuevoCompromiso();
        when(repo.findById(1L)).thenReturn(Optional.of(existente));

        service.eliminar(1L);

        org.mockito.Mockito.verify(repo).delete(existente);
    }

    @Test
    void porRangoDeFechasDelegaEnElRepositorio() {
        Compromiso c = nuevoCompromiso();
        when(repo.findByFechaPrevistaBetweenOrderByFechaPrevistaAsc(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(c));

        List<Compromiso> resultado = service.porRangoDeFechas(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertThat(resultado).containsExactly(c);
    }
}

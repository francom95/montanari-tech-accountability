package com.montanaritech.contable.pendiente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.auth.Usuario;
import com.montanaritech.contable.auth.UsuarioRepository;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.cliente.ClienteRepository;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.pendiente.dto.PendienteAdministrativoCrearRequest;
import com.montanaritech.contable.pendiente.dto.PendienteAdministrativoEditarRequest;
import com.montanaritech.contable.pendiente.dto.PendienteAdministrativoResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendienteAdministrativoServiceTest {

    @Mock private PendienteAdministrativoRepository repo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private ProyectoRepository proyectoRepo;
    @Mock private ClienteRepository clienteRepo;
    @Mock private ProveedorRepository proveedorRepo;
    @Mock private PendienteAdministrativoMapper mapper;
    @Mock private AuditoriaService auditoria;

    private PendienteAdministrativoService service;

    @BeforeEach
    void setUp() {
        service = new PendienteAdministrativoService(repo, usuarioRepo, proyectoRepo, clienteRepo, proveedorRepo,
                mapper, auditoria);
    }

    private PendienteAdministrativo nuevoPendiente() {
        PendienteAdministrativo p = new PendienteAdministrativo();
        p.setId(1L);
        p.setTitulo("Pedir factura X");
        p.setPrioridad(PrioridadPendiente.MEDIA);
        p.setEstado(EstadoPendiente.PENDIENTE);
        p.setActivo(true);
        return p;
    }

    @Test
    void crearSinFksOpcionalesFunciona() {
        when(repo.save(any(PendienteAdministrativo.class))).thenAnswer(inv -> {
            PendienteAdministrativo p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        PendienteAdministrativo creado = service.crear(new PendienteAdministrativoCrearRequest(
                "Revisar movimiento sin identificar", null, LocalDate.of(2026, 8, 15),
                PrioridadPendiente.ALTA, null, "Bancos", null, null, null, null));

        assertThat(creado.getTitulo()).isEqualTo("Revisar movimiento sin identificar");
        assertThat(creado.getResponsable()).isNull();
        assertThat(creado.getEstado()).isEqualTo(EstadoPendiente.PENDIENTE);
    }

    @Test
    void crearConResponsableInexistenteLanzaNoEncontrado() {
        when(usuarioRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(new PendienteAdministrativoCrearRequest(
                "X", null, null, PrioridadPendiente.MEDIA, 99L, null, null, null, null, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearConResponsableExistenteLoAsocia() {
        Usuario u = new Usuario();
        u.setId(5L);
        when(usuarioRepo.findById(5L)).thenReturn(Optional.of(u));
        when(repo.save(any(PendienteAdministrativo.class))).thenAnswer(inv -> inv.getArgument(0));

        PendienteAdministrativo creado = service.crear(new PendienteAdministrativoCrearRequest(
                "Revisar IVA", null, null, PrioridadPendiente.ALTA, 5L, "Impuestos", null, null, null, null));

        assertThat(creado.getResponsable()).isEqualTo(u);
    }

    @Test
    void obtenerConIdInexistenteLanzaNoEncontrado() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void editarActualizaEstadoYAudita() {
        PendienteAdministrativo existente = nuevoPendiente();
        when(repo.findById(1L)).thenReturn(Optional.of(existente));
        when(mapper.aResponse(any(PendienteAdministrativo.class))).thenReturn(
                new PendienteAdministrativoResponse(1L, "x", null, null, PrioridadPendiente.MEDIA,
                        EstadoPendiente.PENDIENTE, null, null, null, null, null, null, null, null, null, null, true));

        service.editar(1L, new PendienteAdministrativoEditarRequest("Pedir factura X - actualizado", "detalle",
                LocalDate.of(2026, 9, 1), PrioridadPendiente.BAJA, EstadoPendiente.EN_PROCESO, null, "Facturación",
                null, null, null, "seguimiento"));

        assertThat(existente.getTitulo()).isEqualTo("Pedir factura X - actualizado");
        assertThat(existente.getEstado()).isEqualTo(EstadoPendiente.EN_PROCESO);
        assertThat(existente.getPrioridad()).isEqualTo(PrioridadPendiente.BAJA);
        verify(auditoria).registrar(eq(com.montanaritech.contable.common.audit.AccionAuditoria.EDITAR),
                eq("PendienteAdministrativo"), eq(1L), any(), any());
    }

    @Test
    void eliminarFunciona() {
        PendienteAdministrativo existente = nuevoPendiente();
        when(repo.findById(1L)).thenReturn(Optional.of(existente));

        service.eliminar(1L);

        verify(repo).delete(existente);
    }

    @Test
    void proximosAVencerDelegaEnElRepositorioConEstadoPendiente() {
        PendienteAdministrativo p = nuevoPendiente();
        when(repo.findByEstadoAndFechaEstimadaResolucionLessThanEqualOrderByFechaEstimadaResolucionAsc(
                eq(EstadoPendiente.PENDIENTE), any(LocalDate.class))).thenReturn(List.of(p));

        List<PendienteAdministrativo> resultado = service.proximosAVencer(10);

        assertThat(resultado).containsExactly(p);
    }
}

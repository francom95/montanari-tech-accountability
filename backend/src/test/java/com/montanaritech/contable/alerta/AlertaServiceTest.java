package com.montanaritech.contable.alerta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.auth.Usuario;
import com.montanaritech.contable.auth.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Aislamiento de {@link AlertaLectura} por usuario (F9.1, decisión: leída es por usuario, no global). */
@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {

    @Mock private AlertaRepository repo;
    @Mock private AlertaLecturaRepository lecturaRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private MotorAlertasService motor;

    private AlertaService service;

    @BeforeEach
    void setUp() {
        service = new AlertaService(repo, lecturaRepo, usuarioRepo, motor);
    }

    private Alerta alerta(Long id) {
        Alerta a = new Alerta();
        a.setId(id);
        a.setTipo(TipoAlerta.VENCIMIENTO_PROXIMO);
        a.setSeveridad(SeveridadAlerta.ADVERTENCIA);
        a.setMensaje("mensaje");
        a.setEntidadTipo(TipoEntidadAlerta.VENCIMIENTO);
        a.setEntidadRefId(1L);
        a.setFecha(java.time.LocalDate.now());
        a.setEstado(EstadoAlerta.ACTIVA);
        return a;
    }

    private Usuario usuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    @Test
    void marcarLeidaPorUnUsuarioNoAfectaAOtroUsuario() {
        Alerta a = alerta(100L);
        when(repo.findById(100L)).thenReturn(Optional.of(a));
        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(usuario(1L)));
        when(lecturaRepo.findByAlerta_IdAndUsuario_Id(100L, 1L)).thenReturn(Optional.empty());

        service.marcarLeida(100L, 1L);

        verify(lecturaRepo).save(argThat(l -> l.getUsuario().getId().equals(1L) && l.getAlerta().getId().equals(100L)));
        // El usuario 2 nunca marcó nada: contarActivasNoLeidas para él debe seguir contando esta alerta.
        when(repo.contarActivasNoLeidasPorUsuario(2L)).thenReturn(1L);
        when(repo.contarActivasNoLeidasPorUsuario(1L)).thenReturn(0L);

        assertThat(service.contarActivasNoLeidas(2L)).isEqualTo(1L);
        assertThat(service.contarActivasNoLeidas(1L)).isEqualTo(0L);
    }

    @Test
    void marcarLeidaEsIdempotenteNoDuplicaSiYaEstabaLeidaPorEseUsuario() {
        AlertaLectura existente = new AlertaLectura();
        when(lecturaRepo.findByAlerta_IdAndUsuario_Id(100L, 1L)).thenReturn(Optional.of(existente));

        service.marcarLeida(100L, 1L);

        verify(lecturaRepo, never()).save(any(AlertaLectura.class));
    }

    @Test
    void listarResuelveLeidaSoloParaLasAlertasDelUsuarioActual() {
        Alerta a1 = alerta(100L);
        Alerta a2 = alerta(200L);
        var pagina = new org.springframework.data.domain.PageImpl<>(List.of(a1, a2));
        when(repo.buscar(any(), any(), any())).thenReturn(pagina);
        when(lecturaRepo.findAlertaIdsLeidasPorUsuario(eq(List.of(100L, 200L)), eq(1L))).thenReturn(List.of(100L));

        var resultado = service.listar(null, null, 1L, org.springframework.data.domain.Pageable.unpaged());

        assertThat(resultado.getContent()).extracting("id", "leida")
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(100L, true),
                        org.assertj.core.groups.Tuple.tuple(200L, false));
        verify(lecturaRepo, times(1)).findAlertaIdsLeidasPorUsuario(any(), eq(1L));
    }
}

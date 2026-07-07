package com.montanaritech.contable.maestros.comisionista;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.ConflictoException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.comisionista.dto.ComisionistaCrearRequest;
import com.montanaritech.contable.maestros.comisionista.dto.ComisionistaEditarRequest;
import com.montanaritech.contable.maestros.comisionista.dto.ComisionistaResponse;
import com.montanaritech.contable.maestros.proyecto.comision.ComisionProyectoRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ComisionistaServiceTest {

    @Mock
    private ComisionistaRepository repo;

    @Mock
    private ComisionistaMapper mapper;

    @Mock
    private AuditoriaService auditoria;

    @Mock
    private ComisionProyectoRepository comisionProyectoRepo;

    @InjectMocks
    private ComisionistaService service;

    private Comisionista entidad;

    @BeforeEach
    void setUp() {
        entidad = new Comisionista();
        entidad.setId(1L);
        entidad.setNombre("Cristian Pittaluga");
        entidad.setActivo(true);
    }

    @Test
    void crearGuardaLaEntidadActiva() {
        when(repo.save(any(Comisionista.class))).thenAnswer(inv -> inv.getArgument(0));

        Comisionista creado = service.crear(new ComisionistaCrearRequest("Javier Montanari", null, null, null, null));

        assertThat(creado.getNombre()).isEqualTo("Javier Montanari");
        assertThat(creado.isActivo()).isTrue();
        assertThat(creado.getCuit()).isNull();
    }

    @Test
    void crearNormalizaCuitVacioANull() {
        when(repo.save(any(Comisionista.class))).thenAnswer(inv -> inv.getArgument(0));

        Comisionista creado = service.crear(new ComisionistaCrearRequest("Test", "", null, null, null));

        assertThat(creado.getCuit()).isNull();
    }

    @Test
    void obtenerConIdInexistenteLanzaNoEncontrado() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void editarActualizaNombre() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(Comisionista.class))).thenAnswer(inv -> {
            Comisionista c = inv.getArgument(0);
            return new ComisionistaResponse(c.getId(), c.getNombre(), c.getCuit(), c.getContacto(), c.getEmail(), c.getTelefono(), c.isActivo());
        });

        service.editar(1L, new ComisionistaEditarRequest("Nombre Editado", null, null, null, null));

        assertThat(entidad.getNombre()).isEqualTo("Nombre Editado");
        verify(auditoria).registrar(
                eq(AccionAuditoria.EDITAR), eq("Comisionista"), eq(1L), any(ComisionistaResponse.class), any(ComisionistaResponse.class));
    }

    @Test
    void desactivarCambiaEstadoAFalse() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(Comisionista.class))).thenAnswer(inv -> {
            Comisionista c = inv.getArgument(0);
            return new ComisionistaResponse(c.getId(), c.getNombre(), c.getCuit(), c.getContacto(), c.getEmail(), c.getTelefono(), c.isActivo());
        });

        service.desactivar(1L);

        assertThat(entidad.isActivo()).isFalse();
    }

    @Test
    void eliminarConComisionesAsociadasLanzaConflicto() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(comisionProyectoRepo.existsByComisionistaId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(1L)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void eliminarSinComisionesBorraLaEntidad() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(comisionProyectoRepo.existsByComisionistaId(1L)).thenReturn(false);
        when(mapper.aResponse(any(Comisionista.class))).thenAnswer(inv -> {
            Comisionista c = inv.getArgument(0);
            return new ComisionistaResponse(c.getId(), c.getNombre(), c.getCuit(), c.getContacto(), c.getEmail(), c.getTelefono(), c.isActivo());
        });

        service.eliminar(1L);

        verify(repo).delete(entidad);
        verify(auditoria).registrar(
                eq(AccionAuditoria.ELIMINAR), eq("Comisionista"), eq(1L), any(ComisionistaResponse.class), eq(null));
    }
}

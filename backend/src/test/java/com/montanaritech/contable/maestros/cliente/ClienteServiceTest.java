package com.montanaritech.contable.maestros.cliente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.cliente.dto.ClienteCrearRequest;
import com.montanaritech.contable.maestros.cliente.dto.ClienteEditarRequest;
import com.montanaritech.contable.maestros.cliente.dto.ClienteResponse;
import com.montanaritech.contable.maestros.jurisdiccion.Jurisdiccion;
import com.montanaritech.contable.maestros.jurisdiccion.JurisdiccionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository repo;

    @Mock
    private ClienteMapper mapper;

    @Mock
    private AuditoriaService auditoria;

    @Mock
    private JurisdiccionRepository jurisdiccionRepo;

    @InjectMocks
    private ClienteService service;

    private Cliente entidad;
    private Jurisdiccion jurisdiccion;

    @BeforeEach
    void setUp() {
        jurisdiccion = new Jurisdiccion();
        jurisdiccion.setId(1L);
        jurisdiccion.setNombre("Buenos Aires");

        entidad = new Cliente();
        entidad.setId(1L);
        entidad.setNombre("Cliente Test");
        entidad.setCuit("20-12345678-9");
        entidad.setJurisdiccion(jurisdiccion);
        entidad.setContacto("Juan Pérez");
        entidad.setEmail("juan@test.com");
        entidad.setTelefono("1123456789");
        entidad.setActivo(true);
    }

    @Test
    void crearGuardaLaEntidadActiva() {
        when(jurisdiccionRepo.findById(1L)).thenReturn(Optional.of(jurisdiccion));
        when(repo.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente creada = service.crear(new ClienteCrearRequest("Nuevo Cliente", "30-12345678-9", 1L, "Contacto", "email@test.com", "987654321", null));

        assertThat(creada.getNombre()).isEqualTo("Nuevo Cliente");
        assertThat(creada.getCuit()).isEqualTo("30-12345678-9");
        assertThat(creada.isActivo()).isTrue();
    }

    @Test
    void crearConJurisdictionNoExistenteThrow() {
        when(jurisdiccionRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(new ClienteCrearRequest("Test", "20-12345678-9", 99L, null, null, null, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void obtenerConIdInexistenteLanzaNoEncontrado() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void obtenerConIdExistenteRetornaEntidad() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));

        Cliente obtenida = service.obtener(1L);

        assertThat(obtenida.getNombre()).isEqualTo("Cliente Test");
    }

    @Test
    void editarActualizaNombreYJurisdiccion() {
        var jurisdiccion2 = new Jurisdiccion();
        jurisdiccion2.setId(2L);
        jurisdiccion2.setNombre("Córdoba");

        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(jurisdiccionRepo.findById(2L)).thenReturn(Optional.of(jurisdiccion2));
        when(mapper.aResponse(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            return new ClienteResponse(c.getId(), c.getNombre(), c.getCuit(), c.getJurisdiccion().getId(),
                    c.getJurisdiccion().getNombre(), c.getContacto(), c.getEmail(), c.getTelefono(), null, null, c.isActivo());
        });

        service.editar(1L, new ClienteEditarRequest("Cliente Editado", 2L, "Contacto New", "new@test.com", "111111111", null));

        assertThat(entidad.getNombre()).isEqualTo("Cliente Editado");
        assertThat(entidad.getJurisdiccion().getId()).isEqualTo(2L);
        verify(auditoria).registrar(
                eq(AccionAuditoria.EDITAR), eq("Cliente"), eq(1L), any(ClienteResponse.class), any(ClienteResponse.class));
    }

    @Test
    void editarConJurisdictionNoExistenteThrow() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(jurisdiccionRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editar(1L, new ClienteEditarRequest("Test", 99L, null, null, null, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void desactivarCambiaEstadoAFalse() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            return new ClienteResponse(c.getId(), c.getNombre(), c.getCuit(), c.getJurisdiccion().getId(),
                    c.getJurisdiccion().getNombre(), c.getContacto(), c.getEmail(), c.getTelefono(), null, null, c.isActivo());
        });

        service.desactivar(1L);

        assertThat(entidad.isActivo()).isFalse();
        verify(auditoria).registrar(
                eq(AccionAuditoria.CAMBIO_ESTADO), eq("Cliente"), eq(1L), any(ClienteResponse.class), any(ClienteResponse.class));
    }

    @Test
    void activarCambiaEstadoATrue() {
        entidad.setActivo(false);

        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            return new ClienteResponse(c.getId(), c.getNombre(), c.getCuit(), c.getJurisdiccion().getId(),
                    c.getJurisdiccion().getNombre(), c.getContacto(), c.getEmail(), c.getTelefono(), null, null, c.isActivo());
        });

        service.activar(1L);

        assertThat(entidad.isActivo()).isTrue();
        verify(auditoria).registrar(
                eq(AccionAuditoria.CAMBIO_ESTADO), eq("Cliente"), eq(1L), any(ClienteResponse.class), any(ClienteResponse.class));
    }

    @Test
    void eliminarBorraLaEntidad() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            return new ClienteResponse(c.getId(), c.getNombre(), c.getCuit(), c.getJurisdiccion().getId(),
                    c.getJurisdiccion().getNombre(), c.getContacto(), c.getEmail(), c.getTelefono(), null, null, c.isActivo());
        });

        service.eliminar(1L);

        verify(repo).delete(entidad);
        verify(auditoria).registrar(
                eq(AccionAuditoria.ELIMINAR), eq("Cliente"), eq(1L), any(ClienteResponse.class), eq(null));
    }

    @Test
    void listarSinFiltrosRetornaTodos() {
        when(repo.buscar(isNull(), isNull(), any())).thenReturn(null); // Mock would be set up with actual pages

        service.listar(null, null, null);

        verify(repo).buscar(isNull(), isNull(), isNull());
    }

    @Test
    void listarConTextoFiltra() {
        when(repo.buscar(eq("Cliente"), isNull(), any())).thenReturn(null);

        service.listar("Cliente", null, null);

        verify(repo).buscar(eq("Cliente"), isNull(), isNull());
    }

    @Test
    void listarConActivoFiltra() {
        when(repo.buscar(isNull(), eq(true), any())).thenReturn(null);

        service.listar(null, true, null);

        verify(repo).buscar(isNull(), eq(true), isNull());
    }

    @Test
    void cuitDebeEstarPresenteEnCrear() {
        when(jurisdiccionRepo.findById(1L)).thenReturn(Optional.of(jurisdiccion));
        when(repo.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente creada = service.crear(new ClienteCrearRequest("Test", "20-12345678-9", 1L, null, null, null, null));

        assertThat(creada.getCuit()).isNotBlank();
    }

    @Test
    void contactoOptionalEnCrear() {
        when(jurisdiccionRepo.findById(1L)).thenReturn(Optional.of(jurisdiccion));
        when(repo.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente creada = service.crear(new ClienteCrearRequest("Test", "20-12345678-9", 1L, null, null, null, null));

        assertThat(creada.getContacto()).isNull();
    }

    @Test
    void emailOptionalEnCrear() {
        when(jurisdiccionRepo.findById(1L)).thenReturn(Optional.of(jurisdiccion));
        when(repo.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente creada = service.crear(new ClienteCrearRequest("Test", "20-12345678-9", 1L, null, null, null, null));

        assertThat(creada.getEmail()).isNull();
    }

    @Test
    void telefonoOptionalEnCrear() {
        when(jurisdiccionRepo.findById(1L)).thenReturn(Optional.of(jurisdiccion));
        when(repo.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente creada = service.crear(new ClienteCrearRequest("Test", "20-12345678-9", 1L, null, null, null, null));

        assertThat(creada.getTelefono()).isNull();
    }
}

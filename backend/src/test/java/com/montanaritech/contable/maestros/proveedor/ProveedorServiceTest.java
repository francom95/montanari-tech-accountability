package com.montanaritech.contable.maestros.proveedor;

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
import com.montanaritech.contable.maestros.jurisdiccion.Jurisdiccion;
import com.montanaritech.contable.maestros.jurisdiccion.JurisdiccionRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proveedor.dto.ProveedorCrearRequest;
import com.montanaritech.contable.maestros.proveedor.dto.ProveedorEditarRequest;
import com.montanaritech.contable.maestros.proveedor.dto.ProveedorResponse;
import com.montanaritech.contable.maestros.tipocosto.TipoCosto;
import com.montanaritech.contable.maestros.tipocosto.TipoCostoRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {

    @Mock
    private ProveedorRepository repo;

    @Mock
    private ProveedorMapper mapper;

    @Mock
    private AuditoriaService auditoria;

    @Mock
    private JurisdiccionRepository jurisdiccionRepo;

    @Mock
    private MonedaRepository monedaRepo;

    @Mock
    private TipoCostoRepository tipoCostoRepo;

    @InjectMocks
    private ProveedorService service;

    private Proveedor entidad;
    private Jurisdiccion jurisdiccion;
    private Moneda moneda;
    private TipoCosto tipoCosto;

    @BeforeEach
    void setUp() {
        jurisdiccion = new Jurisdiccion();
        jurisdiccion.setId(1L);
        jurisdiccion.setNombre("Buenos Aires");

        moneda = new Moneda();
        moneda.setId(1L);
        moneda.setCodigo("ARS");
        moneda.setNombre("Peso argentino");

        tipoCosto = new TipoCosto();
        tipoCosto.setId(1L);
        tipoCosto.setNombre("Tipo Costo A");

        entidad = new Proveedor();
        entidad.setId(1L);
        entidad.setNombre("Proveedor Test");
        entidad.setCuit("20-12345678-9");
        entidad.setJurisdiccion(jurisdiccion);
        entidad.setMonedaHabitual(moneda);
        entidad.setTiposCosto(new HashSet<>(List.of(tipoCosto)));
        entidad.setContacto("Juan Pérez");
        entidad.setEmail("juan@test.com");
        entidad.setTelefono("1123456789");
        entidad.setActivo(true);
    }

    @Test
    void crearGuardaLaEntidadActiva() {
        when(jurisdiccionRepo.findById(1L)).thenReturn(Optional.of(jurisdiccion));
        when(monedaRepo.findById(1L)).thenReturn(Optional.of(moneda));
        when(tipoCostoRepo.findAllById(any())).thenReturn(List.of(tipoCosto));
        when(repo.save(any(Proveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        Proveedor creada = service.crear(new ProveedorCrearRequest("Nuevo Proveedor", "30-12345678-9", 1L, 1L, Set.of(1L), "Contacto", "email@test.com", "987654321", null, null));

        assertThat(creada.getNombre()).isEqualTo("Nuevo Proveedor");
        assertThat(creada.getCuit()).isEqualTo("30-12345678-9");
        assertThat(creada.isActivo()).isTrue();
    }

    @Test
    void crearConJurisdictionNoExistenteThrow() {
        when(jurisdiccionRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(new ProveedorCrearRequest("Test", "20-12345678-9", 99L, 1L, null, null, null, null, null, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearConMonedaNoExistenteThrow() {
        when(jurisdiccionRepo.findById(1L)).thenReturn(Optional.of(jurisdiccion));
        when(monedaRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(new ProveedorCrearRequest("Test", "20-12345678-9", 1L, 99L, null, null, null, null, null, null)))
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

        Proveedor obtenida = service.obtener(1L);

        assertThat(obtenida.getNombre()).isEqualTo("Proveedor Test");
    }

    @Test
    void editarActualizaNombreYJurisdiccion() {
        var jurisdiccion2 = new Jurisdiccion();
        jurisdiccion2.setId(2L);
        jurisdiccion2.setNombre("Córdoba");

        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(jurisdiccionRepo.findById(2L)).thenReturn(Optional.of(jurisdiccion2));
        when(monedaRepo.findById(1L)).thenReturn(Optional.of(moneda));
        when(tipoCostoRepo.findAllById(any())).thenReturn(List.of(tipoCosto));
        when(mapper.aResponse(any(Proveedor.class))).thenAnswer(inv -> {
            Proveedor p = inv.getArgument(0);
            return new ProveedorResponse(p.getId(), p.getNombre(), p.getCuit(), p.getJurisdiccion().getId(),
                    p.getJurisdiccion().getNombre(), p.getMonedaHabitual().getId(), p.getMonedaHabitual().getCodigo(),
                    new HashSet<>(), p.getContacto(), p.getEmail(), p.getTelefono(), p.getCondicionIva(), null, null, p.isActivo());
        });

        service.editar(1L, new ProveedorEditarRequest("Proveedor Editado", 2L, 1L, Set.of(1L), "Contacto New", "new@test.com", "111111111", null, null));

        assertThat(entidad.getNombre()).isEqualTo("Proveedor Editado");
        assertThat(entidad.getJurisdiccion().getId()).isEqualTo(2L);
        verify(auditoria).registrar(
                eq(AccionAuditoria.EDITAR), eq("Proveedor"), eq(1L), any(ProveedorResponse.class), any(ProveedorResponse.class));
    }

    @Test
    void editarConJurisdictionNoExistenteThrow() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(jurisdiccionRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editar(1L, new ProveedorEditarRequest("Test", 99L, 1L, null, null, null, null, null, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void editarConMonedaNoExistenteThrow() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(jurisdiccionRepo.findById(1L)).thenReturn(Optional.of(jurisdiccion));
        when(monedaRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editar(1L, new ProveedorEditarRequest("Test", 1L, 99L, null, null, null, null, null, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void desactivarCambiaEstadoAFalse() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(Proveedor.class))).thenAnswer(inv -> {
            Proveedor p = inv.getArgument(0);
            return new ProveedorResponse(p.getId(), p.getNombre(), p.getCuit(), p.getJurisdiccion().getId(),
                    p.getJurisdiccion().getNombre(), p.getMonedaHabitual().getId(), p.getMonedaHabitual().getCodigo(),
                    new HashSet<>(), p.getContacto(), p.getEmail(), p.getTelefono(), p.getCondicionIva(), null, null, p.isActivo());
        });

        service.desactivar(1L);

        assertThat(entidad.isActivo()).isFalse();
        verify(auditoria).registrar(
                eq(AccionAuditoria.CAMBIO_ESTADO), eq("Proveedor"), eq(1L), any(ProveedorResponse.class), any(ProveedorResponse.class));
    }

    @Test
    void activarCambiaEstadoATrue() {
        entidad.setActivo(false);

        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(Proveedor.class))).thenAnswer(inv -> {
            Proveedor p = inv.getArgument(0);
            return new ProveedorResponse(p.getId(), p.getNombre(), p.getCuit(), p.getJurisdiccion().getId(),
                    p.getJurisdiccion().getNombre(), p.getMonedaHabitual().getId(), p.getMonedaHabitual().getCodigo(),
                    new HashSet<>(), p.getContacto(), p.getEmail(), p.getTelefono(), p.getCondicionIva(), null, null, p.isActivo());
        });

        service.activar(1L);

        assertThat(entidad.isActivo()).isTrue();
        verify(auditoria).registrar(
                eq(AccionAuditoria.CAMBIO_ESTADO), eq("Proveedor"), eq(1L), any(ProveedorResponse.class), any(ProveedorResponse.class));
    }

    @Test
    void eliminarBorraLaEntidad() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(Proveedor.class))).thenAnswer(inv -> {
            Proveedor p = inv.getArgument(0);
            return new ProveedorResponse(p.getId(), p.getNombre(), p.getCuit(), p.getJurisdiccion().getId(),
                    p.getJurisdiccion().getNombre(), p.getMonedaHabitual().getId(), p.getMonedaHabitual().getCodigo(),
                    new HashSet<>(), p.getContacto(), p.getEmail(), p.getTelefono(), p.getCondicionIva(), null, null, p.isActivo());
        });

        service.eliminar(1L);

        verify(repo).delete(entidad);
        verify(auditoria).registrar(
                eq(AccionAuditoria.ELIMINAR), eq("Proveedor"), eq(1L), any(ProveedorResponse.class), eq(null));
    }

    @Test
    void listarSinFiltrosRetornaTodos() {
        when(repo.buscar(isNull(), isNull(), any())).thenReturn(null);

        service.listar(null, null, null);

        verify(repo).buscar(isNull(), isNull(), isNull());
    }

    @Test
    void listarConTextoFiltra() {
        when(repo.buscar(eq("Proveedor"), isNull(), any())).thenReturn(null);

        service.listar("Proveedor", null, null);

        verify(repo).buscar(eq("Proveedor"), isNull(), isNull());
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
        when(monedaRepo.findById(1L)).thenReturn(Optional.of(moneda));
        when(tipoCostoRepo.findAllById(any())).thenReturn(List.of(tipoCosto));
        when(repo.save(any(Proveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        Proveedor creada = service.crear(new ProveedorCrearRequest("Test", "20-12345678-9", 1L, 1L, Set.of(1L), null, null, null, null, null));

        assertThat(creada.getCuit()).isNotBlank();
    }

    @Test
    void contactoOptionalEnCrear() {
        when(jurisdiccionRepo.findById(1L)).thenReturn(Optional.of(jurisdiccion));
        when(monedaRepo.findById(1L)).thenReturn(Optional.of(moneda));
        when(tipoCostoRepo.findAllById(any())).thenReturn(List.of(tipoCosto));
        when(repo.save(any(Proveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        Proveedor creada = service.crear(new ProveedorCrearRequest("Test", "20-12345678-9", 1L, 1L, Set.of(1L), null, null, null, null, null));

        assertThat(creada.getContacto()).isNull();
    }

    @Test
    void emailOptionalEnCrear() {
        when(jurisdiccionRepo.findById(1L)).thenReturn(Optional.of(jurisdiccion));
        when(monedaRepo.findById(1L)).thenReturn(Optional.of(moneda));
        when(tipoCostoRepo.findAllById(any())).thenReturn(List.of(tipoCosto));
        when(repo.save(any(Proveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        Proveedor creada = service.crear(new ProveedorCrearRequest("Test", "20-12345678-9", 1L, 1L, Set.of(1L), null, null, null, null, null));

        assertThat(creada.getEmail()).isNull();
    }

    @Test
    void telefonoOptionalEnCrear() {
        when(jurisdiccionRepo.findById(1L)).thenReturn(Optional.of(jurisdiccion));
        when(monedaRepo.findById(1L)).thenReturn(Optional.of(moneda));
        when(tipoCostoRepo.findAllById(any())).thenReturn(List.of(tipoCosto));
        when(repo.save(any(Proveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        Proveedor creada = service.crear(new ProveedorCrearRequest("Test", "20-12345678-9", 1L, 1L, Set.of(1L), null, null, null, null, null));

        assertThat(creada.getTelefono()).isNull();
    }

    @Test
    void crearSinMonedaHabitualEsValido() {
        when(jurisdiccionRepo.findById(1L)).thenReturn(Optional.of(jurisdiccion));
        when(tipoCostoRepo.findAllById(any())).thenReturn(List.of(tipoCosto));
        when(repo.save(any(Proveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        Proveedor creada = service.crear(new ProveedorCrearRequest("Test", "20-12345678-9", 1L, null, Set.of(1L), null, null, null, null, null));

        assertThat(creada.getMonedaHabitual()).isNull();
    }

    @Test
    void crearSinTiposCostoEsValido() {
        when(jurisdiccionRepo.findById(1L)).thenReturn(Optional.of(jurisdiccion));
        when(monedaRepo.findById(1L)).thenReturn(Optional.of(moneda));
        when(repo.save(any(Proveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        Proveedor creada = service.crear(new ProveedorCrearRequest("Test", "20-12345678-9", 1L, 1L, null, null, null, null, null, null));

        assertThat(creada.getTiposCosto()).isEmpty();
    }
}

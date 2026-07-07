package com.montanaritech.contable.maestros.proyecto.etapa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.etapa.dto.EtapaCrearRequest;
import com.montanaritech.contable.maestros.proyecto.etapa.dto.EtapaEditarRequest;
import com.montanaritech.contable.maestros.proyecto.etapa.dto.EtapaResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class EtapaServiceTest {

    @Mock
    private EtapaRepository repo;

    @Mock
    private EtapaMapper mapper;

    @Mock
    private AuditoriaService auditoria;

    @Mock
    private ProyectoRepository proyectoRepo;

    @Mock
    private ProveedorRepository proveedorRepo;

    @InjectMocks
    private EtapaService service;

    private Proyecto proyecto;
    private Etapa entidad;
    private Proveedor proveedor;

    @BeforeEach
    void setUp() {
        proyecto = new Proyecto();
        proyecto.setId(1L);
        proyecto.setNombre("Proyecto Test");

        proveedor = new Proveedor();
        proveedor.setId(1L);
        proveedor.setNombre("Proveedor Test");

        entidad = new Etapa();
        entidad.setId(1L);
        entidad.setProyecto(proyecto);
        entidad.setNombre("Etapa Test");
        entidad.setActivo(true);
    }

    @Test
    void crearGuardaLaEtapaActivaConProveedores() {
        when(proyectoRepo.findById(1L)).thenReturn(Optional.of(proyecto));
        when(proveedorRepo.findAllById(Set.of(1L))).thenReturn(List.of(proveedor));
        when(repo.save(any(Etapa.class))).thenAnswer(inv -> inv.getArgument(0));

        EtapaCrearRequest req = new EtapaCrearRequest(
                "Nueva etapa", "desc", null, LocalDate.now(), LocalDate.now().plusDays(30), 0,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(500), Set.of(1L), BigDecimal.ZERO, BigDecimal.ZERO, "obs");

        Etapa creada = service.crear(1L, req);

        assertThat(creada.getNombre()).isEqualTo("Nueva etapa");
        assertThat(creada.isActivo()).isTrue();
        assertThat(creada.getEstado()).isEqualTo(Etapa.EstadoEtapa.PENDIENTE);
        assertThat(creada.getProveedores()).containsExactly(proveedor);
    }

    @Test
    void crearSinProveedoresEsValido() {
        when(proyectoRepo.findById(1L)).thenReturn(Optional.of(proyecto));
        when(repo.save(any(Etapa.class))).thenAnswer(inv -> inv.getArgument(0));

        EtapaCrearRequest req = new EtapaCrearRequest(
                "Nueva etapa", null, "EN_CURSO", null, null, null, null, null, null, null, null, null);

        Etapa creada = service.crear(1L, req);

        assertThat(creada.getProveedores()).isEmpty();
        assertThat(creada.getEstado()).isEqualTo(Etapa.EstadoEtapa.EN_CURSO);
    }

    @Test
    void crearConProyectoInexistenteThrow() {
        when(proyectoRepo.findById(99L)).thenReturn(Optional.empty());

        EtapaCrearRequest req = new EtapaCrearRequest(
                "Test", null, null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.crear(99L, req)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void obtenerConIdDeOtroProyectoLanzaNoEncontrado() {
        when(repo.findByIdAndProyectoId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(2L, 1L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void editarActualizaCamposYProveedores() {
        when(repo.findByIdAndProyectoId(1L, 1L)).thenReturn(Optional.of(entidad));
        when(proveedorRepo.findAllById(Set.of(1L))).thenReturn(List.of(proveedor));
        when(mapper.aResponse(any(Etapa.class))).thenReturn(mockResponse());

        EtapaEditarRequest req = new EtapaEditarRequest(
                "Editada", "desc2", "FINALIZADA", LocalDate.now(), LocalDate.now(), 100,
                BigDecimal.TEN, BigDecimal.ONE, Set.of(1L), BigDecimal.ZERO, BigDecimal.ZERO, "obs2");

        service.editar(1L, 1L, req);

        assertThat(entidad.getNombre()).isEqualTo("Editada");
        assertThat(entidad.getEstado()).isEqualTo(Etapa.EstadoEtapa.FINALIZADA);
        assertThat(entidad.getProveedores()).containsExactly(proveedor);
        verify(auditoria).registrar(
                eq(AccionAuditoria.EDITAR), eq("Etapa"), eq(1L), any(EtapaResponse.class), any(EtapaResponse.class));
    }

    @Test
    void eliminarBorraLaEtapa() {
        when(repo.findByIdAndProyectoId(1L, 1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(Etapa.class))).thenReturn(mockResponse());

        service.eliminar(1L, 1L);

        verify(repo).delete(entidad);
        verify(auditoria).registrar(
                eq(AccionAuditoria.ELIMINAR), eq("Etapa"), eq(1L), any(EtapaResponse.class), eq(null));
    }

    private EtapaResponse mockResponse() {
        return new EtapaResponse(1L, 1L, "x", null, "PENDIENTE", null, null, null, null, null, Set.of(), null, null, null, true);
    }
}

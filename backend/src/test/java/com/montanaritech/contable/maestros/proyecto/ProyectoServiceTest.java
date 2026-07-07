package com.montanaritech.contable.maestros.proyecto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.auth.Usuario;
import com.montanaritech.contable.auth.UsuarioRepository;
import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.ConflictoException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.cliente.ClienteRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proyecto.dto.CuotaRequest;
import com.montanaritech.contable.maestros.proyecto.dto.ProyectoCrearRequest;
import com.montanaritech.contable.maestros.proyecto.dto.ProyectoEditarRequest;
import com.montanaritech.contable.maestros.proyecto.dto.ProyectoResponse;
import com.montanaritech.contable.maestros.proyecto.etapa.EtapaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProyectoServiceTest {

    @Mock
    private ProyectoRepository repo;

    @Mock
    private ProyectoMapper mapper;

    @Mock
    private AuditoriaService auditoria;

    @Mock
    private ClienteRepository clienteRepo;

    @Mock
    private UsuarioRepository usuarioRepo;

    @Mock
    private MonedaRepository monedaRepo;

    @Mock
    private EtapaRepository etapaRepo;

    @InjectMocks
    private ProyectoService service;

    private Cliente cliente;
    private Usuario responsable;
    private Moneda moneda;
    private Proyecto entidad;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Cliente Test");

        responsable = new Usuario();
        responsable.setId(1L);
        responsable.setNombre("Responsable Test");

        moneda = new Moneda();
        moneda.setId(1L);
        moneda.setCodigo("ARS");

        entidad = new Proyecto();
        entidad.setId(1L);
        entidad.setNombre("Proyecto Test");
        entidad.setCliente(cliente);
        entidad.setResponsable(responsable);
        entidad.setMoneda(moneda);
        entidad.setMontoTotal(BigDecimal.valueOf(1000));
        entidad.setActivo(true);
    }

    private ProyectoCrearRequest crearRequestBase(List<CuotaRequest> cuotas) {
        return new ProyectoCrearRequest(
                "Nuevo Proyecto", 1L, 1L, "Argentina", "Desarrollo", null, 1L,
                BigDecimal.valueOf(5000), 3, "comentario", null, null, null,
                LocalDate.of(2026, 12, 31), null, cuotas);
    }

    @Test
    void crearGuardaLaEntidadActivaConEstadosPorDefecto() {
        when(clienteRepo.findById(1L)).thenReturn(Optional.of(cliente));
        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(responsable));
        when(monedaRepo.findById(1L)).thenReturn(Optional.of(moneda));
        when(repo.save(any(Proyecto.class))).thenAnswer(inv -> inv.getArgument(0));

        Proyecto creado = service.crear(crearRequestBase(null));

        assertThat(creado.getNombre()).isEqualTo("Nuevo Proyecto");
        assertThat(creado.isActivo()).isTrue();
        assertThat(creado.getEstado()).isEqualTo(Proyecto.EstadoProyecto.PROSPECTO);
        assertThat(creado.getEstadoComercial()).isEqualTo(Proyecto.EstadoComercial.PROSPECTO);
        assertThat(creado.getEstadoFacturacion()).isEqualTo(Proyecto.EstadoFacturacion.NO_FACTURADO);
        assertThat(creado.getEstadoCobranza()).isEqualTo(Proyecto.EstadoCobranza.PENDIENTE);
    }

    @Test
    void crearSinResponsableEsValido() {
        when(clienteRepo.findById(1L)).thenReturn(Optional.of(cliente));
        when(monedaRepo.findById(1L)).thenReturn(Optional.of(moneda));
        when(repo.save(any(Proyecto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProyectoCrearRequest req = new ProyectoCrearRequest(
                "Sin responsable", 1L, null, null, null, null, 1L,
                BigDecimal.TEN, null, null, null, null, null, null, null, null);

        Proyecto creado = service.crear(req);

        assertThat(creado.getResponsable()).isNull();
    }

    @Test
    void crearConCuotasLasArmaNumeradas() {
        when(clienteRepo.findById(1L)).thenReturn(Optional.of(cliente));
        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(responsable));
        when(monedaRepo.findById(1L)).thenReturn(Optional.of(moneda));
        when(repo.save(any(Proyecto.class))).thenAnswer(inv -> inv.getArgument(0));

        List<CuotaRequest> cuotas = List.of(
                new CuotaRequest(LocalDate.of(2026, 1, 1), BigDecimal.valueOf(2500)),
                new CuotaRequest(LocalDate.of(2026, 2, 1), BigDecimal.valueOf(2500)));

        Proyecto creado = service.crear(crearRequestBase(cuotas));

        assertThat(creado.getCuotas()).hasSize(2);
        assertThat(creado.getCuotas().get(0).getNumero()).isEqualTo(1);
        assertThat(creado.getCuotas().get(1).getNumero()).isEqualTo(2);
        assertThat(creado.getCuotas().get(0).getProyecto()).isEqualTo(creado);
    }

    @Test
    void crearConClienteInexistenteThrow() {
        when(clienteRepo.findById(99L)).thenReturn(Optional.empty());

        ProyectoCrearRequest req = new ProyectoCrearRequest(
                "Test", 99L, null, null, null, null, 1L, BigDecimal.TEN, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.crear(req)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearConMonedaInexistenteThrow() {
        when(clienteRepo.findById(1L)).thenReturn(Optional.of(cliente));
        when(monedaRepo.findById(99L)).thenReturn(Optional.empty());

        ProyectoCrearRequest req = new ProyectoCrearRequest(
                "Test", 1L, null, null, null, null, 99L, BigDecimal.TEN, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.crear(req)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void obtenerConIdInexistenteLanzaNoEncontrado() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void editarActualizaCamposYReemplazaCuotas() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(clienteRepo.findById(1L)).thenReturn(Optional.of(cliente));
        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(responsable));
        when(monedaRepo.findById(1L)).thenReturn(Optional.of(moneda));
        when(mapper.aResponse(any(Proyecto.class))).thenReturn(mockResponse());

        ProyectoEditarRequest req = new ProyectoEditarRequest(
                "Editado", 1L, 1L, "Chile", "Consultoría", "EN_CURSO", 1L,
                BigDecimal.valueOf(9999), 5, "otro comentario", "GANADO", "PARCIALMENTE_FACTURADO", "PARCIAL",
                LocalDate.of(2027, 1, 1), null,
                List.of(new CuotaRequest(LocalDate.of(2026, 6, 1), BigDecimal.valueOf(9999))));

        service.editar(1L, req);

        assertThat(entidad.getNombre()).isEqualTo("Editado");
        assertThat(entidad.getEstado()).isEqualTo(Proyecto.EstadoProyecto.EN_CURSO);
        assertThat(entidad.getEstadoComercial()).isEqualTo(Proyecto.EstadoComercial.GANADO);
        assertThat(entidad.getCuotas()).hasSize(1);
        verify(auditoria).registrar(
                eq(AccionAuditoria.EDITAR), eq("Proyecto"), eq(1L), any(ProyectoResponse.class), any(ProyectoResponse.class));
    }

    @Test
    void desactivarCambiaEstadoAFalse() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(mapper.aResponse(any(Proyecto.class))).thenReturn(mockResponse());

        service.desactivar(1L);

        assertThat(entidad.isActivo()).isFalse();
        verify(auditoria).registrar(
                eq(AccionAuditoria.CAMBIO_ESTADO), eq("Proyecto"), eq(1L), any(ProyectoResponse.class), any(ProyectoResponse.class));
    }

    @Test
    void eliminarConEtapasAsociadasLanzaConflicto() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(etapaRepo.existsByProyectoId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(1L)).isInstanceOf(ConflictoException.class);
    }

    @Test
    void eliminarSinEtapasBorraLaEntidad() {
        when(repo.findById(1L)).thenReturn(Optional.of(entidad));
        when(etapaRepo.existsByProyectoId(1L)).thenReturn(false);
        when(mapper.aResponse(any(Proyecto.class))).thenReturn(mockResponse());

        service.eliminar(1L);

        verify(repo).delete(entidad);
        verify(auditoria).registrar(
                eq(AccionAuditoria.ELIMINAR), eq("Proyecto"), eq(1L), any(ProyectoResponse.class), eq(null));
    }

    private ProyectoResponse mockResponse() {
        return new ProyectoResponse(
                1L, "x", 1L, "cli", 1L, "resp", "pais", "tipo", "PROSPECTO", 1L, "ARS",
                BigDecimal.TEN, 1, "c", "PROSPECTO", "NO_FACTURADO", "PENDIENTE", null, null, List.of(), true);
    }
}

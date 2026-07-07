package com.montanaritech.contable.maestros.proyecto.comision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.comisionista.Comisionista;
import com.montanaritech.contable.maestros.comisionista.ComisionistaRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.comision.dto.ComisionProyectoCrearRequest;
import com.montanaritech.contable.maestros.proyecto.comision.dto.ComisionProyectoEditarRequest;
import com.montanaritech.contable.maestros.proyecto.comision.dto.ComisionProyectoResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ComisionProyectoServiceTest {

    @Mock
    private ComisionProyectoRepository repo;

    @Mock
    private ComisionProyectoMapper mapper;

    @Mock
    private AuditoriaService auditoria;

    @Mock
    private ProyectoRepository proyectoRepo;

    @Mock
    private ComisionistaRepository comisionistaRepo;

    @Mock
    private MonedaRepository monedaRepo;

    private final ComisionCalculoService calculoService = new ComisionCalculoService();

    private ComisionProyectoService service;

    private Proyecto proyecto;
    private Comisionista comisionista;
    private Moneda moneda;

    @BeforeEach
    void setUp() {
        service = new ComisionProyectoService(repo, mapper, auditoria, proyectoRepo, comisionistaRepo, monedaRepo, calculoService);

        proyecto = new Proyecto();
        proyecto.setId(1L);
        proyecto.setMontoTotal(BigDecimal.valueOf(100000));

        comisionista = new Comisionista();
        comisionista.setId(1L);
        comisionista.setNombre("Cristian Pittaluga");

        moneda = new Moneda();
        moneda.setId(1L);
        moneda.setCodigo("ARS");
    }

    @Test
    void crearCalculaImporteEstimado() {
        when(proyectoRepo.findById(1L)).thenReturn(Optional.of(proyecto));
        when(comisionistaRepo.findById(1L)).thenReturn(Optional.of(comisionista));
        when(monedaRepo.findById(1L)).thenReturn(Optional.of(moneda));
        when(repo.save(any(ComisionProyecto.class))).thenAnswer(inv -> inv.getArgument(0));

        ComisionProyectoCrearRequest req = new ComisionProyectoCrearRequest(1L, BigDecimal.TEN, "MONTO_TOTAL", 1L, null, null);

        ComisionProyecto creada = service.crear(1L, req);

        assertThat(creada.getImporteEstimado()).isEqualByComparingTo("10000.00");
        assertThat(creada.getEstadoPago()).isEqualTo(ComisionProyecto.EstadoPago.PENDIENTE);
        assertThat(creada.isActivo()).isTrue();
    }

    @Test
    void crearConComisionistaInexistenteThrow() {
        when(proyectoRepo.findById(1L)).thenReturn(Optional.of(proyecto));
        when(comisionistaRepo.findById(99L)).thenReturn(Optional.empty());

        ComisionProyectoCrearRequest req = new ComisionProyectoCrearRequest(99L, BigDecimal.TEN, "MONTO_TOTAL", 1L, null, null);

        assertThatThrownBy(() -> service.crear(1L, req)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void editarRecalculaAlCambiarPorcentaje() {
        ComisionProyecto existente = new ComisionProyecto();
        existente.setId(5L);
        existente.setProyecto(proyecto);
        existente.setComisionista(comisionista);
        existente.setMoneda(moneda);
        existente.setPorcentajeComision(BigDecimal.TEN);
        existente.setBaseCalculo(ComisionProyecto.BaseCalculo.MONTO_TOTAL);
        existente.setImporteEstimado(BigDecimal.valueOf(10000));

        when(repo.findByIdAndProyectoId(5L, 1L)).thenReturn(Optional.of(existente));
        when(comisionistaRepo.findById(1L)).thenReturn(Optional.of(comisionista));
        when(monedaRepo.findById(1L)).thenReturn(Optional.of(moneda));
        when(mapper.aResponse(any(ComisionProyecto.class))).thenReturn(mockResponse());

        ComisionProyectoEditarRequest req = new ComisionProyectoEditarRequest(1L, BigDecimal.valueOf(20), "MONTO_TOTAL", 1L, null, null, null, null);

        service.editar(1L, 5L, req);

        assertThat(existente.getImporteEstimado()).isEqualByComparingTo("20000.00");
    }

    @Test
    void recalcularEstimadosDeProyectoActualizaTodasLasActivas() {
        ComisionProyecto c1 = new ComisionProyecto();
        c1.setPorcentajeComision(BigDecimal.TEN);
        c1.setBaseCalculo(ComisionProyecto.BaseCalculo.MONTO_TOTAL);
        c1.setImporteEstimado(BigDecimal.valueOf(10000));

        when(repo.findByProyectoIdAndActivoTrue(1L)).thenReturn(List.of(c1));

        proyecto.setMontoTotal(BigDecimal.valueOf(200000));
        service.recalcularEstimadosDeProyecto(proyecto);

        assertThat(c1.getImporteEstimado()).isEqualByComparingTo("20000.00");
    }

    private ComisionProyectoResponse mockResponse() {
        return new ComisionProyectoResponse(5L, 1L, "p", 1L, "c", BigDecimal.TEN, "MONTO_TOTAL", 1L, "ARS",
                BigDecimal.valueOf(10000), null, "PENDIENTE", null, null, true);
    }
}

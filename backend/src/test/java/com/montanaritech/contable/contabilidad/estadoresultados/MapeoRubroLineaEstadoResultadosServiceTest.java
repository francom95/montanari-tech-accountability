package com.montanaritech.contable.contabilidad.estadoresultados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.audit.AccionAuditoria;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.MapeoRubroLineaErDtos.CrearRequest;
import com.montanaritech.contable.contabilidad.estadoresultados.dto.MapeoRubroLineaErDtos.EditarRequest;
import com.montanaritech.contable.maestros.categoria.Categoria;
import com.montanaritech.contable.maestros.rubro.Rubro;
import com.montanaritech.contable.maestros.rubro.RubroRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

/** CRUD del mapeo rubro→línea del estado de resultados (F7.3). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MapeoRubroLineaEstadoResultadosServiceTest {

    @Mock private MapeoRubroLineaEstadoResultadosRepository repo;
    @Mock private RubroRepository rubroRepo;
    @Mock private MapeoRubroLineaEstadoResultadosMapper mapper;
    @Mock private AuditoriaService auditoria;

    @InjectMocks
    private MapeoRubroLineaEstadoResultadosService service;

    private Rubro rubro;

    @BeforeEach
    void setUp() {
        rubro = new Rubro();
        rubro.setId(1L);
        rubro.setNombre("Ingresos por servicios brutos");
        when(rubroRepo.findById(1L)).thenReturn(Optional.of(rubro));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void crearConRubroInexistenteLanzaExcepcion() {
        when(rubroRepo.findById(99L)).thenReturn(Optional.empty());
        CrearRequest req = new CrearRequest(99L, Categoria.TipoCategoria.RP, LineaEstadoResultados.INGRESOS_POR_VENTAS);

        assertThatThrownBy(() -> service.crear(req)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearConNaturalezaFueraDeRpRnLanzaExcepcion() {
        CrearRequest req = new CrearRequest(1L, Categoria.TipoCategoria.ACTIVO, LineaEstadoResultados.INGRESOS_POR_VENTAS);

        assertThatThrownBy(() -> service.crear(req))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("NATURALEZA_INVALIDA_PARA_ER");
    }

    @Test
    void crearResuelveRubroYGuardaYAudita() {
        CrearRequest req = new CrearRequest(1L, Categoria.TipoCategoria.RP, LineaEstadoResultados.INGRESOS_POR_VENTAS);

        MapeoRubroLineaEstadoResultados guardado = service.crear(req);

        assertThat(guardado.getRubro()).isEqualTo(rubro);
        assertThat(guardado.getNaturaleza()).isEqualTo(Categoria.TipoCategoria.RP);
        assertThat(guardado.getLinea()).isEqualTo(LineaEstadoResultados.INGRESOS_POR_VENTAS);
        verify(auditoria).registrar(any(AccionAuditoria.class), org.mockito.ArgumentMatchers.eq("MapeoRubroLineaEstadoResultados"),
                any(), any(), any());
    }

    @Test
    void crearDuplicadoConvierteViolacionEnNegocioException() {
        when(repo.save(any())).thenThrow(new DataIntegrityViolationException("duplicado"));
        CrearRequest req = new CrearRequest(1L, Categoria.TipoCategoria.RP, LineaEstadoResultados.INGRESOS_POR_VENTAS);

        assertThatThrownBy(() -> service.crear(req))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("MAPEO_RUBRO_LINEA_DUPLICADO");
    }

    @Test
    void editarActualizaLineaYAudita() {
        MapeoRubroLineaEstadoResultados existente = new MapeoRubroLineaEstadoResultados();
        existente.setId(5L);
        existente.setRubro(rubro);
        existente.setNaturaleza(Categoria.TipoCategoria.RP);
        existente.setLinea(LineaEstadoResultados.INGRESOS_POR_VENTAS);
        when(repo.findById(5L)).thenReturn(Optional.of(existente));

        MapeoRubroLineaEstadoResultados editado = service.editar(5L, new EditarRequest(LineaEstadoResultados.OTROS_INGRESOS));

        assertThat(editado.getLinea()).isEqualTo(LineaEstadoResultados.OTROS_INGRESOS);
        verify(auditoria).registrar(org.mockito.ArgumentMatchers.eq(AccionAuditoria.EDITAR), any(), any(), any(), any());
    }

    @Test
    void eliminarAudita() {
        MapeoRubroLineaEstadoResultados existente = new MapeoRubroLineaEstadoResultados();
        existente.setId(7L);
        existente.setRubro(rubro);
        when(repo.findById(7L)).thenReturn(Optional.of(existente));

        service.eliminar(7L);

        verify(repo).delete(existente);
        verify(auditoria).registrar(org.mockito.ArgumentMatchers.eq(AccionAuditoria.ELIMINAR), any(), any(), any(), any());
    }

    @Test
    void obtenerConIdInexistenteLanzaExcepcion() {
        when(repo.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(404L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}

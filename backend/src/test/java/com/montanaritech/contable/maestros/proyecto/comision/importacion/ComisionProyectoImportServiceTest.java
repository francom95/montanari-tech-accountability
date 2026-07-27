package com.montanaritech.contable.maestros.proyecto.comision.importacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.maestros.comisionista.Comisionista;
import com.montanaritech.contable.maestros.comisionista.ComisionistaRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.comision.ComisionProyecto;
import com.montanaritech.contable.maestros.proyecto.comision.ComisionProyectoMapper;
import com.montanaritech.contable.maestros.proyecto.comision.ComisionProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.comision.ComisionProyectoService;
import com.montanaritech.contable.maestros.proyecto.comision.dto.ComisionProyectoResponse;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ComisionProyectoImportServiceTest {

    private ProyectoRepository proyectoRepo;
    private ComisionistaRepository comisionistaRepo;
    private ComisionProyectoRepository comisionProyectoRepo;
    private ComisionProyectoService comisionProyectoService;
    private ComisionProyectoMapper comisionProyectoMapper;
    private ComisionProyectoImportService service;

    private List<ComisionProyectoImportFilaCruda> filasAServir;

    @BeforeEach
    void setUp() {
        proyectoRepo = mock(ProyectoRepository.class);
        comisionistaRepo = mock(ComisionistaRepository.class);
        comisionProyectoRepo = mock(ComisionProyectoRepository.class);
        comisionProyectoService = mock(ComisionProyectoService.class);
        comisionProyectoMapper = mock(ComisionProyectoMapper.class);

        ComisionProyectoImportParser parserFalso = new ComisionProyectoImportParser() {
            @Override
            public boolean soporta(String nombreArchivo) {
                return nombreArchivo.endsWith(".xlsx");
            }

            @Override
            public List<ComisionProyectoImportFilaCruda> parsear(InputStream in) {
                return filasAServir;
            }
        };

        service = new ComisionProyectoImportService(List.of(parserFalso), proyectoRepo, comisionistaRepo,
                comisionProyectoRepo, comisionProyectoService, comisionProyectoMapper);
    }

    private MockMultipartFile archivoXlsx() {
        return new MockMultipartFile("archivo", "comisiones.xlsx", "application/vnd.ms-excel", new byte[]{1, 2, 3});
    }

    @Test
    void previsualizarDetectaMonedaUsdPorComentario() {
        when(proyectoRepo.findByNombreIgnoreCase("Acme")).thenReturn(Optional.of(new Proyecto()));
        when(comisionistaRepo.findByNombreIgnoreCase("Cristian Pittaluga")).thenReturn(Optional.of(new Comisionista()));
        filasAServir = List.of(new ComisionProyectoImportFilaCruda(2, "Acme", "Cristian Pittaluga", "10", "500", "En dols"));

        List<ComisionProyectoImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).esUsd()).isTrue();
        assertThat(resultado.get(0).errores()).isEmpty();
    }

    @Test
    void previsualizarComisionistaNoEncontradoGeneraError() {
        when(proyectoRepo.findByNombreIgnoreCase("Acme")).thenReturn(Optional.of(new Proyecto()));
        when(comisionistaRepo.findByNombreIgnoreCase("Fantasma")).thenReturn(Optional.empty());
        filasAServir = List.of(new ComisionProyectoImportFilaCruda(2, "Acme", "Fantasma", "", "500", ""));

        List<ComisionProyectoImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).errores()).anyMatch(e -> e.contains("Comisionista 'Fantasma' no encontrado"));
    }

    @Test
    void previsualizarFormatoNoSoportadoLanzaNegocioException() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "comisiones.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.previsualizar(archivo)).isInstanceOf(NegocioException.class);
    }

    @Test
    void confirmarConPorcentajeCreaSinLlamarEditar() {
        ComisionProyectoImportFilaDto fila = new ComisionProyectoImportFilaDto(
                2, "Acme", "Cristian Pittaluga", BigDecimal.TEN, BigDecimal.valueOf(500), false, List.of());

        Proyecto proyecto = new Proyecto();
        proyecto.setId(1L);
        proyecto.setMontoTotal(BigDecimal.valueOf(5000));
        Comisionista comisionista = new Comisionista();
        comisionista.setId(2L);
        when(proyectoRepo.findByNombreIgnoreCase("Acme")).thenReturn(Optional.of(proyecto));
        when(comisionistaRepo.findByNombreIgnoreCase("Cristian Pittaluga")).thenReturn(Optional.of(comisionista));
        when(comisionProyectoRepo.existsByProyectoIdAndComisionistaId(1L, 2L)).thenReturn(false);

        ComisionProyecto creada = new ComisionProyecto();
        creada.setId(9L);
        when(comisionProyectoService.crear(anyLong(), any())).thenReturn(creada);
        when(comisionProyectoMapper.aResponse(creada)).thenReturn(mockResponse());

        ComisionProyectoImportResultado resultado = service.confirmar(3L, 4L, List.of(fila));

        assertThat(resultado.creadas()).hasSize(1);
        verify(comisionProyectoService, never()).editar(anyLong(), anyLong(), any());
    }

    @Test
    void confirmarSinPorcentajeCalculaSinteticoYLlamaEditar() {
        ComisionProyectoImportFilaDto fila = new ComisionProyectoImportFilaDto(
                2, "Acme", "Cristian Pittaluga", null, BigDecimal.valueOf(500), false, List.of());

        Proyecto proyecto = new Proyecto();
        proyecto.setId(1L);
        proyecto.setMontoTotal(BigDecimal.valueOf(5000));
        Comisionista comisionista = new Comisionista();
        comisionista.setId(2L);
        when(proyectoRepo.findByNombreIgnoreCase("Acme")).thenReturn(Optional.of(proyecto));
        when(comisionistaRepo.findByNombreIgnoreCase("Cristian Pittaluga")).thenReturn(Optional.of(comisionista));
        when(comisionProyectoRepo.existsByProyectoIdAndComisionistaId(1L, 2L)).thenReturn(false);

        ComisionProyecto creada = new ComisionProyecto();
        creada.setId(9L);
        when(comisionProyectoService.crear(anyLong(), any())).thenReturn(creada);
        when(comisionProyectoService.editar(anyLong(), anyLong(), any())).thenReturn(creada);
        when(comisionProyectoMapper.aResponse(creada)).thenReturn(mockResponse());

        ComisionProyectoImportResultado resultado = service.confirmar(3L, 4L, List.of(fila));

        assertThat(resultado.creadas()).hasSize(1);
        verify(comisionProyectoService).editar(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.argThat(r -> r.importeFinal().compareTo(BigDecimal.valueOf(500)) == 0));
    }

    @Test
    void confirmarNoDuplicaSiYaExiste() {
        ComisionProyectoImportFilaDto fila = new ComisionProyectoImportFilaDto(
                2, "Acme", "Cristian Pittaluga", BigDecimal.TEN, BigDecimal.valueOf(500), false, List.of());

        Proyecto proyecto = new Proyecto();
        proyecto.setId(1L);
        proyecto.setMontoTotal(BigDecimal.valueOf(5000));
        Comisionista comisionista = new Comisionista();
        comisionista.setId(2L);
        when(proyectoRepo.findByNombreIgnoreCase("Acme")).thenReturn(Optional.of(proyecto));
        when(comisionistaRepo.findByNombreIgnoreCase("Cristian Pittaluga")).thenReturn(Optional.of(comisionista));
        when(comisionProyectoRepo.existsByProyectoIdAndComisionistaId(1L, 2L)).thenReturn(true);

        ComisionProyectoImportResultado resultado = service.confirmar(3L, 4L, List.of(fila));

        assertThat(resultado.creadas()).isEmpty();
        assertThat(resultado.yaExistian()).isEqualTo(1);
        verify(comisionProyectoService, never()).crear(anyLong(), any());
    }

    private ComisionProyectoResponse mockResponse() {
        return new ComisionProyectoResponse(9L, 1L, "Acme", 2L, "Cristian Pittaluga", BigDecimal.TEN,
                "MONTO_TOTAL", 3L, "ARS", BigDecimal.valueOf(500), null, "PENDIENTE", null, null, true);
    }
}

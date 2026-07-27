package com.montanaritech.contable.maestros.proyecto.importacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.cliente.ClienteRepository;
import com.montanaritech.contable.maestros.proyecto.Proyecto;
import com.montanaritech.contable.maestros.proyecto.ProyectoMapper;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.ProyectoService;
import com.montanaritech.contable.maestros.proyecto.dto.ProyectoResponse;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ProyectoImportServiceTest {

    private ClienteRepository clienteRepo;
    private ProyectoRepository proyectoRepo;
    private ProyectoService proyectoService;
    private ProyectoMapper proyectoMapper;
    private ProyectoImportService service;

    private List<ProyectoImportFilaCruda> filasAServir;

    @BeforeEach
    void setUp() {
        clienteRepo = mock(ClienteRepository.class);
        proyectoRepo = mock(ProyectoRepository.class);
        proyectoService = mock(ProyectoService.class);
        proyectoMapper = mock(ProyectoMapper.class);

        ProyectoImportParser parserFalso = new ProyectoImportParser() {
            @Override
            public boolean soporta(String nombreArchivo) {
                return nombreArchivo.endsWith(".xlsx");
            }

            @Override
            public List<ProyectoImportFilaCruda> parsear(InputStream in) {
                return filasAServir;
            }
        };

        service = new ProyectoImportService(List.of(parserFalso), clienteRepo, proyectoRepo, proyectoService, proyectoMapper);
    }

    private MockMultipartFile archivoXlsx() {
        return new MockMultipartFile("archivo", "proyectos.xlsx", "application/vnd.ms-excel", new byte[]{1, 2, 3});
    }

    private ProyectoImportFilaCruda filaCon(String compuesto, String pais, String monto, String comentarios) {
        return new ProyectoImportFilaCruda(2, compuesto, "", pais, "", "", "", monto, "", "", "", "", "", "", "", "", comentarios);
    }

    @Test
    void previsualizarSepararClienteYProyectoDelTextoCompuesto() {
        when(clienteRepo.findByNombreIgnoreCase("Acme")).thenReturn(Optional.of(new Cliente()));
        filasAServir = List.of(filaCon("Acme - (Fase: 1)", "Argentina", "1000", ""));

        List<ProyectoImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).clienteNombre()).isEqualTo("Acme");
        assertThat(resultado.get(0).proyectoNombre()).isEqualTo("Acme - (Fase: 1)");
        assertThat(resultado.get(0).tipoProyecto()).isEqualTo("ARGENTINA");
        assertThat(resultado.get(0).errores()).isEmpty();
    }

    @Test
    void previsualizarPaisNoArgentinaEsExterior() {
        when(clienteRepo.findByNombreIgnoreCase("Acme")).thenReturn(Optional.of(new Cliente()));
        filasAServir = List.of(filaCon("Acme - Proyecto X", "Colombia", "1000", ""));

        List<ProyectoImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).tipoProyecto()).isEqualTo("EXTERIOR");
    }

    @Test
    void previsualizarClienteNoEncontradoGeneraError() {
        when(clienteRepo.findByNombreIgnoreCase("Fantasma")).thenReturn(Optional.empty());
        filasAServir = List.of(filaCon("Fantasma - Proyecto X", "Argentina", "1000", ""));

        List<ProyectoImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).errores()).anyMatch(e -> e.contains("Cliente 'Fantasma' no encontrado"));
    }

    @Test
    void previsualizarHeuristicaEstadoProspectoSinMontoNiComentario() {
        when(clienteRepo.findByNombreIgnoreCase("Acme")).thenReturn(Optional.of(new Cliente()));
        filasAServir = List.of(filaCon("Acme - Proyecto X", "Argentina", "0", ""));

        List<ProyectoImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).estado()).isEqualTo("PROSPECTO");
    }

    @Test
    void previsualizarHeuristicaEstadoFinalizadoPorComentario() {
        when(clienteRepo.findByNombreIgnoreCase("Acme")).thenReturn(Optional.of(new Cliente()));
        filasAServir = List.of(filaCon("Acme - Proyecto X", "Argentina", "5000", "Proyecto FINALIZADO en marzo"));

        List<ProyectoImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).estado()).isEqualTo("FINALIZADO");
    }

    @Test
    void previsualizarHeuristicaEstadoEnCursoPorDefecto() {
        when(clienteRepo.findByNombreIgnoreCase("Acme")).thenReturn(Optional.of(new Cliente()));
        filasAServir = List.of(filaCon("Acme - Proyecto X", "Argentina", "5000", "avanzando bien"));

        List<ProyectoImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).estado()).isEqualTo("EN_CURSO");
    }

    @Test
    void previsualizarFormatoNoSoportadoLanzaNegocioException() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "proyectos.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.previsualizar(archivo)).isInstanceOf(NegocioException.class);
    }

    @Test
    void confirmarCreaFilaValidaYRechazaClienteNoEncontrado() {
        ProyectoImportFilaDto valida = new ProyectoImportFilaDto(
                2, "Acme", "Acme - Proyecto X", "Argentina", "ARGENTINA", "EN_CURSO",
                BigDecimal.valueOf(1000), List.of(), null, List.of());
        ProyectoImportFilaDto invalida = new ProyectoImportFilaDto(
                3, "Fantasma", "Fantasma - Proyecto Y", "Argentina", "ARGENTINA", "EN_CURSO",
                BigDecimal.valueOf(500), List.of(), null, List.of());

        Cliente acme = new Cliente();
        acme.setId(1L);
        when(clienteRepo.findByNombreIgnoreCase("Acme")).thenReturn(Optional.of(acme));
        when(clienteRepo.findByNombreIgnoreCase("Fantasma")).thenReturn(Optional.empty());
        when(proyectoRepo.findByNombreIgnoreCaseAndClienteId("Acme - Proyecto X", 1L)).thenReturn(Optional.empty());

        Proyecto creado = new Proyecto();
        creado.setId(10L);
        when(proyectoService.crear(any())).thenReturn(creado);
        when(proyectoMapper.aResponse(creado)).thenReturn(mockResponse());

        ProyectoImportResultado resultado = service.confirmar(2L, List.of(valida, invalida));

        assertThat(resultado.creadas()).hasSize(1);
        assertThat(resultado.rechazadas()).hasSize(1);
        assertThat(resultado.rechazadas().get(0).errores()).anyMatch(e -> e.contains("Cliente 'Fantasma' no encontrado"));
        verify(proyectoService, never()).crear(org.mockito.ArgumentMatchers.argThat(
                r -> r != null && "Fantasma - Proyecto Y".equals(r.nombre())));
    }

    @Test
    void confirmarNoDuplicaSiYaExiste() {
        ProyectoImportFilaDto valida = new ProyectoImportFilaDto(
                2, "Acme", "Acme - Proyecto X", "Argentina", "ARGENTINA", "EN_CURSO",
                BigDecimal.valueOf(1000), List.of(), null, List.of());
        Cliente acme = new Cliente();
        acme.setId(1L);
        when(clienteRepo.findByNombreIgnoreCase("Acme")).thenReturn(Optional.of(acme));
        Proyecto existente = new Proyecto();
        existente.setId(5L);
        when(proyectoRepo.findByNombreIgnoreCaseAndClienteId("Acme - Proyecto X", 1L)).thenReturn(Optional.of(existente));

        ProyectoImportResultado resultado = service.confirmar(2L, List.of(valida));

        assertThat(resultado.creadas()).isEmpty();
        assertThat(resultado.yaExistian()).isEqualTo(1);
        verify(proyectoService, never()).crear(any());
    }

    private ProyectoResponse mockResponse() {
        return new ProyectoResponse(10L, "Acme - Proyecto X", 1L, "Acme", null, null, "Argentina", "ARGENTINA",
                "EN_CURSO", 2L, "USD", BigDecimal.valueOf(1000), null, null,
                "PROSPECTO", "NO_FACTURADO", "PENDIENTE", null, null, List.of(), true);
    }
}

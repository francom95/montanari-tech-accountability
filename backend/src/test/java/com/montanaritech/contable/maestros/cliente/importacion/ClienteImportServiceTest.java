package com.montanaritech.contable.maestros.cliente.importacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.cliente.ClienteMapper;
import com.montanaritech.contable.maestros.cliente.ClienteRepository;
import com.montanaritech.contable.maestros.cliente.ClienteService;
import com.montanaritech.contable.maestros.cliente.dto.ClienteResponse;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ClienteImportServiceTest {

    private ClienteRepository clienteRepo;
    private ClienteService clienteService;
    private ClienteMapper clienteMapper;
    private ClienteImportService service;

    private List<ClienteImportFilaCruda> filasAServir;

    @BeforeEach
    void setUp() {
        clienteRepo = mock(ClienteRepository.class);
        clienteService = mock(ClienteService.class);
        clienteMapper = mock(ClienteMapper.class);

        ClienteImportParser parserFalso = new ClienteImportParser() {
            @Override
            public boolean soporta(String nombreArchivo) {
                return nombreArchivo.endsWith(".xlsx");
            }

            @Override
            public List<ClienteImportFilaCruda> parsear(InputStream in) {
                return filasAServir;
            }
        };

        service = new ClienteImportService(List.of(parserFalso), clienteRepo, clienteService, clienteMapper);
    }

    private MockMultipartFile archivoXlsx() {
        return new MockMultipartFile("archivo", "clientes.xlsx", "application/vnd.ms-excel", new byte[]{1, 2, 3});
    }

    @Test
    void previsualizarFilaValidaSinErrores() {
        filasAServir = List.of(new ClienteImportFilaCruda(2, "Acme SA", "Acme Sociedad Anónima", "30-71234567-4"));

        List<ClienteImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).errores()).isEmpty();
        assertThat(resultado.get(0).nombre()).isEqualTo("Acme Sociedad Anónima");
        assertThat(resultado.get(0).cuit()).isEqualTo("30-71234567-4");
    }

    @Test
    void previsualizarSinCuitGeneraError() {
        filasAServir = List.of(new ClienteImportFilaCruda(2, "Acme SA", "Acme Sociedad Anónima", ""));

        List<ClienteImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).errores()).anyMatch(e -> e.contains("CUIT no informado"));
    }

    @Test
    void previsualizarUsaNombreClientesSiFaltaRazonSocial() {
        filasAServir = List.of(new ClienteImportFilaCruda(2, "Acme SA", "", "30-71234567-4"));

        List<ClienteImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).nombre()).isEqualTo("Acme SA");
    }

    @Test
    void previsualizarFormatoNoSoportadoLanzaNegocioException() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "clientes.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.previsualizar(archivo)).isInstanceOf(NegocioException.class);
    }

    @Test
    void confirmarCreaFilaValidaYRechazaSinCuit() {
        ClienteImportFilaDto valida = new ClienteImportFilaDto(2, "Acme SA", "30-71234567-4", List.of());
        ClienteImportFilaDto sinCuit = new ClienteImportFilaDto(3, "Fantasma SA", null, List.of());

        when(clienteRepo.findByNombreIgnoreCase("Acme SA")).thenReturn(Optional.empty());
        Cliente creado = new Cliente();
        creado.setId(10L);
        when(clienteService.crear(any())).thenReturn(creado);
        when(clienteMapper.aResponse(creado)).thenReturn(mockResponse());

        ClienteImportResultado resultado = service.confirmar(1L, List.of(valida, sinCuit));

        assertThat(resultado.creadas()).hasSize(1);
        assertThat(resultado.rechazadas()).hasSize(1);
        assertThat(resultado.yaExistian()).isZero();
        verify(clienteService, never()).crear(org.mockito.ArgumentMatchers.argThat(
                r -> r != null && "Fantasma SA".equals(r.nombre())));
    }

    @Test
    void confirmarNoDuplicaSiYaExiste() {
        ClienteImportFilaDto valida = new ClienteImportFilaDto(2, "Acme SA", "30-71234567-4", List.of());
        Cliente existente = new Cliente();
        existente.setId(5L);
        when(clienteRepo.findByNombreIgnoreCase("Acme SA")).thenReturn(Optional.of(existente));

        ClienteImportResultado resultado = service.confirmar(1L, List.of(valida));

        assertThat(resultado.creadas()).isEmpty();
        assertThat(resultado.yaExistian()).isEqualTo(1);
        verify(clienteService, never()).crear(any());
    }

    private ClienteResponse mockResponse() {
        return new ClienteResponse(10L, "Acme SA", "30-71234567-4", 1L, "CABA", null, null, null, null, null, true);
    }
}

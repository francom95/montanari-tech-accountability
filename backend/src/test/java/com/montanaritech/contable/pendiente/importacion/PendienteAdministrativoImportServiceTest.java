package com.montanaritech.contable.pendiente.importacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.pendiente.EstadoPendiente;
import com.montanaritech.contable.pendiente.PendienteAdministrativo;
import com.montanaritech.contable.pendiente.PendienteAdministrativoMapper;
import com.montanaritech.contable.pendiente.PendienteAdministrativoRepository;
import com.montanaritech.contable.pendiente.PendienteAdministrativoService;
import com.montanaritech.contable.pendiente.PrioridadPendiente;
import com.montanaritech.contable.pendiente.dto.PendienteAdministrativoResponse;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class PendienteAdministrativoImportServiceTest {

    private PendienteAdministrativoRepository pendienteRepo;
    private PendienteAdministrativoService pendienteService;
    private PendienteAdministrativoMapper pendienteMapper;
    private PendienteAdministrativoImportService service;

    private List<PendienteAdministrativoImportFilaCruda> filasAServir;

    @BeforeEach
    void setUp() {
        pendienteRepo = mock(PendienteAdministrativoRepository.class);
        pendienteService = mock(PendienteAdministrativoService.class);
        pendienteMapper = mock(PendienteAdministrativoMapper.class);

        PendienteAdministrativoImportParser parserFalso = new PendienteAdministrativoImportParser() {
            @Override
            public boolean soporta(String nombreArchivo) {
                return nombreArchivo.endsWith(".xlsx");
            }

            @Override
            public List<PendienteAdministrativoImportFilaCruda> parsear(InputStream in) {
                return filasAServir;
            }
        };

        service = new PendienteAdministrativoImportService(List.of(parserFalso), pendienteRepo, pendienteService, pendienteMapper);
    }

    private MockMultipartFile archivoXlsx() {
        return new MockMultipartFile("archivo", "pendientes.xlsx", "application/vnd.ms-excel", new byte[]{1, 2, 3});
    }

    @Test
    void previsualizarFilaValidaSinErrores() {
        filasAServir = List.of(new PendienteAdministrativoImportFilaCruda(2, "Renovar certificado de la AFIP"));

        List<PendienteAdministrativoImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).titulo()).isEqualTo("Renovar certificado de la AFIP");
        assertThat(resultado.get(0).errores()).isEmpty();
    }

    @Test
    void previsualizarSinTituloGeneraError() {
        filasAServir = List.of(new PendienteAdministrativoImportFilaCruda(2, ""));

        List<PendienteAdministrativoImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).errores()).contains("El título es obligatorio");
    }

    @Test
    void previsualizarFormatoNoSoportadoLanzaNegocioException() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "pendientes.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.previsualizar(archivo)).isInstanceOf(NegocioException.class);
    }

    @Test
    void confirmarCreaConPrioridadMediaFija() {
        PendienteAdministrativoImportFilaDto valida = new PendienteAdministrativoImportFilaDto(2, "Renovar certificado", List.of());
        when(pendienteRepo.findByTitulo("Renovar certificado")).thenReturn(Optional.empty());
        PendienteAdministrativo creado = new PendienteAdministrativo();
        creado.setId(3L);
        when(pendienteService.crear(any())).thenReturn(creado);
        when(pendienteMapper.aResponse(creado)).thenReturn(mockResponse());

        PendienteAdministrativoImportResultado resultado = service.confirmar(List.of(valida));

        assertThat(resultado.creadas()).hasSize(1);
        verify(pendienteService).crear(org.mockito.ArgumentMatchers.argThat(
                r -> r.prioridad() == PrioridadPendiente.MEDIA && r.categoria() == null && r.fechaEstimadaResolucion() == null));
    }

    @Test
    void confirmarNoDuplicaSiYaExiste() {
        PendienteAdministrativoImportFilaDto valida = new PendienteAdministrativoImportFilaDto(2, "Renovar certificado", List.of());
        PendienteAdministrativo existente = new PendienteAdministrativo();
        existente.setId(9L);
        when(pendienteRepo.findByTitulo("Renovar certificado")).thenReturn(Optional.of(existente));

        PendienteAdministrativoImportResultado resultado = service.confirmar(List.of(valida));

        assertThat(resultado.creadas()).isEmpty();
        assertThat(resultado.yaExistian()).isEqualTo(1);
        verify(pendienteService, never()).crear(any());
    }

    private PendienteAdministrativoResponse mockResponse() {
        return new PendienteAdministrativoResponse(3L, "Renovar certificado", null, null, PrioridadPendiente.MEDIA,
                EstadoPendiente.PENDIENTE, null, null, null, null, null, null, null, null, null, null, true);
    }
}

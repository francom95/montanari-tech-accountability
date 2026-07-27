package com.montanaritech.contable.compromiso.importacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.compromiso.Compromiso;
import com.montanaritech.contable.compromiso.CompromisoMapper;
import com.montanaritech.contable.compromiso.CompromisoRepository;
import com.montanaritech.contable.compromiso.CompromisoService;
import com.montanaritech.contable.compromiso.EstadoCompromiso;
import com.montanaritech.contable.compromiso.TipoCompromiso;
import com.montanaritech.contable.compromiso.dto.CompromisoResponse;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class CompromisoImportServiceTest {

    private CompromisoRepository compromisoRepo;
    private CompromisoService compromisoService;
    private CompromisoMapper compromisoMapper;
    private CompromisoImportService service;

    private List<CompromisoImportFilaCruda> filasAServir;

    @BeforeEach
    void setUp() {
        compromisoRepo = mock(CompromisoRepository.class);
        compromisoService = mock(CompromisoService.class);
        compromisoMapper = mock(CompromisoMapper.class);

        CompromisoImportParser parserFalso = new CompromisoImportParser() {
            @Override
            public boolean soporta(String nombreArchivo) {
                return nombreArchivo.endsWith(".xlsx");
            }

            @Override
            public List<CompromisoImportFilaCruda> parsear(InputStream in) {
                return filasAServir;
            }
        };

        service = new CompromisoImportService(List.of(parserFalso), compromisoRepo, compromisoService, compromisoMapper);
    }

    private MockMultipartFile archivoXlsx() {
        return new MockMultipartFile("archivo", "compromisos.xlsx", "application/vnd.ms-excel", new byte[]{1, 2, 3});
    }

    @Test
    void previsualizarFilaValidaSinErrores() {
        filasAServir = List.of(new CompromisoImportFilaCruda(2, "Cuota 1/12 Plan IG", "20/03/2026", "15.000,50"));

        List<CompromisoImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).errores()).isEmpty();
        assertThat(resultado.get(0).fechaPrevista()).isEqualTo(LocalDate.of(2026, 3, 20));
        assertThat(resultado.get(0).importe()).isEqualByComparingTo("15000.50");
    }

    @Test
    void previsualizarSinConceptoGeneraError() {
        filasAServir = List.of(new CompromisoImportFilaCruda(2, "", "20/03/2026", "15000"));

        List<CompromisoImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).errores()).contains("El concepto es obligatorio");
    }

    @Test
    void previsualizarFechaInvalidaGeneraErrorSinDuplicar() {
        filasAServir = List.of(new CompromisoImportFilaCruda(2, "Cuota 1", "31/13/2026", "15000"));

        List<CompromisoImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).errores()).hasSize(1);
        assertThat(resultado.get(0).errores().get(0)).contains("Formato de fecha de vencimiento inválido");
    }

    @Test
    void previsualizarFormatoNoSoportadoLanzaNegocioException() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "compromisos.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.previsualizar(archivo)).isInstanceOf(NegocioException.class);
    }

    @Test
    void confirmarCreaFilaValidaConVencimiento() {
        CompromisoImportFilaDto valida = new CompromisoImportFilaDto(2, "Cuota 1", LocalDate.of(2026, 3, 20), BigDecimal.valueOf(15000), List.of());

        when(compromisoRepo.existsByConceptoAndFechaPrevista("Cuota 1", LocalDate.of(2026, 3, 20))).thenReturn(false);
        Compromiso creado = new Compromiso();
        creado.setId(5L);
        when(compromisoService.crear(any())).thenReturn(creado);
        when(compromisoMapper.aResponse(creado)).thenReturn(mockResponse());

        CompromisoImportResultado resultado = service.confirmar(1L, List.of(valida));

        assertThat(resultado.creadas()).hasSize(1);
        verify(compromisoService).crear(org.mockito.ArgumentMatchers.argThat(
                r -> r.tipo() == TipoCompromiso.CUOTA_PLAN_DE_PAGOS && r.generarVencimiento()
                        && r.proveedorId() == null && r.proyectoId() == null));
    }

    @Test
    void confirmarNoDuplicaSiYaExiste() {
        CompromisoImportFilaDto valida = new CompromisoImportFilaDto(2, "Cuota 1", LocalDate.of(2026, 3, 20), BigDecimal.valueOf(15000), List.of());
        when(compromisoRepo.existsByConceptoAndFechaPrevista("Cuota 1", LocalDate.of(2026, 3, 20))).thenReturn(true);

        CompromisoImportResultado resultado = service.confirmar(1L, List.of(valida));

        assertThat(resultado.creadas()).isEmpty();
        assertThat(resultado.yaExistian()).isEqualTo(1);
        verify(compromisoService, never()).crear(any());
    }

    private CompromisoResponse mockResponse() {
        return new CompromisoResponse(5L, "Cuota 1", TipoCompromiso.CUOTA_PLAN_DE_PAGOS, LocalDate.of(2026, 3, 20),
                BigDecimal.valueOf(15000), 1L, "ARS", null, null, null, null, EstadoCompromiso.PENDIENTE, null, null, true);
    }
}

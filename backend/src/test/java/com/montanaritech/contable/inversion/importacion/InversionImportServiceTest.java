package com.montanaritech.contable.inversion.importacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.inversion.Inversion;
import com.montanaritech.contable.inversion.InversionRepository;
import com.montanaritech.contable.inversion.InversionService;
import com.montanaritech.contable.inversion.MovimientoInversion;
import com.montanaritech.contable.inversion.MovimientoInversionRepository;
import com.montanaritech.contable.inversion.MovimientoInversionService;
import com.montanaritech.contable.inversion.TipoMovimientoInversion;
import com.montanaritech.contable.inversion.dto.MovimientoInversionResponse;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class InversionImportServiceTest {

    private InversionRepository inversionRepo;
    private InversionService inversionService;
    private MovimientoInversionRepository movimientoInversionRepo;
    private MovimientoInversionService movimientoInversionService;
    private InversionImportService service;

    private List<InversionImportFilaCruda> filasAServir;

    @BeforeEach
    void setUp() {
        inversionRepo = mock(InversionRepository.class);
        inversionService = mock(InversionService.class);
        movimientoInversionRepo = mock(MovimientoInversionRepository.class);
        movimientoInversionService = mock(MovimientoInversionService.class);

        InversionImportParser parserFalso = new InversionImportParser() {
            @Override
            public boolean soporta(String nombreArchivo) {
                return nombreArchivo.endsWith(".xlsx");
            }

            @Override
            public List<InversionImportFilaCruda> parsear(InputStream in) {
                return filasAServir;
            }
        };

        service = new InversionImportService(List.of(parserFalso), inversionRepo, inversionService,
                movimientoInversionRepo, movimientoInversionService);
    }

    private MockMultipartFile archivoXlsx() {
        return new MockMultipartFile("archivo", "inversiones.xlsx", "application/vnd.ms-excel", new byte[]{1, 2, 3});
    }

    @Test
    void previsualizarDescartaFilaDeRevaluacionSinOperacion() {
        filasAServir = List.of(
                new InversionImportFilaCruda(2, "Fima Premium", "Valuacion del Fondo Fima", "", "20/03/2026", "100", "1000", "100000"),
                new InversionImportFilaCruda(3, "Fima Premium", "IVA 04.26", "Agregar", "20/03/2026", "100", "1000", "100000"));

        List<InversionImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).fila()).isEqualTo(3);
    }

    @Test
    void previsualizarAgregarMapeaASuscripcion() {
        filasAServir = List.of(new InversionImportFilaCruda(2, "Fima Premium", "IVA", "Agregar", "20/03/2026", "100", "1000", "100000"));

        List<InversionImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).tipo()).isEqualTo(TipoMovimientoInversion.SUSCRIPCION);
        assertThat(resultado.get(0).errores()).isEmpty();
    }

    @Test
    void previsualizarRetirarMapeaARescate() {
        filasAServir = List.of(new InversionImportFilaCruda(2, "Fima Premium", "IVA", "Retirar", "20/03/2026", "100", "1000", "100000"));

        List<InversionImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).tipo()).isEqualTo(TipoMovimientoInversion.RESCATE);
    }

    @Test
    void previsualizarOperacionDesconocidaGeneraError() {
        filasAServir = List.of(new InversionImportFilaCruda(2, "Fima Premium", "IVA", "Transferir", "20/03/2026", "100", "1000", "100000"));

        List<InversionImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).errores()).anyMatch(e -> e.contains("Operación desconocida"));
    }

    @Test
    void previsualizarFormatoNoSoportadoLanzaNegocioException() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "inversiones.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.previsualizar(archivo)).isInstanceOf(NegocioException.class);
    }

    @Test
    void confirmarReusaLaMismaInversionParaVariasFilas() {
        InversionImportFilaDto fila1 = new InversionImportFilaDto(2, "Fima Premium", "IVA", TipoMovimientoInversion.SUSCRIPCION,
                LocalDate.of(2026, 3, 20), BigDecimal.valueOf(100), BigDecimal.valueOf(1000), BigDecimal.valueOf(100000), List.of());
        InversionImportFilaDto fila2 = new InversionImportFilaDto(3, "Fima Premium", "IIBB", TipoMovimientoInversion.SUSCRIPCION,
                LocalDate.of(2026, 4, 20), BigDecimal.valueOf(50), BigDecimal.valueOf(1010), BigDecimal.valueOf(50500), List.of());

        when(inversionRepo.findByInstrumentoIgnoreCase("Fima Premium")).thenReturn(Optional.empty());
        Inversion creada = new Inversion();
        creada.setId(7L);
        when(inversionService.crear(any())).thenReturn(creada);
        when(movimientoInversionRepo.existsByInversion_IdAndFechaAndTipoAndCuotapartes(any(), any(), any(), any())).thenReturn(false);
        MovimientoInversion mov = new MovimientoInversion();
        mov.setId(1L);
        when(movimientoInversionService.crear(any())).thenReturn(mov);
        when(movimientoInversionService.aResponse(mov)).thenReturn(mockResponse());

        InversionImportResultado resultado = service.confirmar(9L, List.of(fila1, fila2));

        assertThat(resultado.creadas()).hasSize(2);
        verify(inversionService, org.mockito.Mockito.times(1)).crear(any());
    }

    @Test
    void confirmarNoDuplicaSiYaExiste() {
        InversionImportFilaDto fila = new InversionImportFilaDto(2, "Fima Premium", "IVA", TipoMovimientoInversion.SUSCRIPCION,
                LocalDate.of(2026, 3, 20), BigDecimal.valueOf(100), BigDecimal.valueOf(1000), BigDecimal.valueOf(100000), List.of());

        Inversion existente = new Inversion();
        existente.setId(7L);
        when(inversionRepo.findByInstrumentoIgnoreCase("Fima Premium")).thenReturn(Optional.of(existente));
        when(movimientoInversionRepo.existsByInversion_IdAndFechaAndTipoAndCuotapartes(
                7L, LocalDate.of(2026, 3, 20), TipoMovimientoInversion.SUSCRIPCION, BigDecimal.valueOf(100))).thenReturn(true);

        InversionImportResultado resultado = service.confirmar(9L, List.of(fila));

        assertThat(resultado.creadas()).isEmpty();
        assertThat(resultado.yaExistian()).isEqualTo(1);
        verify(movimientoInversionService, never()).crear(any());
    }

    private MovimientoInversionResponse mockResponse() {
        return new MovimientoInversionResponse(1L, 7L, TipoMovimientoInversion.SUSCRIPCION, LocalDate.of(2026, 3, 20),
                BigDecimal.valueOf(100000), BigDecimal.valueOf(100), BigDecimal.valueOf(1000), LocalDate.of(2026, 3, 20), null, null);
    }
}

package com.montanaritech.contable.maestros.proyecto.etapa.importacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.etapa.Etapa;
import com.montanaritech.contable.maestros.proyecto.etapa.EtapaMapper;
import com.montanaritech.contable.maestros.proyecto.etapa.EtapaService;
import com.montanaritech.contable.maestros.proyecto.etapa.dto.EtapaResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

class EtapaImportServiceTest {

    private ProveedorRepository proveedorRepo;
    private EtapaService etapaService;
    private EtapaMapper etapaMapper;
    private AuditoriaService auditoria;
    private EtapaImportService service;

    private List<EtapaImportFilaCruda> filasAServir;

    @BeforeEach
    void setUp() {
        proveedorRepo = mock(ProveedorRepository.class);
        etapaService = mock(EtapaService.class);
        etapaMapper = mock(EtapaMapper.class);
        auditoria = mock(AuditoriaService.class);

        EtapaImportParser parserFalso = new EtapaImportParser() {
            @Override
            public boolean soporta(String nombreArchivo) {
                return nombreArchivo.endsWith(".xlsx");
            }

            @Override
            public List<EtapaImportFilaCruda> parsear(InputStream in) {
                return filasAServir;
            }
        };

        service = new EtapaImportService(List.of(parserFalso), proveedorRepo, etapaService, etapaMapper, auditoria);
    }

    private MockMultipartFile archivoXlsx() {
        return new MockMultipartFile("archivo", "etapas.xlsx", "application/vnd.ms-excel", new byte[]{1, 2, 3});
    }

    @Test
    void previsualizarFilaValidaSinErrores() {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(1L);
        proveedor.setNombre("Acme");
        when(proveedorRepo.findByNombreIgnoreCase("Acme")).thenReturn(Optional.of(proveedor));

        filasAServir = List.of(new EtapaImportFilaCruda(
                2, "Etapa 1", "Descripción", "EN_CURSO", "01/03/2026", "30/04/2026", "50",
                "1.234,50", "500", "0", "0", "obs", "Acme"));

        List<EtapaImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado).hasSize(1);
        EtapaImportFilaDto fila = resultado.get(0);
        assertThat(fila.errores()).isEmpty();
        assertThat(fila.nombre()).isEqualTo("Etapa 1");
        assertThat(fila.estado()).isEqualTo("EN_CURSO");
        assertThat(fila.fechaInicio()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(fila.montoPresupuestado()).isEqualByComparingTo("1234.50");
        assertThat(fila.proveedoresIds()).containsExactly(1L);
    }

    @Test
    void previsualizarSinNombreGeneraError() {
        filasAServir = List.of(new EtapaImportFilaCruda(
                2, "", "", "", "", "", "", "", "", "", "", "", ""));

        List<EtapaImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).errores()).contains("El nombre es obligatorio");
    }

    @Test
    void previsualizarFechaInvalidaGeneraError() {
        filasAServir = List.of(new EtapaImportFilaCruda(
                2, "Etapa 1", "", "", "31/13/2026", "", "", "", "", "", "", "", ""));

        List<EtapaImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).errores()).anyMatch(e -> e.contains("fecha de inicio"));
        assertThat(resultado.get(0).fechaInicio()).isNull();
    }

    @Test
    void previsualizarProveedorNoEncontradoGeneraError() {
        when(proveedorRepo.findByNombreIgnoreCase("Fantasma")).thenReturn(Optional.empty());

        filasAServir = List.of(new EtapaImportFilaCruda(
                2, "Etapa 1", "", "", "", "", "", "", "", "", "", "", "Fantasma"));

        List<EtapaImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).errores()).contains("Proveedor 'Fantasma' no encontrado");
    }

    @Test
    void previsualizarFormatoNoSoportadoLanzaNegocioException() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "etapas.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.previsualizar(archivo)).isInstanceOf(NegocioException.class);
    }

    @Test
    void confirmarCreaFilasValidasYRechazaInvalidas() {
        EtapaImportFilaDto valida = new EtapaImportFilaDto(
                2, "Etapa válida", null, "PENDIENTE", null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of());
        EtapaImportFilaDto invalida = new EtapaImportFilaDto(
                3, "Etapa inválida", null, "PENDIENTE", null, null, null, BigDecimal.valueOf(-1), null, null, null, null,
                List.of(), List.of(), List.of());

        Etapa etapaCreada = new Etapa();
        etapaCreada.setId(10L);
        when(etapaService.crear(Mockito.eq(1L), any())).thenReturn(etapaCreada);
        when(etapaMapper.aResponse(etapaCreada)).thenReturn(mockResponse());

        EtapaImportResultado resultado = service.confirmar(1L, List.of(valida, invalida));

        assertThat(resultado.creadas()).hasSize(1);
        assertThat(resultado.rechazadas()).hasSize(1);
        assertThat(resultado.rechazadas().get(0).errores()).contains("Los importes no pueden ser negativos");
        verify(etapaService, Mockito.times(1)).crear(anyLong(), any());
        verify(etapaService, never()).crear(anyLong(), Mockito.argThat(r -> r != null && "Etapa inválida".equals(r.nombre())));
    }

    private EtapaResponse mockResponse() {
        return new EtapaResponse(10L, 1L, "x", null, "PENDIENTE", null, null, null, null, null, java.util.Set.of(), null, null, null, true);
    }
}

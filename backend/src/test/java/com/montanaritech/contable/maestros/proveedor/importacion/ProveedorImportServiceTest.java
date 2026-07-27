package com.montanaritech.contable.maestros.proveedor.importacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proveedor.ProveedorMapper;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proveedor.ProveedorService;
import com.montanaritech.contable.maestros.proveedor.dto.ProveedorResponse;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ProveedorImportServiceTest {

    private ProveedorRepository proveedorRepo;
    private ProveedorService proveedorService;
    private ProveedorMapper proveedorMapper;
    private ProveedorImportService service;

    private List<ProveedorImportFilaCruda> filasAServir;

    @BeforeEach
    void setUp() {
        proveedorRepo = mock(ProveedorRepository.class);
        proveedorService = mock(ProveedorService.class);
        proveedorMapper = mock(ProveedorMapper.class);

        ProveedorImportParser parserFalso = new ProveedorImportParser() {
            @Override
            public boolean soporta(String nombreArchivo) {
                return nombreArchivo.endsWith(".xlsx");
            }

            @Override
            public List<ProveedorImportFilaCruda> parsear(InputStream in) {
                return filasAServir;
            }
        };

        service = new ProveedorImportService(List.of(parserFalso), proveedorRepo, proveedorService, proveedorMapper);
    }

    private MockMultipartFile archivoXlsx() {
        return new MockMultipartFile("archivo", "proveedores.xlsx", "application/vnd.ms-excel", new byte[]{1, 2, 3});
    }

    @Test
    void previsualizarSinCuitGeneraError() {
        filasAServir = List.of(new ProveedorImportFilaCruda(2, "Basilotta Matias", ""));

        List<ProveedorImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("Basilotta Matias");
        assertThat(resultado.get(0).errores()).anyMatch(e -> e.contains("CUIT no informado"));
    }

    @Test
    void previsualizarConCuitSinErrores() {
        filasAServir = List.of(new ProveedorImportFilaCruda(2, "Basilotta Matias", "20-12345678-3"));

        List<ProveedorImportFilaDto> resultado = service.previsualizar(archivoXlsx());

        assertThat(resultado.get(0).errores()).isEmpty();
    }

    @Test
    void previsualizarFormatoNoSoportadoLanzaNegocioException() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "proveedores.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.previsualizar(archivo)).isInstanceOf(NegocioException.class);
    }

    @Test
    void confirmarRechazaFilasSinCuit() {
        ProveedorImportFilaDto sinCuit = new ProveedorImportFilaDto(2, "Basilotta Matias", null, List.of());

        ProveedorImportResultado resultado = service.confirmar(1L, List.of(sinCuit));

        assertThat(resultado.creadas()).isEmpty();
        assertThat(resultado.rechazadas()).hasSize(1);
        assertThat(resultado.rechazadas().get(0).errores()).anyMatch(e -> e.contains("CUIT no informado"));
        verify(proveedorService, never()).crear(any());
    }

    @Test
    void confirmarCreaFilaValidaConCuit() {
        ProveedorImportFilaDto valida = new ProveedorImportFilaDto(2, "Basilotta Matias", "20-12345678-3", List.of());
        when(proveedorRepo.findByNombreIgnoreCase("Basilotta Matias")).thenReturn(Optional.empty());
        Proveedor creado = new Proveedor();
        creado.setId(7L);
        when(proveedorService.crear(any())).thenReturn(creado);
        when(proveedorMapper.aResponse(creado)).thenReturn(mockResponse());

        ProveedorImportResultado resultado = service.confirmar(1L, List.of(valida));

        assertThat(resultado.creadas()).hasSize(1);
        assertThat(resultado.rechazadas()).isEmpty();
    }

    @Test
    void confirmarNoDuplicaSiYaExiste() {
        ProveedorImportFilaDto valida = new ProveedorImportFilaDto(2, "Basilotta Matias", "20-12345678-3", List.of());
        Proveedor existente = new Proveedor();
        existente.setId(3L);
        when(proveedorRepo.findByNombreIgnoreCase("Basilotta Matias")).thenReturn(Optional.of(existente));

        ProveedorImportResultado resultado = service.confirmar(1L, List.of(valida));

        assertThat(resultado.creadas()).isEmpty();
        assertThat(resultado.yaExistian()).isEqualTo(1);
        verify(proveedorService, never()).crear(any());
    }

    private ProveedorResponse mockResponse() {
        return new ProveedorResponse(7L, "Basilotta Matias", "20-12345678-3", 1L, "CABA", null, null,
                Set.of(), null, null, null, null, null, null, true);
    }
}

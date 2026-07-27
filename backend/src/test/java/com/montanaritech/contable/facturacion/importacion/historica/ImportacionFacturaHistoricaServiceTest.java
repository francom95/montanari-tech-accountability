package com.montanaritech.contable.facturacion.importacion.historica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.asiento.BuscarAsientoPorComprobante;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.facturacion.TipoComprobante;
import com.montanaritech.contable.facturacion.importacion.ImportacionFacturaService;
import com.montanaritech.contable.facturacion.importacion.dto.FilaImportacionConfirmarRequest;
import com.montanaritech.contable.facturacion.importacion.dto.FilaImportacionPreviewResponse;
import com.montanaritech.contable.facturacion.importacion.dto.FilaImportacionResultadoResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * F10.3: verifica la decisión de agujero vs. vinculación, y que el
 * matching/agujero se recalcula siempre server-side (nunca confía en lo
 * que mande el cliente).
 */
class ImportacionFacturaHistoricaServiceTest {

    private ImportacionFacturaService importacionFacturaService;
    private BuscarAsientoPorComprobante buscarAsientoPorComprobante;
    private ImportacionFacturaHistoricaService service;

    @BeforeEach
    void setUp() {
        importacionFacturaService = mock(ImportacionFacturaService.class);
        buscarAsientoPorComprobante = mock(BuscarAsientoPorComprobante.class);
        service = new ImportacionFacturaHistoricaService(importacionFacturaService, buscarAsientoPorComprobante);
    }

    private FilaImportacionPreviewResponse preview(LocalDate fecha) {
        return new FilaImportacionPreviewResponse("f.pdf", "VENTA", TipoComprobante.FACTURA_C, "00003", "00000105",
                fecha, "20111111112", null, null, null, null, "ARS", 1L, BigDecimal.ONE,
                new BigDecimal("1000"), BigDecimal.ZERO, new BigDecimal("1000"), "CAE", List.of(), "texto");
    }

    private FilaImportacionConfirmarRequest confirmarReq(LocalDate fecha, Long asientoIdExistenteEnviadoPorCliente) {
        return new FilaImportacionConfirmarRequest("f.pdf", "VENTA", 1L, null, null, null, null, null,
                fecha, null, TipoComprobante.FACTURA_C, "00003", "00000105", 1L, BigDecimal.ONE, null,
                "Importado", new BigDecimal("1000"), BigDecimal.ZERO, "VENTA", null, "CONFIRMADO",
                asientoIdExistenteEnviadoPorCliente);
    }

    // ---- previsualizar ----

    @Test
    void previsualizarEnElAgujeroNoBuscaCoincidencia() {
        when(importacionFacturaService.previsualizar("f.pdf", new byte[0])).thenReturn(preview(LocalDate.of(2025, 11, 15)));

        var resultado = service.previsualizar("f.pdf", new byte[0]);

        assertThat(resultado.enAgujero()).isTrue();
        assertThat(resultado.asientosCandidatosIds()).isEmpty();
        assertThat(resultado.advertenciaMatching()).isNull();
        verify(buscarAsientoPorComprobante, never()).buscar(any(), any());
    }

    @Test
    void previsualizarFueraDelAgujeroConUnaCoincidenciaNoTieneAdvertencia() {
        when(importacionFacturaService.previsualizar("f.pdf", new byte[0])).thenReturn(preview(LocalDate.of(2026, 6, 1)));
        Asiento a = new Asiento();
        a.setId(500L);
        when(buscarAsientoPorComprobante.buscar("00003", "00000105")).thenReturn(List.of(a));

        var resultado = service.previsualizar("f.pdf", new byte[0]);

        assertThat(resultado.enAgujero()).isFalse();
        assertThat(resultado.asientosCandidatosIds()).containsExactly(500L);
        assertThat(resultado.advertenciaMatching()).isNull();
    }

    @Test
    void previsualizarFueraDelAgujeroSinCoincidenciaAdvierte() {
        when(importacionFacturaService.previsualizar("f.pdf", new byte[0])).thenReturn(preview(LocalDate.of(2026, 6, 1)));
        when(buscarAsientoPorComprobante.buscar("00003", "00000105")).thenReturn(List.of());

        var resultado = service.previsualizar("f.pdf", new byte[0]);

        assertThat(resultado.asientosCandidatosIds()).isEmpty();
        assertThat(resultado.advertenciaMatching()).contains("Sin asiento correspondiente");
    }

    @Test
    void previsualizarFueraDelAgujeroConVariasCoincidenciasAdvierteAmbiguedad() {
        when(importacionFacturaService.previsualizar("f.pdf", new byte[0])).thenReturn(preview(LocalDate.of(2026, 6, 1)));
        Asiento a1 = new Asiento();
        a1.setId(1L);
        Asiento a2 = new Asiento();
        a2.setId(2L);
        when(buscarAsientoPorComprobante.buscar("00003", "00000105")).thenReturn(List.of(a1, a2));

        var resultado = service.previsualizar("f.pdf", new byte[0]);

        assertThat(resultado.asientosCandidatosIds()).containsExactly(1L, 2L);
        assertThat(resultado.advertenciaMatching()).contains("ambigüedad");
    }

    // ---- confirmar: el agujero/matching se recalcula, nunca se confía en el cliente ----

    @Test
    void confirmarEnElAgujeroFuerzaAsientoIdExistenteNuloIgnorandoLoQueMandeElCliente() {
        when(importacionFacturaService.confirmar(anyList())).thenReturn(List.of());

        service.confirmar(List.of(confirmarReq(LocalDate.of(2025, 11, 15), 999L)));

        ArgumentCaptor<List<FilaImportacionConfirmarRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(importacionFacturaService).confirmar(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).asientoIdExistente()).isNull();
    }

    @Test
    void confirmarFueraDelAgujeroConUnaCoincidenciaUsaEseAsientoIgnorandoLoQueMandeElCliente() {
        Asiento a = new Asiento();
        a.setId(500L);
        when(buscarAsientoPorComprobante.buscar("00003", "00000105")).thenReturn(List.of(a));
        when(importacionFacturaService.confirmar(anyList())).thenReturn(List.of());

        service.confirmar(List.of(confirmarReq(LocalDate.of(2026, 6, 1), 999L)));

        ArgumentCaptor<List<FilaImportacionConfirmarRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(importacionFacturaService).confirmar(captor.capture());
        assertThat(captor.getValue().get(0).asientoIdExistente()).isEqualTo(500L);
    }

    @Test
    void confirmarFueraDelAgujeroSinCoincidenciaRechazaSinLlamarAlMotorDeF46() {
        when(buscarAsientoPorComprobante.buscar("00003", "00000105")).thenReturn(List.of());
        when(importacionFacturaService.confirmar(anyList())).thenReturn(List.of());

        List<FilaImportacionResultadoResponse> resultados = service.confirmar(
                List.of(confirmarReq(LocalDate.of(2026, 6, 1), null)));

        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).exito()).isFalse();
        assertThat(resultados.get(0).motivoRechazo()).contains("Sin asiento correspondiente");
        verify(importacionFacturaService).confirmar(eq(List.of()));
    }

    @Test
    void confirmarFueraDelAgujeroConVariasCoincidenciasRechazaPorAmbiguedad() {
        Asiento a1 = new Asiento();
        a1.setId(1L);
        Asiento a2 = new Asiento();
        a2.setId(2L);
        when(buscarAsientoPorComprobante.buscar("00003", "00000105")).thenReturn(List.of(a1, a2));
        when(importacionFacturaService.confirmar(anyList())).thenReturn(List.of());

        List<FilaImportacionResultadoResponse> resultados = service.confirmar(
                List.of(confirmarReq(LocalDate.of(2026, 6, 1), null)));

        assertThat(resultados.get(0).exito()).isFalse();
        assertThat(resultados.get(0).motivoRechazo()).contains("ambigüedad");
    }
}

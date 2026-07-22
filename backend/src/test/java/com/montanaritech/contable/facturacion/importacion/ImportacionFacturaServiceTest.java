package com.montanaritech.contable.facturacion.importacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.facturacion.TipoComprobante;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompra;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompraRepository;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompraService;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaRepository;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaService;
import com.montanaritech.contable.facturacion.importacion.dto.FilaImportacionConfirmarRequest;
import com.montanaritech.contable.facturacion.importacion.dto.FilaImportacionResultadoResponse;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.cliente.ClienteRepository;
import com.montanaritech.contable.maestros.cliente.ClienteService;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proveedor.ProveedorService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Ciclo de confirmación del importador (F4.6): idempotencia, alta rápida, aislamiento por fila. */
@ExtendWith(MockitoExtension.class)
class ImportacionFacturaServiceTest {

    @Mock private ExtractorFacturaPdf extractor;
    @Mock private ClienteRepository clienteRepo;
    @Mock private ClienteService clienteService;
    @Mock private ProveedorRepository proveedorRepo;
    @Mock private ProveedorService proveedorService;
    @Mock private MonedaRepository monedaRepo;
    @Mock private FacturaVentaRepository facturaVentaRepo;
    @Mock private FacturaVentaService facturaVentaService;
    @Mock private FacturaCompraRepository facturaCompraRepo;
    @Mock private FacturaCompraService facturaCompraService;

    private ImportacionFacturaService service;

    @BeforeEach
    void setUp() {
        service = new ImportacionFacturaService(extractor, clienteRepo, clienteService, proveedorRepo, proveedorService,
                monedaRepo, facturaVentaRepo, facturaVentaService, facturaCompraRepo, facturaCompraService);
    }

    private FilaImportacionConfirmarRequest filaVenta(Long clienteId, String altaRapidaNombre, String altaRapidaCuit,
            String estadoDestino) {
        return new FilaImportacionConfirmarRequest("factura.pdf", "VENTA", clienteId, null,
                altaRapidaNombre, altaRapidaCuit, altaRapidaNombre != null ? 1L : null, null,
                LocalDate.of(2026, 7, 1), null, TipoComprobante.FACTURA_C, "00003", "00000105",
                1L, new BigDecimal("1.000000"), null, "Importado", new BigDecimal("1450000.00"),
                BigDecimal.ZERO, "VENTA", null, estadoDestino);
    }

    // ---- Venta: cliente existente ----

    @Test
    void confirmarVentaConClienteExistenteCreaBorrador() {
        FacturaVenta creada = new FacturaVenta();
        creada.setId(50L);
        when(facturaVentaRepo.existsByClienteIdAndTipoComprobanteAndPuntoVentaAndNumero(
                1L, TipoComprobante.FACTURA_C, "00003", "00000105")).thenReturn(false);
        when(facturaVentaService.crearBorrador(any())).thenReturn(creada);

        List<FilaImportacionResultadoResponse> resultados = service.confirmar(List.of(filaVenta(1L, null, null, "BORRADOR")));

        assertThat(resultados).hasSize(1);
        FilaImportacionResultadoResponse resultado = resultados.get(0);
        assertThat(resultado.exito()).isTrue();
        assertThat(resultado.facturaId()).isEqualTo(50L);
        assertThat(resultado.estadoFinal()).isEqualTo("BORRADOR");
        verify(facturaVentaService, never()).confirmar(any());
    }

    // ---- Venta: alta rápida de cliente nuevo ----

    @Test
    void confirmarVentaSinClienteHaceAltaRapida() {
        Cliente clienteNuevo = new Cliente();
        clienteNuevo.setId(99L);
        when(clienteService.crear(any())).thenReturn(clienteNuevo);
        when(facturaVentaRepo.existsByClienteIdAndTipoComprobanteAndPuntoVentaAndNumero(
                eq(99L), any(), any(), any())).thenReturn(false);
        FacturaVenta creada = new FacturaVenta();
        creada.setId(51L);
        when(facturaVentaService.crearBorrador(any())).thenReturn(creada);

        List<FilaImportacionResultadoResponse> resultados = service.confirmar(
                List.of(filaVenta(null, "Cliente Nuevo", "20-12345678-9", "BORRADOR")));

        assertThat(resultados.get(0).exito()).isTrue();
        verify(clienteService).crear(any());
    }

    // ---- Venta: ya importada ----

    @Test
    void confirmarVentaDuplicadaSeRechaza() {
        when(facturaVentaRepo.existsByClienteIdAndTipoComprobanteAndPuntoVentaAndNumero(
                1L, TipoComprobante.FACTURA_C, "00003", "00000105")).thenReturn(true);

        List<FilaImportacionResultadoResponse> resultados = service.confirmar(List.of(filaVenta(1L, null, null, "BORRADOR")));

        assertThat(resultados.get(0).exito()).isFalse();
        assertThat(resultados.get(0).motivoRechazo()).contains("Ya importada");
        verify(facturaVentaService, never()).crearBorrador(any());
    }

    // ---- Venta: pide CONFIRMADO y la confirmación automática falla -> queda como borrador con advertencia ----

    @Test
    void confirmarVentaConEstadoDestinoConfirmadoQueFallaQuedaComoBorrador() {
        FacturaVenta creada = new FacturaVenta();
        creada.setId(52L);
        lenient().when(facturaVentaRepo.existsByClienteIdAndTipoComprobanteAndPuntoVentaAndNumero(any(), any(), any(), any())).thenReturn(false);
        when(facturaVentaService.crearBorrador(any())).thenReturn(creada);
        when(facturaVentaService.confirmar(52L)).thenThrow(new RuntimeException("MAPEO_CUENTA_FALTANTE"));

        List<FilaImportacionResultadoResponse> resultados = service.confirmar(List.of(filaVenta(1L, null, null, "CONFIRMADO")));

        FilaImportacionResultadoResponse resultado = resultados.get(0);
        assertThat(resultado.exito()).isTrue();
        assertThat(resultado.estadoFinal()).isEqualTo("BORRADOR");
        assertThat(resultado.advertencia()).contains("MAPEO_CUENTA_FALTANTE");
    }

    // ---- Compra: sin tipoCostoId se rechaza ----

    @Test
    void confirmarCompraSinTipoCostoSeRechaza() {
        FilaImportacionConfirmarRequest fila = new FilaImportacionConfirmarRequest("compra.pdf", "COMPRA", null, 1L,
                null, null, null, null, LocalDate.of(2026, 6, 1), null, TipoComprobante.FACTURA_A, "0001", "00000001",
                1L, new BigDecimal("1.000000"), null, "Importado", new BigDecimal("1000.00"), BigDecimal.ZERO,
                null, null, "BORRADOR");
        lenient().when(facturaCompraRepo.existsByProveedorIdAndTipoComprobanteAndPuntoVentaAndNumero(any(), any(), any(), any()))
                .thenReturn(false);

        List<FilaImportacionResultadoResponse> resultados = service.confirmar(List.of(fila));

        assertThat(resultados.get(0).exito()).isFalse();
        assertThat(resultados.get(0).motivoRechazo()).contains("tipo de costo");
    }

    // ---- Aislamiento: una fila que revienta no impide procesar las demás del lote ----

    @Test
    void unaFilaConExcepcionNoAbortaElRestoDelLote() {
        FacturaVenta creada = new FacturaVenta();
        creada.setId(53L);
        FilaImportacionConfirmarRequest filaRota = filaVenta(1L, null, null, "BORRADOR");
        FilaImportacionConfirmarRequest filaOk = new FilaImportacionConfirmarRequest("ok.pdf", "VENTA", 2L, null,
                null, null, null, null, LocalDate.of(2026, 7, 2), null, TipoComprobante.FACTURA_C, "00003", "00000106",
                1L, new BigDecimal("1.000000"), null, "Importado", new BigDecimal("1000.00"), BigDecimal.ZERO,
                "VENTA", null, "BORRADOR");
        // Stub específico (clienteId=2, la fila OK) primero: si el stub genérico any()-throw se registrara
        // antes, esta misma llamada de configuración dispararía la excepción al armar el mock.
        when(facturaVentaRepo.existsByClienteIdAndTipoComprobanteAndPuntoVentaAndNumero(
                eq(2L), any(), any(), any())).thenReturn(false);
        when(facturaVentaRepo.existsByClienteIdAndTipoComprobanteAndPuntoVentaAndNumero(eq(1L), any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));
        when(facturaVentaService.crearBorrador(any())).thenReturn(creada);

        List<FilaImportacionResultadoResponse> resultados = service.confirmar(List.of(filaRota, filaOk));

        assertThat(resultados).hasSize(2);
        assertThat(resultados.get(0).exito()).isFalse();
        assertThat(resultados.get(0).motivoRechazo()).isEqualTo("boom");
        assertThat(resultados.get(1).exito()).isTrue();
    }
}

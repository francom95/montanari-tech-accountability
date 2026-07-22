package com.montanaritech.contable.bancos.importacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.bancos.importacion.dto.FilaImportacionConfirmarRequest;
import com.montanaritech.contable.bancos.importacion.dto.FilaImportacionPreviewResponse;
import com.montanaritech.contable.bancos.importacion.dto.FilaImportacionResultadoResponse;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancario;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancarioRepository;
import com.montanaritech.contable.bancos.movimientobancario.MovimientoBancarioService;
import com.montanaritech.contable.bancos.movimientobancario.OrigenImportacionMovimiento;
import com.montanaritech.contable.bancos.movimientobancario.dto.CrearMovimientoBancarioRequest;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Servicio de importación (F5.2): resolución de moneda faltante contra la
 * cuenta bancaria destino, detección de duplicados por hash, requerimiento
 * de TC para filas en moneda extranjera y resiliencia fila-a-fila (una fila
 * con error no aborta el resto del lote — mismo criterio que F4.6).
 */
@ExtendWith(MockitoExtension.class)
class ImportacionMovimientoBancarioServiceTest {

    @Mock private MovimientoBancarioRepository movimientoBancarioRepo;
    @Mock private MovimientoBancarioService movimientoBancarioService;
    @Mock private CuentaBancariaRepository cuentaBancariaRepo;
    @Mock private MonedaRepository monedaRepo;

    private CuentaBancaria cuentaBancariaArs;
    private Moneda ars;
    private Moneda usd;

    @BeforeEach
    void setUp() {
        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        usd = new Moneda();
        usd.setId(2L);
        usd.setCodigo("USD");

        cuentaBancariaArs = new CuentaBancaria();
        cuentaBancariaArs.setId(10L);
        cuentaBancariaArs.setMoneda(ars);

        lenient().when(cuentaBancariaRepo.findById(10L)).thenReturn(Optional.of(cuentaBancariaArs));
        lenient().when(monedaRepo.findByCodigo("ARS")).thenReturn(Optional.of(ars));
        lenient().when(monedaRepo.findByCodigo("USD")).thenReturn(Optional.of(usd));
    }

    private ImportacionMovimientoBancarioService servicioCon(ResumenParser... parsers) {
        return new ImportacionMovimientoBancarioService(movimientoBancarioRepo, movimientoBancarioService,
                cuentaBancariaRepo, monedaRepo, List.of(parsers));
    }

    private ResumenParser parserFalso(OrigenImportacionMovimiento origen, List<MovimientoParseado> filas) {
        return new ResumenParser() {
            @Override
            public OrigenImportacionMovimiento origen() {
                return origen;
            }

            @Override
            public List<MovimientoParseado> parsear(byte[] contenido) {
                return filas;
            }
        };
    }

    // ---- Previsualizar ----

    @Test
    void previsualizarUsaLaMonedaDeLaCuentaBancariaCuandoElParserNoLaDeclara() {
        MovimientoParseado fila = new MovimientoParseado(null, "Comision", new BigDecimal("-100.00"), null, null);
        var service = servicioCon(parserFalso(OrigenImportacionMovimiento.GALICIA, List.of(fila)));

        List<FilaImportacionPreviewResponse> preview = service.previsualizar(OrigenImportacionMovimiento.GALICIA, 10L, new byte[0]);

        assertThat(preview).hasSize(1);
        assertThat(preview.get(0).monedaCodigo()).isEqualTo("ARS");
        assertThat(preview.get(0).fecha()).isNull();
        assertThat(preview.get(0).duplicado()).isFalse();
    }

    @Test
    void previsualizarMarcaDuplicadoCuandoYaExisteElHashEnLaCuentaBancaria() {
        MovimientoParseado fila = new MovimientoParseado(LocalDate.of(2026, 6, 1), "Pago", new BigDecimal("-50.00"), "ARS", "REF-1");
        when(movimientoBancarioRepo.existsByCuentaBancaria_IdAndHashImportacion(eq(10L), any())).thenReturn(true);
        var service = servicioCon(parserFalso(OrigenImportacionMovimiento.GALICIA, List.of(fila)));

        List<FilaImportacionPreviewResponse> preview = service.previsualizar(OrigenImportacionMovimiento.GALICIA, 10L, new byte[0]);

        assertThat(preview.get(0).duplicado()).isTrue();
    }

    @Test
    void previsualizarConOrigenSinParserRegistradoLanzaError() {
        var service = servicioCon(); // sin parsers

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.previsualizar(OrigenImportacionMovimiento.MERCADO_PAGO, 10L, new byte[0]))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("ORIGEN_SIN_PARSER");
    }

    @Test
    void previsualizarConCuentaBancariaInexistenteLanzaError() {
        when(cuentaBancariaRepo.findById(999L)).thenReturn(Optional.empty());
        var service = servicioCon(parserFalso(OrigenImportacionMovimiento.GALICIA, List.of()));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.previsualizar(OrigenImportacionMovimiento.GALICIA, 999L, new byte[0]))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ---- Confirmar ----

    @Test
    void confirmarCreaElMovimientoYDevuelveImportado() {
        MovimientoBancario creado = new MovimientoBancario();
        creado.setId(555L);
        when(movimientoBancarioService.crear(any())).thenReturn(creado);
        var service = servicioCon();

        FilaImportacionConfirmarRequest fila = new FilaImportacionConfirmarRequest(
                LocalDate.of(2026, 6, 1), "Comision", new BigDecimal("-100.00"), null, "REF-1", "hash-1");

        List<FilaImportacionResultadoResponse> resultado = service.confirmar(
                OrigenImportacionMovimiento.GALICIA, 10L, null, List.of(fila));

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).resultado()).isEqualTo("IMPORTADO");
        assertThat(resultado.get(0).movimientoBancarioId()).isEqualTo(555L);

        var captor = org.mockito.ArgumentCaptor.forClass(CrearMovimientoBancarioRequest.class);
        verify(movimientoBancarioService).crear(captor.capture());
        assertThat(captor.getValue().origenImportacion()).isEqualTo(OrigenImportacionMovimiento.GALICIA);
        assertThat(captor.getValue().hashImportacion()).isEqualTo("hash-1");
        assertThat(captor.getValue().monedaId()).isEqualTo(1L); // ARS resuelta de la cuenta bancaria
        assertThat(captor.getValue().tipoCambio()).isEqualByComparingTo("1");
    }

    @Test
    void confirmarSaltaLasFilasYaImportadasSinLlamarACrear() {
        when(movimientoBancarioRepo.existsByCuentaBancaria_IdAndHashImportacion(10L, "hash-dup")).thenReturn(true);
        var service = servicioCon();

        FilaImportacionConfirmarRequest fila = new FilaImportacionConfirmarRequest(
                LocalDate.of(2026, 6, 1), "Comision", new BigDecimal("-100.00"), null, "REF-1", "hash-dup");

        List<FilaImportacionResultadoResponse> resultado = service.confirmar(
                OrigenImportacionMovimiento.GALICIA, 10L, null, List.of(fila));

        assertThat(resultado.get(0).resultado()).isEqualTo("DUPLICADO");
        verify(movimientoBancarioService, never()).crear(any());
    }

    @Test
    void confirmarUnaFilaEnMonedaExtranjeraSinTipoDeCambioDaError() {
        var service = servicioCon();

        FilaImportacionConfirmarRequest fila = new FilaImportacionConfirmarRequest(
                LocalDate.of(2026, 6, 1), "Consumo USD", new BigDecimal("-20.00"), "USD", "REF-2", "hash-2");

        List<FilaImportacionResultadoResponse> resultado = service.confirmar(
                OrigenImportacionMovimiento.TARJETA_CREDITO, 10L, null, List.of(fila));

        assertThat(resultado.get(0).resultado()).isEqualTo("ERROR");
        verify(movimientoBancarioService, never()).crear(any());
    }

    @Test
    void confirmarUnaFilaEnMonedaExtranjeraConTipoDeCambioLaImporta() {
        MovimientoBancario creado = new MovimientoBancario();
        creado.setId(777L);
        when(movimientoBancarioService.crear(any())).thenReturn(creado);
        var service = servicioCon();

        FilaImportacionConfirmarRequest fila = new FilaImportacionConfirmarRequest(
                LocalDate.of(2026, 6, 1), "Consumo USD", new BigDecimal("-20.00"), "USD", "REF-2", "hash-3");

        List<FilaImportacionResultadoResponse> resultado = service.confirmar(
                OrigenImportacionMovimiento.TARJETA_CREDITO, 10L, new BigDecimal("1200.00"), List.of(fila));

        assertThat(resultado.get(0).resultado()).isEqualTo("IMPORTADO");
        var captor = org.mockito.ArgumentCaptor.forClass(CrearMovimientoBancarioRequest.class);
        verify(movimientoBancarioService).crear(captor.capture());
        assertThat(captor.getValue().monedaId()).isEqualTo(2L);
        assertThat(captor.getValue().tipoCambio()).isEqualByComparingTo("1200.00");
    }

    @Test
    void confirmarConFechaNulaNoFallaYPropagaFechaNulaAlCrear() {
        MovimientoBancario creado = new MovimientoBancario();
        creado.setId(888L);
        when(movimientoBancarioService.crear(any())).thenReturn(creado);
        var service = servicioCon();

        FilaImportacionConfirmarRequest fila = new FilaImportacionConfirmarRequest(
                null, "Movimiento sin fecha (Galicia ARS)", new BigDecimal("-1000.00"), null, null, "hash-4");

        service.confirmar(OrigenImportacionMovimiento.GALICIA, 10L, null, List.of(fila));

        var captor = org.mockito.ArgumentCaptor.forClass(CrearMovimientoBancarioRequest.class);
        verify(movimientoBancarioService).crear(captor.capture());
        assertThat(captor.getValue().fecha()).isNull();
    }

    @Test
    void confirmarUnLoteConUnaFilaConErrorNoAbortaElResto() {
        MovimientoBancario creado = new MovimientoBancario();
        creado.setId(1L);
        when(movimientoBancarioService.crear(any()))
                .thenThrow(new NegocioException("IMPORTE_CERO", "El importe no puede ser cero"))
                .thenReturn(creado);
        var service = servicioCon();

        FilaImportacionConfirmarRequest filaConError = new FilaImportacionConfirmarRequest(
                LocalDate.of(2026, 6, 1), "Fila con error", BigDecimal.ZERO, null, null, "hash-a");
        FilaImportacionConfirmarRequest filaOk = new FilaImportacionConfirmarRequest(
                LocalDate.of(2026, 6, 2), "Fila ok", new BigDecimal("-10.00"), null, null, "hash-b");

        List<FilaImportacionResultadoResponse> resultado = service.confirmar(
                OrigenImportacionMovimiento.GALICIA, 10L, null, List.of(filaConError, filaOk));

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).resultado()).isEqualTo("ERROR");
        assertThat(resultado.get(0).motivoError()).isEqualTo("El importe no puede ser cero");
        assertThat(resultado.get(1).resultado()).isEqualTo("IMPORTADO");
    }
}

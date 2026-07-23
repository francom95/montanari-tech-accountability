package com.montanaritech.contable.bancos.tarjetacredito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.bancos.importacion.MovimientoParseado;
import com.montanaritech.contable.bancos.importacion.ParserTarjeta;
import com.montanaritech.contable.bancos.tarjetacredito.dto.ConsumoImportacionConfirmarRequest;
import com.montanaritech.contable.bancos.tarjetacredito.dto.ConsumoImportacionPreviewResponse;
import com.montanaritech.contable.common.saldo.RecalculoSaldoService;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCredito;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCreditoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Importación del resumen de tarjeta (F5.4): reusa ParserTarjeta (F5.2), produce ConsumoTarjeta. */
@ExtendWith(MockitoExtension.class)
class ImportacionConsumoTarjetaServiceTest {

    @Mock private ParserTarjeta parser;
    @Mock private TarjetaCreditoRepository tarjetaCreditoRepository;
    @Mock private ConsumoTarjetaRepository consumoTarjetaRepository;
    @Mock private MonedaRepository monedaRepository;
    @Mock private RecalculoSaldoService recalculoSaldoService;

    private ImportacionConsumoTarjetaService service;
    private TarjetaCredito tarjeta;
    private Moneda ars;
    private Moneda usd;

    @BeforeEach
    void setUp() {
        service = new ImportacionConsumoTarjetaService(parser, tarjetaCreditoRepository, consumoTarjetaRepository,
                monedaRepository, recalculoSaldoService);

        tarjeta = new TarjetaCredito();
        tarjeta.setId(5L);

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        usd = new Moneda();
        usd.setId(2L);
        usd.setCodigo("USD");

        lenient().when(tarjetaCreditoRepository.findById(5L)).thenReturn(Optional.of(tarjeta));
        lenient().when(monedaRepository.findByCodigo("ARS")).thenReturn(Optional.of(ars));
        lenient().when(monedaRepository.findByCodigo("USD")).thenReturn(Optional.of(usd));
        lenient().when(consumoTarjetaRepository.save(any())).thenAnswer(inv -> {
            ConsumoTarjeta c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });
    }

    @Test
    void previsualizarMarcaDuplicadoCuandoYaExisteElHashEnLaTarjeta() {
        MovimientoParseado fila = new MovimientoParseado(LocalDate.of(2026, 6, 1), "DONWEB", new BigDecimal("-25000.00"), "ARS", "005035");
        when(parser.parsear(any())).thenReturn(List.of(fila));
        when(consumoTarjetaRepository.existsByTarjetaCredito_IdAndHashImportacion(eq(5L), any())).thenReturn(true);

        List<ConsumoImportacionPreviewResponse> preview = service.previsualizar(5L, new byte[0]);

        assertThat(preview).hasSize(1);
        assertThat(preview.get(0).duplicado()).isTrue();
    }

    @Test
    void confirmarCreaElConsumoEnArsSinNecesitarTipoDeCambio() {
        ConsumoImportacionConfirmarRequest fila = new ConsumoImportacionConfirmarRequest(
                LocalDate.of(2026, 6, 1), "DONWEB", new BigDecimal("-25000.00"), "ARS", "005035", "hash-1");

        var resultado = service.confirmar(5L, null, List.of(fila));

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).resultado()).isEqualTo("IMPORTADO");
        assertThat(resultado.get(0).consumoTarjetaId()).isEqualTo(100L);
        verify(recalculoSaldoService).recalcular(tarjeta);
    }

    @Test
    void confirmarUnaFilaEnUsdSinTipoDeCambioDaError() {
        ConsumoImportacionConfirmarRequest fila = new ConsumoImportacionConfirmarRequest(
                LocalDate.of(2026, 5, 31), "TWILIO SENDGRID", new BigDecimal("-0.64"), "USD", "740226", "hash-2");

        var resultado = service.confirmar(5L, null, List.of(fila));

        assertThat(resultado.get(0).resultado()).isEqualTo("ERROR");
        assertThat(resultado.get(0).motivoError()).contains("tipo de cambio");
        verify(consumoTarjetaRepository, never()).save(any());
    }

    @Test
    void confirmarUnaFilaEnUsdConTipoDeCambioCalculaElImporteArs() {
        ConsumoImportacionConfirmarRequest fila = new ConsumoImportacionConfirmarRequest(
                LocalDate.of(2026, 5, 31), "TWILIO SENDGRID", new BigDecimal("-0.64"), "USD", "740226", "hash-3");

        var resultado = service.confirmar(5L, new BigDecimal("1200.00"), List.of(fila));

        assertThat(resultado.get(0).resultado()).isEqualTo("IMPORTADO");
        var captor = org.mockito.ArgumentCaptor.forClass(ConsumoTarjeta.class);
        verify(consumoTarjetaRepository).save(captor.capture());
        assertThat(captor.getValue().getImporteArs()).isEqualByComparingTo("-768.00");
        assertThat(captor.getValue().getMoneda()).isEqualTo(usd);
    }

    @Test
    void confirmarSaltaFilasYaImportadasSinCrearNiRecalcular() {
        when(consumoTarjetaRepository.existsByTarjetaCredito_IdAndHashImportacion(5L, "hash-dup")).thenReturn(true);
        ConsumoImportacionConfirmarRequest fila = new ConsumoImportacionConfirmarRequest(
                LocalDate.of(2026, 6, 1), "DONWEB", new BigDecimal("-25000.00"), "ARS", "005035", "hash-dup");

        var resultado = service.confirmar(5L, null, List.of(fila));

        assertThat(resultado.get(0).resultado()).isEqualTo("DUPLICADO");
        verify(consumoTarjetaRepository, never()).save(any());
    }
}

package com.montanaritech.contable.contabilidad.mayor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoLinea;
import com.montanaritech.contable.contabilidad.asiento.AsientoLineaRepository;
import com.montanaritech.contable.contabilidad.asiento.OrigenAsiento;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.contabilidad.mayor.MayorService.MayorCompleto;
import com.montanaritech.contable.contabilidad.mayor.dto.MayorFilaResponse;
import com.montanaritech.contable.contabilidad.mayor.dto.MayorResponse;
import com.montanaritech.contable.maestros.moneda.Moneda;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cubre F3.1 §10 CP-17 (mayor con saldo anterior/acumulado), CP-18 (base de
 * las sumas y saldos: acá solo el cálculo por cuenta), CP-20 (saldo
 * contrario al esperado advierte, nunca bloquea), más la agregación de
 * madre sobre descendientes (§5.3) y la vista analítica (§5.4).
 */
@ExtendWith(MockitoExtension.class)
class MayorServiceTest {

    @Mock private AsientoLineaRepository lineaRepo;
    @Mock private CuentaContableRepository cuentaRepo;

    private MayorService service;
    private CuentaContable bancoImputable;
    private Moneda ars;

    @BeforeEach
    void setUp() {
        service = new MayorService(lineaRepo, cuentaRepo);

        bancoImputable = new CuentaContable();
        bancoImputable.setId(10L);
        bancoImputable.setCodigo("1.1.2001");
        bancoImputable.setNombre("Banco Galicia CC");
        bancoImputable.setImputable(true);
        bancoImputable.setSaldoEsperado(CuentaContable.SaldoEsperado.DEUDOR);

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        lenient().when(cuentaRepo.findById(10L)).thenReturn(Optional.of(bancoImputable));
    }

    private AsientoLinea linea(Long asientoId, LocalDate fecha, Long numero, BigDecimal debe, BigDecimal haber, Integer orden) {
        Asiento a = new Asiento();
        a.setId(asientoId);
        a.setFecha(fecha);
        a.setNumero(numero);
        a.setDescripcion("Movimiento " + asientoId);
        a.setOrigen(OrigenAsiento.MANUAL);

        AsientoLinea l = new AsientoLinea();
        l.setAsiento(a);
        l.setOrden(orden);
        l.setCuentaContable(bancoImputable);
        l.setDebe(debe);
        l.setHaber(haber);
        l.setMoneda(ars);
        l.setTipoCambio(new BigDecimal("1.000000"));
        l.setImporteOriginal(debe.compareTo(BigDecimal.ZERO) != 0 ? debe : haber);
        return l;
    }

    // ---- CP-17: mayor sin filtro de fecha ----

    @Test
    void cp17_mayorSinFiltroAcumulaDesdeCeroYSaldoFinalDeudor() {
        List<AsientoLinea> lineas = List.of(
                linea(1L, LocalDate.of(2026, 6, 1), 1L, new BigDecimal("500000.00"), BigDecimal.ZERO, 1),
                linea(2L, LocalDate.of(2026, 6, 5), 2L, new BigDecimal("121000.00"), BigDecimal.ZERO, 1),
                linea(3L, LocalDate.of(2026, 6, 12), 3L, BigDecimal.ZERO, new BigDecimal("80000.00"), 1),
                linea(4L, LocalDate.of(2026, 6, 18), 4L, BigDecimal.ZERO, new BigDecimal("200000.00"), 1));
        when(lineaRepo.buscarParaMayor(eq(Set.of(10L)), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), eq(EstadoDocumento.CONFIRMADO))).thenReturn(lineas);

        MayorCompleto completo = service.calcular(10L, null, null, null, null, null, null, null, null);

        assertThat(completo.filas()).hasSize(4);
        assertThat(completo.filas().get(0).esSaldoAnterior()).isFalse();
        assertThat(completo.filas()).extracting(MayorFilaResponse::saldoAcumulado)
                .containsExactly(new BigDecimal("500000.00"), new BigDecimal("621000.00"),
                        new BigDecimal("541000.00"), new BigDecimal("341000.00"));
        assertThat(completo.saldoFinal()).isEqualByComparingTo("341000.00");
        assertThat(completo.saldoFinalEtiqueta()).isEqualTo("DEUDOR");
    }

    @Test
    void cp17_mayorFiltradoPorFechaAgregaFilaDeSaldoAnterior() {
        when(lineaRepo.sumarDebeAntesDeFecha(eq(Set.of(10L)), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(LocalDate.of(2026, 6, 10)), eq(EstadoDocumento.CONFIRMADO)))
                .thenReturn(new BigDecimal("621000.00"));
        when(lineaRepo.sumarHaberAntesDeFecha(eq(Set.of(10L)), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(LocalDate.of(2026, 6, 10)), eq(EstadoDocumento.CONFIRMADO)))
                .thenReturn(BigDecimal.ZERO);
        List<AsientoLinea> lineasDesdeFecha = List.of(
                linea(3L, LocalDate.of(2026, 6, 12), 3L, BigDecimal.ZERO, new BigDecimal("80000.00"), 1),
                linea(4L, LocalDate.of(2026, 6, 18), 4L, BigDecimal.ZERO, new BigDecimal("200000.00"), 1));
        when(lineaRepo.buscarParaMayor(eq(Set.of(10L)), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(LocalDate.of(2026, 6, 10)), isNull(), eq(EstadoDocumento.CONFIRMADO))).thenReturn(lineasDesdeFecha);

        MayorCompleto completo = service.calcular(10L, null, null, null, null, null, null, LocalDate.of(2026, 6, 10), null);

        assertThat(completo.filas()).hasSize(3);
        assertThat(completo.filas().get(0).esSaldoAnterior()).isTrue();
        assertThat(completo.filas().get(0).saldoAcumulado()).isEqualByComparingTo("621000.00");
        assertThat(completo.filas().get(1).saldoAcumulado()).isEqualByComparingTo("541000.00");
        assertThat(completo.filas().get(2).saldoAcumulado()).isEqualByComparingTo("341000.00");
        assertThat(completo.saldoFinal()).isEqualByComparingTo("341000.00");
    }

    // ---- CP-20: saldo contrario al esperado advierte, no bloquea ----

    @Test
    void cp20_saldoContrarioAlEsperadoQuedaMarcadoPeroNoLanzaError() {
        List<AsientoLinea> lineas = List.of(
                linea(5L, LocalDate.of(2026, 6, 20), 5L, BigDecimal.ZERO, new BigDecimal("400000.00"), 1));
        when(lineaRepo.sumarDebeAntesDeFecha(eq(Set.of(10L)), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(LocalDate.of(2026, 6, 19)), eq(EstadoDocumento.CONFIRMADO)))
                .thenReturn(new BigDecimal("341000.00"));
        when(lineaRepo.sumarHaberAntesDeFecha(eq(Set.of(10L)), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(LocalDate.of(2026, 6, 19)), eq(EstadoDocumento.CONFIRMADO)))
                .thenReturn(BigDecimal.ZERO);
        when(lineaRepo.buscarParaMayor(eq(Set.of(10L)), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(LocalDate.of(2026, 6, 19)), isNull(), eq(EstadoDocumento.CONFIRMADO))).thenReturn(lineas);

        MayorCompleto completo = service.calcular(10L, null, null, null, null, null, null, LocalDate.of(2026, 6, 19), null);

        assertThat(completo.saldoFinal()).isEqualByComparingTo("-59000.00");
        assertThat(completo.saldoFinalEtiqueta()).isEqualTo("ACREEDOR");
        assertThat(completo.contrarioAlEsperado()).isTrue();
    }

    // ---- Vista analítica (F3.1 §5.4) ----

    @Test
    void filtroPorProyectoActivaVistaAnaliticaYSuprimeContrarioAlEsperado() {
        when(lineaRepo.buscarParaMayor(eq(Set.of(10L)), isNull(), eq(7L), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), eq(EstadoDocumento.CONFIRMADO))).thenReturn(List.of());

        MayorCompleto completo = service.calcular(10L, null, 7L, null, null, null, null, null, null);

        assertThat(completo.vistaAnalitica()).isTrue();
        assertThat(completo.contrarioAlEsperado()).isNull();
    }

    // ---- Cuenta madre: agrega descendientes imputables (F3.1 §5.3) ----

    @Test
    void mayorDeUnaCuentaMadreAgregaTodasLasImputablesDescendientes() {
        CuentaContable madre = new CuentaContable();
        madre.setId(1L);
        madre.setCodigo("1");
        madre.setNombre("ACTIVO");
        madre.setImputable(false);
        madre.setSaldoEsperado(CuentaContable.SaldoEsperado.DEUDOR);

        CuentaContable madreIntermedia = new CuentaContable();
        madreIntermedia.setId(2L);
        madreIntermedia.setCodigo("1.1");
        madreIntermedia.setImputable(false);

        CuentaContable imputableDirecta = new CuentaContable();
        imputableDirecta.setId(3L);
        imputableDirecta.setImputable(true);

        CuentaContable imputableNieta = new CuentaContable();
        imputableNieta.setId(4L);
        imputableNieta.setImputable(true);

        when(cuentaRepo.findById(1L)).thenReturn(Optional.of(madre));
        when(cuentaRepo.findByPadreId(1L)).thenReturn(List.of(madreIntermedia, imputableDirecta));
        when(cuentaRepo.findByPadreId(2L)).thenReturn(List.of(imputableNieta));
        when(lineaRepo.buscarParaMayor(eq(Set.of(3L, 4L)), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), eq(EstadoDocumento.CONFIRMADO))).thenReturn(List.of());

        MayorCompleto completo = service.calcular(1L, null, null, null, null, null, null, null, null);

        assertThat(completo.filas()).isEmpty();
        assertThat(completo.saldoFinal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void mayorDeUnaMadreSinDescendientesImputablesQuedaVacioSinConsultarLineas() {
        CuentaContable madreVacia = new CuentaContable();
        madreVacia.setId(6L);
        madreVacia.setCodigo("6");
        madreVacia.setImputable(false);
        when(cuentaRepo.findById(6L)).thenReturn(Optional.of(madreVacia));
        when(cuentaRepo.findByPadreId(6L)).thenReturn(List.of());

        MayorCompleto completo = service.calcular(6L, null, null, null, null, null, null, null, null);

        assertThat(completo.filas()).isEmpty();
        assertThat(completo.saldoFinal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(completo.contrarioAlEsperado()).isNull();
    }

    // ---- Paginación (pantalla) ----

    @Test
    void paginarRecortaLasFilasYCalculaTotalPaginas() {
        List<MayorFilaResponse> cincoFilas = List.of(
                fila(1), fila(2), fila(3), fila(4), fila(5));
        MayorCompleto completo = new MayorCompleto(bancoImputable, false, cincoFilas, BigDecimal.TEN, "DEUDOR", false);

        MayorResponse pagina1 = service.paginar(completo, 1, 2);

        assertThat(pagina1.filas()).hasSize(2);
        assertThat(pagina1.filas().get(0)).isEqualTo(fila(3));
        assertThat(pagina1.totalFilas()).isEqualTo(5);
        assertThat(pagina1.totalPaginas()).isEqualTo(3);
        assertThat(pagina1.cuentaContableId()).isEqualTo(10L);
    }

    private MayorFilaResponse fila(int n) {
        return new MayorFilaResponse(false, LocalDate.of(2026, 6, n), (long) n, (long) n, "Movimiento " + n,
                10L, "1.1.2001", "Banco Galicia CC", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.valueOf(n),
                1L, "ARS", BigDecimal.ONE, BigDecimal.ONE, "MANUAL");
    }
}

package com.montanaritech.contable.contabilidad.asiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.asiento.NumeradorAsiento;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoCrearRequest;
import com.montanaritech.contable.contabilidad.asiento.dto.AsientoLineaRequest;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.maestros.cliente.ClienteRepository;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.proyecto.etapa.EtapaRepository;
import com.montanaritech.contable.maestros.tipocambio.TipoCambio;
import com.montanaritech.contable.maestros.tipocambio.TipoCambioRepository;
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
 * Cubre los casos de prueba de F3.1 §10 que aplican a la carga manual
 * (F3.4): CP-01, CP-02, CP-03(a,b,c), CP-05, CP-19. CP-04 (numeración sin
 * huecos) se prueba en {@link com.montanaritech.contable.common.asiento.NumeradorAsientoPersistenteTest}
 * porque esa garantía vive en la secuencia, no en este service. El resto de
 * los casos de F3.1 §10 (automáticos, anulación, duplicación, mayores) son
 * de F3.5/F3.6/F4.x — fuera de alcance de este paso.
 */
@ExtendWith(MockitoExtension.class)
class AsientoServiceTest {

    @Mock private AsientoRepository repo;
    @Mock private AsientoMapper mapper;
    @Mock private AuditoriaService auditoria;
    @Mock private NumeradorAsiento numerador;
    @Mock private CuentaContableRepository cuentaRepo;
    @Mock private MonedaRepository monedaRepo;
    @Mock private ProyectoRepository proyectoRepo;
    @Mock private EtapaRepository etapaRepo;
    @Mock private ClienteRepository clienteRepo;
    @Mock private ProveedorRepository proveedorRepo;
    @Mock private CuentaBancariaRepository cuentaBancariaRepo;
    @Mock private TipoCambioRepository tipoCambioRepo;

    private AsientoService service;

    private Moneda ars;
    private Moneda usd;
    private CuentaContable bancoImputable;
    private CuentaContable ventasImputable;
    private CuentaContable cajaYBancosMadre;

    @BeforeEach
    void setUp() {
        service = new AsientoService(repo, mapper, auditoria, numerador, cuentaRepo, monedaRepo,
                proyectoRepo, etapaRepo, clienteRepo, proveedorRepo, cuentaBancariaRepo, tipoCambioRepo);

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        usd = new Moneda();
        usd.setId(2L);
        usd.setCodigo("USD");

        bancoImputable = new CuentaContable();
        bancoImputable.setId(10L);
        bancoImputable.setCodigo("1.1.2001");
        bancoImputable.setNombre("Banco Galicia CC");
        bancoImputable.setImputable(true);
        bancoImputable.setActivo(true);

        ventasImputable = new CuentaContable();
        ventasImputable.setId(11L);
        ventasImputable.setCodigo("4.1.2001");
        ventasImputable.setNombre("Ingresos por ventas");
        ventasImputable.setImputable(true);
        ventasImputable.setActivo(true);

        cajaYBancosMadre = new CuentaContable();
        cajaYBancosMadre.setId(12L);
        cajaYBancosMadre.setCodigo("1.1");
        cajaYBancosMadre.setNombre("Caja y Bancos");
        cajaYBancosMadre.setImputable(false);
        cajaYBancosMadre.setActivo(true);

        // lenient: no todos los tests usan las 12 cuentas/monedas de este setup común.
        lenient().when(cuentaRepo.findById(10L)).thenReturn(Optional.of(bancoImputable));
        lenient().when(cuentaRepo.findById(11L)).thenReturn(Optional.of(ventasImputable));
        lenient().when(cuentaRepo.findById(12L)).thenReturn(Optional.of(cajaYBancosMadre));
        lenient().when(monedaRepo.findById(1L)).thenReturn(Optional.of(ars));
        lenient().when(monedaRepo.findById(2L)).thenReturn(Optional.of(usd));
        lenient().when(repo.save(any(Asiento.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private AsientoLineaRequest lineaArs(Long cuentaId, BigDecimal debe, BigDecimal haber) {
        return new AsientoLineaRequest(cuentaId, debe, haber, 1L, null, null, null, null, null, null, null, null);
    }

    /** Simula la persistencia: crea el borrador, le fija un id y lo deja disponible para {@code obtener}. */
    private Asiento asientoBorradorConLineas(List<AsientoLineaRequest> lineas, LocalDate fecha) {
        Asiento a = service.crearBorrador(new AsientoCrearRequest(fecha, "Asiento de prueba", null, lineas));
        a.setId(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(a));
        return a;
    }

    // ---- crearBorrador: sin validación contable (F3.1 §3.5) ----

    @Test
    void crearBorradorDesbalanceadoNoLanzaError() {
        Asiento a = service.crearBorrador(new AsientoCrearRequest(
                LocalDate.of(2026, 6, 5), "Borrador desbalanceado", null,
                List.of(lineaArs(10L, new BigDecimal("50000.00"), BigDecimal.ZERO),
                        lineaArs(11L, BigDecimal.ZERO, new BigDecimal("45000.00")))));

        assertThat(a.getEstado()).isEqualTo(EstadoDocumento.BORRADOR);
        assertThat(a.getNumero()).isNull();
        assertThat(a.getLineas()).hasSize(2);
    }

    // ---- CP-01: asiento manual balanceado ARS ----

    @Test
    void cp01_confirmarAsientoBalanceadoArsAsignaNumeroYNormalizaTc() {
        Asiento a = asientoBorradorConLineas(List.of(
                lineaArs(10L, new BigDecimal("50000.00"), BigDecimal.ZERO),
                lineaArs(11L, BigDecimal.ZERO, new BigDecimal("50000.00"))), LocalDate.of(2026, 6, 5));
        when(numerador.siguienteNumero()).thenReturn(7L);

        Asiento confirmado = service.confirmar(a.getId());

        assertThat(confirmado.getEstado()).isEqualTo(EstadoDocumento.CONFIRMADO);
        assertThat(confirmado.getNumero()).isEqualTo(7L);
        assertThat(confirmado.getLineas()).allSatisfy(l -> {
            assertThat(l.getTipoCambio()).isEqualByComparingTo("1.000000");
            assertThat(l.getFuenteTc()).isNull();
        });
    }

    // ---- CP-02: desbalanceado se rechaza al confirmar ----

    @Test
    void cp02_confirmarDesbalanceadoLanzaAsientoNoBalancea() {
        Asiento a = asientoBorradorConLineas(List.of(
                lineaArs(10L, new BigDecimal("50000.00"), BigDecimal.ZERO),
                lineaArs(11L, BigDecimal.ZERO, new BigDecimal("45000.00"))), LocalDate.of(2026, 6, 5));

        assertThatThrownBy(() -> service.confirmar(a.getId()))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("ASIENTO_NO_BALANCEA");
    }

    // ---- CP-03(a): línea sobre cuenta madre ----

    @Test
    void cp03a_lineaSobreCuentaMadreLanzaCuentaNoImputable() {
        Asiento a = asientoBorradorConLineas(List.of(
                lineaArs(12L, new BigDecimal("100.00"), BigDecimal.ZERO),
                lineaArs(11L, BigDecimal.ZERO, new BigDecimal("100.00"))), LocalDate.of(2026, 6, 5));

        assertThatThrownBy(() -> service.confirmar(a.getId()))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("CUENTA_NO_IMPUTABLE");
    }

    // ---- CP-03(b): línea con debe y haber a la vez ----

    @Test
    void cp03b_lineaConDebeYHaberLanzaLineaDebeXorHaber() {
        Asiento a = asientoBorradorConLineas(List.of(
                lineaArs(10L, new BigDecimal("100.00"), new BigDecimal("100.00")),
                lineaArs(11L, BigDecimal.ZERO, new BigDecimal("100.00"))), LocalDate.of(2026, 6, 5));

        assertThatThrownBy(() -> service.confirmar(a.getId()))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("LINEA_DEBE_XOR_HABER");
    }

    // ---- CP-03(c): asiento de una sola línea / sin líneas ----

    @Test
    void cp03c_asientoDeUnaSolaLineaLanzaAsientoIncompleto() {
        Asiento a = asientoBorradorConLineas(List.of(lineaArs(10L, new BigDecimal("100.00"), BigDecimal.ZERO)), LocalDate.of(2026, 6, 5));

        assertThatThrownBy(() -> service.confirmar(a.getId()))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("ASIENTO_INCOMPLETO");
    }

    @Test
    void cp03c_asientoSinLineasLanzaAsientoSinLineas() {
        Asiento a = asientoBorradorConLineas(List.of(), LocalDate.of(2026, 6, 5));

        assertThatThrownBy(() -> service.confirmar(a.getId()))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("ASIENTO_SIN_LINEAS");
    }

    // ---- CP-05: fecha intermedia sin restricción cronológica ----

    @Test
    void cp05_confirmarConFechaIntermediaNoRestringe() {
        // Fecha entre dos confirmados hipotéticos (10/06 y 20/06): el motor no
        // compara contra otros asientos, así que no hay ninguna restricción.
        Asiento a = asientoBorradorConLineas(List.of(
                lineaArs(10L, new BigDecimal("100.00"), BigDecimal.ZERO),
                lineaArs(11L, BigDecimal.ZERO, new BigDecimal("100.00"))), LocalDate.of(2026, 6, 16));
        when(numerador.siguienteNumero()).thenReturn(1L);

        Asiento confirmado = service.confirmar(a.getId());

        assertThat(confirmado.getFecha()).isEqualTo(LocalDate.of(2026, 6, 16));
        assertThat(confirmado.getEstado()).isEqualTo(EstadoDocumento.CONFIRMADO);
    }

    // ---- CP-19: TC faltante / TC cargado ----

    @Test
    void cp19_lineaUsdSinTcYSinCotizacionLanzaTcFaltante() {
        AsientoLineaRequest lineaUsd = new AsientoLineaRequest(
                10L, new BigDecimal("1500000.00"), BigDecimal.ZERO, 2L, null, new BigDecimal("1000.00"),
                null, null, null, null, null, null);
        Asiento a = asientoBorradorConLineas(List.of(lineaUsd,
                lineaArs(11L, BigDecimal.ZERO, new BigDecimal("1500000.00"))), LocalDate.of(2026, 6, 3));
        when(tipoCambioRepo.findFirstByMonedaIdAndFechaAndActivoTrueOrderByIdAsc(2L, LocalDate.of(2026, 6, 3)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmar(a.getId()))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("TC_FALTANTE");
    }

    @Test
    void cp19_lineaUsdSinTcPeroConCotizacionCargadaConfirma() {
        AsientoLineaRequest lineaUsd = new AsientoLineaRequest(
                10L, new BigDecimal("1500000.00"), BigDecimal.ZERO, 2L, null, new BigDecimal("1000.00"),
                null, null, null, null, null, null);
        Asiento a = asientoBorradorConLineas(List.of(lineaUsd,
                lineaArs(11L, BigDecimal.ZERO, new BigDecimal("1500000.00"))), LocalDate.of(2026, 6, 3));

        TipoCambio cotizacion = new TipoCambio();
        cotizacion.setValorVenta(new BigDecimal("1500.0000"));
        when(tipoCambioRepo.findFirstByMonedaIdAndFechaAndActivoTrueOrderByIdAsc(2L, LocalDate.of(2026, 6, 3)))
                .thenReturn(Optional.of(cotizacion));
        when(numerador.siguienteNumero()).thenReturn(3L);

        Asiento confirmado = service.confirmar(a.getId());

        assertThat(confirmado.getEstado()).isEqualTo(EstadoDocumento.CONFIRMADO);
        AsientoLinea lineaConfirmada = confirmado.getLineas().get(0);
        assertThat(lineaConfirmada.getTipoCambio()).isEqualByComparingTo("1500.0000");
        assertThat(lineaConfirmada.getFuenteTc()).isEqualTo(AsientoLinea.FuenteTc.AUTOMATICO);
    }

    @Test
    void lineaUsdSinImporteOriginalLanzaImporteOriginalRequerido() {
        AsientoLineaRequest lineaUsdSinImporte = new AsientoLineaRequest(
                10L, new BigDecimal("1500000.00"), BigDecimal.ZERO, 2L, new BigDecimal("1500.000000"), null,
                null, null, null, null, null, null);
        Asiento a = asientoBorradorConLineas(List.of(lineaUsdSinImporte,
                lineaArs(11L, BigDecimal.ZERO, new BigDecimal("1500000.00"))), LocalDate.of(2026, 6, 3));

        assertThatThrownBy(() -> service.confirmar(a.getId()))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("IMPORTE_ORIGINAL_REQUERIDO");
    }

    @Test
    void lineaUsdConMontoArsInconsistenteLanzaMontoArsInconsistente() {
        AsientoLineaRequest lineaUsdInconsistente = new AsientoLineaRequest(
                10L, new BigDecimal("1000000.00"), BigDecimal.ZERO, 2L, new BigDecimal("1500.000000"), new BigDecimal("1000.00"),
                null, null, null, null, null, null);
        Asiento a = asientoBorradorConLineas(List.of(lineaUsdInconsistente,
                lineaArs(11L, BigDecimal.ZERO, new BigDecimal("1000000.00"))), LocalDate.of(2026, 6, 3));

        assertThatThrownBy(() -> service.confirmar(a.getId()))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("MONTO_ARS_INCONSISTENTE");
    }

    // ---- Guarda de estado: solo se edita/elimina en borrador ----

    @Test
    void eliminarUnAsientoConfirmadoLanzaTransicionInvalida() {
        Asiento confirmado = new Asiento();
        confirmado.setId(99L);
        confirmado.setEstado(EstadoDocumento.CONFIRMADO);
        when(repo.findById(99L)).thenReturn(Optional.of(confirmado));

        assertThatThrownBy(() -> service.eliminarBorrador(99L))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("TRANSICION_ESTADO_INVALIDA");
    }

    @Test
    void confirmarUnAsientoYaConfirmadoLanzaTransicionInvalida() {
        Asiento confirmado = new Asiento();
        confirmado.setId(98L);
        confirmado.setEstado(EstadoDocumento.CONFIRMADO);
        when(repo.findById(98L)).thenReturn(Optional.of(confirmado));

        assertThatThrownBy(() -> service.confirmar(98L))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("TRANSICION_ESTADO_INVALIDA");
    }
}

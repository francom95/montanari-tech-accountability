package com.montanaritech.contable.vencimientos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.RecursoNoEncontradoException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoRepository;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.dashboard.ConfiguracionDashboardRepository;
import com.montanaritech.contable.impuestos.iibb.LiquidacionIibb;
import com.montanaritech.contable.impuestos.iibb.LiquidacionIibbRepository;
import com.montanaritech.contable.impuestos.iva.LiquidacionIva;
import com.montanaritech.contable.impuestos.iva.LiquidacionIvaRepository;
import com.montanaritech.contable.maestros.concepto.Concepto;
import com.montanaritech.contable.maestros.concepto.ConceptoRepository;
import com.montanaritech.contable.maestros.concepto.Periodicidad;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCredito;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCreditoRepository;
import com.montanaritech.contable.vencimientos.dto.GenerarAutomaticosResponse;
import com.montanaritech.contable.vencimientos.dto.VencimientoCrearRequest;
import com.montanaritech.contable.vencimientos.dto.VencimientoEditarRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VencimientoServiceTest {

    @Mock
    private VencimientoRepository repo;
    @Mock
    private MonedaRepository monedaRepository;
    @Mock
    private CuentaContableRepository cuentaContableRepository;
    @Mock
    private com.montanaritech.contable.maestros.proveedor.ProveedorRepository proveedorRepository;
    @Mock
    private TarjetaCreditoRepository tarjetaCreditoRepository;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private ConceptoRepository conceptoRepository;
    @Mock
    private AsientoRepository asientoRepository;
    @Mock
    private LiquidacionIvaRepository liquidacionIvaRepository;
    @Mock
    private LiquidacionIibbRepository liquidacionIibbRepository;
    @Mock
    private ConfiguracionDashboardRepository configuracionDashboardRepository;
    @Mock
    private VencimientoMapper mapper;
    @Mock
    private AuditoriaService auditoria;

    private VencimientoService service;
    private Moneda ars;

    @BeforeEach
    void setUp() {
        service = new VencimientoService(repo, monedaRepository, cuentaContableRepository, proveedorRepository,
                tarjetaCreditoRepository, proyectoRepository, conceptoRepository, asientoRepository,
                liquidacionIvaRepository, liquidacionIibbRepository, configuracionDashboardRepository, mapper,
                auditoria);

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        lenient().when(configuracionDashboardRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        lenient().when(monedaRepository.findByCodigo("ARS")).thenReturn(Optional.of(ars));
        lenient().when(liquidacionIvaRepository.findByEstado(EstadoDocumento.CONFIRMADO)).thenReturn(List.of());
        lenient().when(liquidacionIibbRepository.findByEstado(EstadoDocumento.CONFIRMADO)).thenReturn(List.of());
        lenient().when(tarjetaCreditoRepository.findByActivoTrue()).thenReturn(List.of());
        lenient().when(conceptoRepository.findByActivoTrue()).thenReturn(List.of());
        lenient().when(repo.findByOrigenGeneracionAndRecurrenciaNotAndEstadoIn(any(), any(), any())).thenReturn(List.of());
        lenient().when(repo.save(any(Vencimiento.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Vencimiento nuevoVencimiento(EstadoVencimientoObligacion estado) {
        Vencimiento v = new Vencimiento();
        v.setId(1L);
        v.setDescripcion("IVA julio");
        v.setTipo(TipoVencimiento.IVA);
        v.setFecha(LocalDate.now().plusDays(5));
        v.setMoneda(ars);
        v.setEstado(estado);
        return v;
    }

    // ---- CRUD ----

    @Test
    void crearResuelveMonedaYGuarda() {
        when(monedaRepository.findById(1L)).thenReturn(Optional.of(ars));

        Vencimiento creado = service.crear(new VencimientoCrearRequest("IVA julio", TipoVencimiento.IVA,
                LocalDate.of(2026, 7, 20), new BigDecimal("1000"), 1L, TipoRecurrencia.UNICA, null, null, null, null,
                null, null, null, null, null));

        assertThat(creado.getMoneda()).isEqualTo(ars);
        assertThat(creado.getDescripcion()).isEqualTo("IVA julio");
        assertThat(creado.getOrigenGeneracion()).isEqualTo(OrigenGeneracionVencimiento.MANUAL);
    }

    @Test
    void crearConMonedaInexistenteLanzaNoEncontrado() {
        when(monedaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(new VencimientoCrearRequest("X", TipoVencimiento.OTRO,
                LocalDate.now(), null, 99L, TipoRecurrencia.UNICA, null, null, null, null, null, null, null, null,
                null))).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void obtenerConIdInexistenteLanzaNoEncontrado() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void editarUnVencimientoPendienteFunciona() {
        Vencimiento existente = nuevoVencimiento(EstadoVencimientoObligacion.PENDIENTE);
        when(repo.findById(1L)).thenReturn(Optional.of(existente));
        when(monedaRepository.findById(1L)).thenReturn(Optional.of(ars));

        Vencimiento editado = service.editar(1L, new VencimientoEditarRequest("IVA julio editado",
                TipoVencimiento.IVA, LocalDate.of(2026, 7, 25), new BigDecimal("1200"), 1L, TipoRecurrencia.UNICA,
                null, null, null, null, null, null, null, null, null));

        assertThat(editado.getDescripcion()).isEqualTo("IVA julio editado");
    }

    @Test
    void editarUnVencimientoPagadoLanzaEstadoInvalido() {
        Vencimiento existente = nuevoVencimiento(EstadoVencimientoObligacion.PAGADO);
        when(repo.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.editar(1L, new VencimientoEditarRequest("X", TipoVencimiento.IVA,
                LocalDate.now(), null, 1L, TipoRecurrencia.UNICA, null, null, null, null, null, null, null, null,
                null))).isInstanceOf(IllegalStateException.class);
    }

    // ---- Ciclo de vida ----

    @Test
    void marcarPagadoConAsientoLoVincula() {
        Vencimiento existente = nuevoVencimiento(EstadoVencimientoObligacion.PENDIENTE);
        Asiento asiento = mock(Asiento.class);
        when(repo.findById(1L)).thenReturn(Optional.of(existente));
        when(asientoRepository.findById(50L)).thenReturn(Optional.of(asiento));

        Vencimiento resultado = service.marcarPagado(1L, 50L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoVencimientoObligacion.PAGADO);
        assertThat(resultado.getAsientoVinculado()).isEqualTo(asiento);
    }

    @Test
    void marcarPagadoUnoYaPagadoLanzaEstadoInvalido() {
        Vencimiento existente = nuevoVencimiento(EstadoVencimientoObligacion.PAGADO);
        when(repo.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.marcarPagado(1L, null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reprogramarActualizaFechaYEstado() {
        Vencimiento existente = nuevoVencimiento(EstadoVencimientoObligacion.PENDIENTE);
        when(repo.findById(1L)).thenReturn(Optional.of(existente));

        LocalDate nuevaFecha = LocalDate.now().plusMonths(1);
        Vencimiento resultado = service.reprogramar(1L, nuevaFecha, "el proveedor pidió postergar");

        assertThat(resultado.getFecha()).isEqualTo(nuevaFecha);
        assertThat(resultado.getEstado()).isEqualTo(EstadoVencimientoObligacion.REPROGRAMADO);
    }

    @Test
    void reprogramarUnoCanceladoLanzaEstadoInvalido() {
        Vencimiento existente = nuevoVencimiento(EstadoVencimientoObligacion.CANCELADO);
        when(repo.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.reprogramar(1L, LocalDate.now(), null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelarGuardaMotivo() {
        Vencimiento existente = nuevoVencimiento(EstadoVencimientoObligacion.PENDIENTE);
        when(repo.findById(1L)).thenReturn(Optional.of(existente));

        Vencimiento resultado = service.cancelar(1L, "ya no aplica");

        assertThat(resultado.getEstado()).isEqualTo(EstadoVencimientoObligacion.CANCELADO);
        assertThat(resultado.getMotivoCancelacion()).isEqualTo("ya no aplica");
    }

    // ---- generarAutomaticos: 5 fuentes ----

    @Test
    void generaVencimientoDesdeLiquidacionIvaConfirmada() {
        LiquidacionIva liq = new LiquidacionIva();
        liq.setId(10L);
        liq.setAnio(2026);
        liq.setMes(6);
        liq.setEstado(EstadoDocumento.CONFIRMADO);
        liq.setSaldoAPagar(new BigDecimal("1500.00"));
        when(liquidacionIvaRepository.findByEstado(EstadoDocumento.CONFIRMADO)).thenReturn(List.of(liq));
        when(repo.existsByOrigenGeneracionAndOrigenGeneracionRefId(OrigenGeneracionVencimiento.LIQUIDACION_IVA, 10L))
                .thenReturn(false);

        GenerarAutomaticosResponse resumen = service.generarAutomaticos();

        ArgumentCaptor<Vencimiento> captor = ArgumentCaptor.forClass(Vencimiento.class);
        verify(repo).save(captor.capture());
        Vencimiento generado = captor.getValue();
        assertThat(generado.getTipo()).isEqualTo(TipoVencimiento.IVA);
        assertThat(generado.getFecha()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(generado.getImporteEstimado()).isEqualByComparingTo("1500.00");
        assertThat(generado.getOrigenGeneracion()).isEqualTo(OrigenGeneracionVencimiento.LIQUIDACION_IVA);
        assertThat(generado.getOrigenGeneracionRefId()).isEqualTo(10L);
        assertThat(resumen.total()).isEqualTo(1);
    }

    @Test
    void noDuplicaVencimientoDeLiquidacionIvaYaGenerado() {
        LiquidacionIva liq = new LiquidacionIva();
        liq.setId(10L);
        liq.setAnio(2026);
        liq.setMes(6);
        liq.setEstado(EstadoDocumento.CONFIRMADO);
        liq.setSaldoAPagar(new BigDecimal("1500.00"));
        when(liquidacionIvaRepository.findByEstado(EstadoDocumento.CONFIRMADO)).thenReturn(List.of(liq));
        when(repo.existsByOrigenGeneracionAndOrigenGeneracionRefId(OrigenGeneracionVencimiento.LIQUIDACION_IVA, 10L))
                .thenReturn(true);

        GenerarAutomaticosResponse resumen = service.generarAutomaticos();

        verify(repo, never()).save(any());
        assertThat(resumen.total()).isEqualTo(0);
    }

    @Test
    void generaVencimientoDesdeLiquidacionIibbConfirmada() {
        LiquidacionIibb liq = new LiquidacionIibb();
        liq.setId(11L);
        liq.setAnio(2026);
        liq.setMes(6);
        liq.setEstado(EstadoDocumento.CONFIRMADO);
        liq.setSaldoAPagarTotal(new BigDecimal("800.00"));
        when(liquidacionIibbRepository.findByEstado(EstadoDocumento.CONFIRMADO)).thenReturn(List.of(liq));

        GenerarAutomaticosResponse resumen = service.generarAutomaticos();

        ArgumentCaptor<Vencimiento> captor = ArgumentCaptor.forClass(Vencimiento.class);
        verify(repo).save(captor.capture());
        Vencimiento generado = captor.getValue();
        assertThat(generado.getTipo()).isEqualTo(TipoVencimiento.IIBB);
        assertThat(generado.getFecha()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(generado.getImporteEstimado()).isEqualByComparingTo("800.00");
        assertThat(resumen.total()).isEqualTo(1);
    }

    @Test
    void generaVencimientoDesdeTarjetaActiva() {
        TarjetaCredito tarjeta = new TarjetaCredito();
        tarjeta.setId(20L);
        tarjeta.setEntidad("Visa Banco X");
        tarjeta.setActivo(true);
        tarjeta.setDiaVencimiento(28);
        tarjeta.setSaldoActual(new BigDecimal("5000.00"));
        when(tarjetaCreditoRepository.findByActivoTrue()).thenReturn(List.of(tarjeta));

        GenerarAutomaticosResponse resumen = service.generarAutomaticos();

        ArgumentCaptor<Vencimiento> captor = ArgumentCaptor.forClass(Vencimiento.class);
        verify(repo).save(captor.capture());
        Vencimiento generado = captor.getValue();
        LocalDate hoy = LocalDate.now();
        LocalDate esperado = hoy.withDayOfMonth(28);
        if (esperado.isBefore(hoy)) {
            esperado = YearMonth.from(hoy).plusMonths(1).atDay(28);
        }
        assertThat(generado.getTipo()).isEqualTo(TipoVencimiento.TARJETA);
        assertThat(generado.getTarjetaCredito()).isEqualTo(tarjeta);
        assertThat(generado.getImporteEstimado()).isEqualByComparingTo("5000.00");
        assertThat(generado.getFecha()).isEqualTo(esperado);
        assertThat(resumen.total()).isEqualTo(1);
    }

    @Test
    void noDuplicaVencimientoDeTarjetaYaGeneradoParaEsaFecha() {
        TarjetaCredito tarjeta = new TarjetaCredito();
        tarjeta.setId(20L);
        tarjeta.setEntidad("Visa Banco X");
        tarjeta.setActivo(true);
        tarjeta.setDiaVencimiento(28);
        tarjeta.setSaldoActual(new BigDecimal("5000.00"));
        when(tarjetaCreditoRepository.findByActivoTrue()).thenReturn(List.of(tarjeta));
        when(repo.existsByOrigenGeneracionAndOrigenGeneracionRefIdAndFecha(
                eq(OrigenGeneracionVencimiento.TARJETA), eq(20L), any())).thenReturn(true);

        GenerarAutomaticosResponse resumen = service.generarAutomaticos();

        verify(repo, never()).save(any());
        assertThat(resumen.total()).isEqualTo(0);
    }

    @Test
    void generaVencimientoMensualDesdeConceptoActivo() {
        Concepto concepto = new Concepto();
        concepto.setId(30L);
        concepto.setNombre("Suscripción hosting");
        concepto.setActivo(true);
        concepto.setPeriodicidad(Periodicidad.MENSUAL);
        concepto.setImporte(new BigDecimal("99.00"));
        concepto.setCreadoEn(Instant.parse("2020-03-15T12:00:00Z"));
        when(conceptoRepository.findByActivoTrue()).thenReturn(List.of(concepto));

        GenerarAutomaticosResponse resumen = service.generarAutomaticos();

        ArgumentCaptor<Vencimiento> captor = ArgumentCaptor.forClass(Vencimiento.class);
        verify(repo).save(captor.capture());
        Vencimiento generado = captor.getValue();
        assertThat(generado.getConceptoRecurrente()).isEqualTo(concepto);
        assertThat(generado.getMoneda()).isEqualTo(ars);
        assertThat(generado.getFecha()).isEqualTo(YearMonth.now().atDay(15));
        assertThat(resumen.total()).isEqualTo(1);
    }

    @Test
    void generaVencimientoAnualDesdeConceptoActivo() {
        Concepto concepto = new Concepto();
        concepto.setId(31L);
        concepto.setNombre("Renovación dominio");
        concepto.setActivo(true);
        concepto.setPeriodicidad(Periodicidad.ANUAL);
        concepto.setImporte(new BigDecimal("50.00"));
        concepto.setCreadoEn(Instant.parse("2020-06-10T12:00:00Z"));
        when(conceptoRepository.findByActivoTrue()).thenReturn(List.of(concepto));

        GenerarAutomaticosResponse resumen = service.generarAutomaticos();

        ArgumentCaptor<Vencimiento> captor = ArgumentCaptor.forClass(Vencimiento.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getFecha()).isEqualTo(LocalDate.of(LocalDate.now().getYear(), 6, 10));
    }

    @Test
    void conceptoConPeriodicidadUnicaNoGeneraNada() {
        Concepto concepto = new Concepto();
        concepto.setId(32L);
        concepto.setNombre("Compra puntual");
        concepto.setActivo(true);
        concepto.setPeriodicidad(Periodicidad.UNICA);
        when(conceptoRepository.findByActivoTrue()).thenReturn(List.of(concepto));

        GenerarAutomaticosResponse resumen = service.generarAutomaticos();

        verify(repo, never()).save(any());
        assertThat(resumen.total()).isEqualTo(0);
    }

    @Test
    void encadenaLaProximaOcurrenciaDeUnVencimientoManualMensualResuelto() {
        Vencimiento resuelto = new Vencimiento();
        resuelto.setId(40L);
        resuelto.setDescripcion("Cuota plan de pagos AFIP");
        resuelto.setTipo(TipoVencimiento.PLAN_DE_PAGO);
        resuelto.setFecha(LocalDate.of(2026, 5, 10));
        resuelto.setImporteEstimado(new BigDecimal("300.00"));
        resuelto.setMoneda(ars);
        resuelto.setRecurrencia(TipoRecurrencia.MENSUAL);
        resuelto.setOrigenGeneracion(OrigenGeneracionVencimiento.MANUAL);
        resuelto.setEstado(EstadoVencimientoObligacion.PAGADO);
        when(repo.findByOrigenGeneracionAndRecurrenciaNotAndEstadoIn(OrigenGeneracionVencimiento.MANUAL,
                TipoRecurrencia.UNICA,
                List.of(EstadoVencimientoObligacion.PAGADO, EstadoVencimientoObligacion.CANCELADO)))
                .thenReturn(List.of(resuelto));

        GenerarAutomaticosResponse resumen = service.generarAutomaticos();

        ArgumentCaptor<Vencimiento> captor = ArgumentCaptor.forClass(Vencimiento.class);
        verify(repo).save(captor.capture());
        Vencimiento generado = captor.getValue();
        assertThat(generado.getFecha()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(generado.getOrigenGeneracion()).isEqualTo(OrigenGeneracionVencimiento.MANUAL);
        assertThat(generado.getOrigenGeneracionRefId()).isEqualTo(40L);
        assertThat(resumen.total()).isEqualTo(1);
    }

    // ---- próximos ----

    @Test
    void proximosDelegaEnElRepositorioConLaVentanaPedida() {
        Vencimiento v = nuevoVencimiento(EstadoVencimientoObligacion.PENDIENTE);
        when(repo.findByEstadoAndFechaLessThanEqualOrderByFechaAsc(eq(EstadoVencimientoObligacion.PENDIENTE), any()))
                .thenReturn(List.of(v));

        List<Vencimiento> resultado = service.proximos(15);

        assertThat(resultado).containsExactly(v);
        verify(repo).findByEstadoAndFechaLessThanEqualOrderByFechaAsc(
                eq(EstadoVencimientoObligacion.PENDIENTE), eq(LocalDate.now().plusDays(15)));
    }
}

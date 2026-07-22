package com.montanaritech.contable.bancos.movimientobancario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.bancos.movimientobancario.dto.CorregirMovimientoBancarioRequest;
import com.montanaritech.contable.bancos.movimientobancario.dto.CrearMovimientoBancarioRequest;
import com.montanaritech.contable.common.asiento.AsientoGenerado;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoRepository;
import com.montanaritech.contable.contabilidad.asiento.AsientoService;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Bandeja de movimientos bancarios (F5.1): las 4 acciones de resolución + la guarda de estado PENDIENTE. */
@ExtendWith(MockitoExtension.class)
class MovimientoBancarioServiceTest {

    @Mock private MovimientoBancarioRepository repo;
    @Mock private MovimientoBancarioMapper mapper;
    @Mock private AuditoriaService auditoria;
    @Mock private AsientoService asientoService;
    @Mock private AsientoRepository asientoRepo;
    @Mock private CuentaBancariaRepository cuentaBancariaRepo;
    @Mock private MonedaRepository monedaRepo;
    @Mock private CuentaContableRepository cuentaContableRepo;

    private MovimientoBancarioService service;
    private CuentaBancaria cuentaBancaria;
    private CuentaContable cuentaFondos;
    private CuentaContable cuentaSugerida;
    private Moneda ars;

    @BeforeEach
    void setUp() {
        service = new MovimientoBancarioService(repo, mapper, auditoria, asientoService, asientoRepo,
                cuentaBancariaRepo, monedaRepo, cuentaContableRepo);

        cuentaFondos = new CuentaContable();
        cuentaFondos.setId(1L);
        cuentaFondos.setCodigo("1.1.2001");

        cuentaBancaria = new CuentaBancaria();
        cuentaBancaria.setId(1L);
        cuentaBancaria.setAlias("Banco Galicia CC");
        cuentaBancaria.setCuentaContable(cuentaFondos);

        cuentaSugerida = new CuentaContable();
        cuentaSugerida.setId(2L);
        cuentaSugerida.setCodigo("6.4003");

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        lenient().when(cuentaBancariaRepo.findById(1L)).thenReturn(Optional.of(cuentaBancaria));
        lenient().when(monedaRepo.findById(1L)).thenReturn(Optional.of(ars));
        lenient().when(cuentaContableRepo.findById(2L)).thenReturn(Optional.of(cuentaSugerida));
        lenient().when(repo.save(any(MovimientoBancario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CrearMovimientoBancarioRequest requestCrear(BigDecimal importe, Long cuentaSugeridaId) {
        return new CrearMovimientoBancarioRequest(1L, LocalDate.of(2026, 7, 1), "Deposito cliente",
                importe, 1L, new BigDecimal("1.000000"), "REF-001", cuentaSugeridaId, null, null, null);
    }

    // ---- Creación ----

    @Test
    void crearGuardaConEstadoPendiente() {
        MovimientoBancario creado = service.crear(requestCrear(new BigDecimal("1000.00"), null));

        assertThat(creado.getEstado()).isEqualTo(EstadoMovimientoBancario.PENDIENTE);
        assertThat(creado.getImporteArs()).isEqualByComparingTo("1000.00");
        assertThat(creado.getTipoCambio()).isEqualByComparingTo("1.000000");
    }

    @Test
    void crearConImporteCeroLanzaError() {
        assertThatThrownBy(() -> service.crear(requestCrear(BigDecimal.ZERO, null)))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("IMPORTE_CERO");
    }

    // ---- Confirmar ----

    @Test
    void confirmarSinCuentaSugeridaLanzaError() {
        MovimientoBancario m = new MovimientoBancario();
        m.setId(1L);
        m.setEstado(EstadoMovimientoBancario.PENDIENTE);
        when(repo.findById(1L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.confirmar(1L))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("SIN_CUENTA_SUGERIDA");
    }

    @Test
    void confirmarSinFechaLanzaError() {
        MovimientoBancario m = new MovimientoBancario();
        m.setId(1L);
        m.setEstado(EstadoMovimientoBancario.PENDIENTE);
        m.setCuentaContableSugerida(cuentaSugerida);
        m.setFecha(null);
        when(repo.findById(1L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.confirmar(1L))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("FECHA_PENDIENTE");
    }

    @Test
    void crearSinFechaQuedaPendienteDeCompletar() {
        CrearMovimientoBancarioRequest req = new CrearMovimientoBancarioRequest(1L, null, "Movimiento sin fecha (Galicia ARS)",
                new BigDecimal("1000.00"), 1L, new BigDecimal("1.000000"), "REF-002", null, null,
                OrigenImportacionMovimiento.GALICIA, "hash-abc");

        MovimientoBancario creado = service.crear(req);

        assertThat(creado.getFecha()).isNull();
        assertThat(creado.getEstado()).isEqualTo(EstadoMovimientoBancario.PENDIENTE);
        assertThat(creado.getOrigenImportacion()).isEqualTo(OrigenImportacionMovimiento.GALICIA);
        assertThat(creado.getHashImportacion()).isEqualTo("hash-abc");
    }

    @Test
    void confirmarUnIngresoDebitaFondosYAcreditaLaCuentaSugerida() {
        MovimientoBancario m = service.crear(requestCrear(new BigDecimal("1000.00"), 2L));
        m.setId(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(m));
        Asiento asientoPersistido = new Asiento();
        asientoPersistido.setId(99L);
        asientoPersistido.setNumero(5L);
        ArgumentCaptor<AsientoGenerado> captor = ArgumentCaptor.forClass(AsientoGenerado.class);
        when(asientoService.registrarAutomatico(captor.capture())).thenReturn(asientoPersistido);

        MovimientoBancario confirmado = service.confirmar(1L);

        assertThat(confirmado.getEstado()).isEqualTo(EstadoMovimientoBancario.CONCILIADO);
        assertThat(confirmado.getAsiento()).isSameAs(asientoPersistido);
        AsientoGenerado generado = captor.getValue();
        assertThat(generado.origen()).isEqualTo("MOVIMIENTO_BANCARIO");
        assertThat(generado.lineas()).hasSize(2);
        assertThat(generado.lineas().get(0).cuentaCodigo()).isEqualTo("1.1.2001");
        assertThat(generado.lineas().get(0).debe()).isEqualByComparingTo("1000.00");
        assertThat(generado.lineas().get(1).cuentaCodigo()).isEqualTo("6.4003");
        assertThat(generado.lineas().get(1).haber()).isEqualByComparingTo("1000.00");
    }

    @Test
    void confirmarUnEgresoAcreditaFondosYDebitaLaCuentaSugerida() {
        MovimientoBancario m = service.crear(requestCrear(new BigDecimal("-500.00"), 2L));
        m.setId(2L);
        when(repo.findById(2L)).thenReturn(Optional.of(m));
        Asiento asientoPersistido = new Asiento();
        asientoPersistido.setId(100L);
        asientoPersistido.setNumero(6L);
        ArgumentCaptor<AsientoGenerado> captor = ArgumentCaptor.forClass(AsientoGenerado.class);
        when(asientoService.registrarAutomatico(captor.capture())).thenReturn(asientoPersistido);

        service.confirmar(2L);

        AsientoGenerado generado = captor.getValue();
        assertThat(generado.lineas().get(0).cuentaCodigo()).isEqualTo("1.1.2001");
        assertThat(generado.lineas().get(0).haber()).isEqualByComparingTo("500.00");
        assertThat(generado.lineas().get(1).cuentaCodigo()).isEqualTo("6.4003");
        assertThat(generado.lineas().get(1).debe()).isEqualByComparingTo("500.00");
    }

    // ---- Imputar ----

    @Test
    void imputarUsaLaCuentaElegidaIgnorandoLaSugerida() {
        CuentaContable otraCuenta = new CuentaContable();
        otraCuenta.setId(3L);
        otraCuenta.setCodigo("5.1.2001");
        when(cuentaContableRepo.findById(3L)).thenReturn(Optional.of(otraCuenta));

        MovimientoBancario m = service.crear(requestCrear(new BigDecimal("1000.00"), 2L));
        m.setId(3L);
        when(repo.findById(3L)).thenReturn(Optional.of(m));
        Asiento asientoPersistido = new Asiento();
        asientoPersistido.setId(101L);
        asientoPersistido.setNumero(7L);
        ArgumentCaptor<AsientoGenerado> captor = ArgumentCaptor.forClass(AsientoGenerado.class);
        when(asientoService.registrarAutomatico(captor.capture())).thenReturn(asientoPersistido);

        MovimientoBancario imputado = service.imputar(3L, 3L);

        assertThat(imputado.getEstado()).isEqualTo(EstadoMovimientoBancario.CONCILIADO);
        assertThat(captor.getValue().lineas().get(1).cuentaCodigo()).isEqualTo("5.1.2001");
    }

    // ---- Asociar ----

    @Test
    void asociarVinculaUnAsientoExistenteSinGenerarUnoNuevo() {
        MovimientoBancario m = new MovimientoBancario();
        m.setId(4L);
        m.setEstado(EstadoMovimientoBancario.PENDIENTE);
        when(repo.findById(4L)).thenReturn(Optional.of(m));

        Asiento asientoExistente = new Asiento();
        asientoExistente.setId(200L);
        asientoExistente.setNumero(10L);
        asientoExistente.setEstado(EstadoDocumento.CONFIRMADO);
        when(asientoRepo.findByNumero(10L)).thenReturn(Optional.of(asientoExistente));
        when(repo.existsByAsiento_Id(200L)).thenReturn(false);

        MovimientoBancario asociado = service.asociar(4L, 10L);

        assertThat(asociado.getEstado()).isEqualTo(EstadoMovimientoBancario.CONCILIADO);
        assertThat(asociado.getAsiento()).isSameAs(asientoExistente);
        verify(asientoService, org.mockito.Mockito.never()).registrarAutomatico(any());
    }

    @Test
    void asociarConAsientoYaAsociadoLanzaError() {
        MovimientoBancario m = new MovimientoBancario();
        m.setId(5L);
        m.setEstado(EstadoMovimientoBancario.PENDIENTE);
        when(repo.findById(5L)).thenReturn(Optional.of(m));

        Asiento asientoExistente = new Asiento();
        asientoExistente.setId(201L);
        asientoExistente.setNumero(11L);
        asientoExistente.setEstado(EstadoDocumento.CONFIRMADO);
        when(asientoRepo.findByNumero(11L)).thenReturn(Optional.of(asientoExistente));
        when(repo.existsByAsiento_Id(201L)).thenReturn(true);

        assertThatThrownBy(() -> service.asociar(5L, 11L))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("ASIENTO_YA_ASOCIADO");
    }

    @Test
    void asociarConAsientoNoConfirmadoLanzaError() {
        MovimientoBancario m = new MovimientoBancario();
        m.setId(6L);
        m.setEstado(EstadoMovimientoBancario.PENDIENTE);
        when(repo.findById(6L)).thenReturn(Optional.of(m));

        Asiento asientoBorrador = new Asiento();
        asientoBorrador.setId(202L);
        asientoBorrador.setNumero(12L);
        asientoBorrador.setEstado(EstadoDocumento.BORRADOR);
        when(asientoRepo.findByNumero(12L)).thenReturn(Optional.of(asientoBorrador));

        assertThatThrownBy(() -> service.asociar(6L, 12L))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("ASIENTO_NO_CONFIRMADO");
    }

    // ---- Descartar ----

    @Test
    void descartarMarcaComoDescartadoConMotivo() {
        MovimientoBancario m = new MovimientoBancario();
        m.setId(7L);
        m.setEstado(EstadoMovimientoBancario.PENDIENTE);
        when(repo.findById(7L)).thenReturn(Optional.of(m));

        MovimientoBancario descartado = service.descartar(7L, "Duplicado del extracto");

        assertThat(descartado.getEstado()).isEqualTo(EstadoMovimientoBancario.DESCARTADO);
        assertThat(descartado.getMotivoDescarte()).isEqualTo("Duplicado del extracto");
        verify(asientoService, org.mockito.Mockito.never()).registrarAutomatico(any());
    }

    // ---- Guarda de estado ----

    @Test
    void accionSobreMovimientoYaResueltoLanzaError() {
        MovimientoBancario m = new MovimientoBancario();
        m.setId(8L);
        m.setEstado(EstadoMovimientoBancario.CONCILIADO);
        when(repo.findById(8L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.descartar(8L, "motivo"))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("MOVIMIENTO_NO_PENDIENTE");
    }

    @Test
    void corregirActualizaCamposMientrasEstaPendiente() {
        MovimientoBancario m = service.crear(requestCrear(new BigDecimal("1000.00"), null));
        m.setId(9L);
        when(repo.findById(9L)).thenReturn(Optional.of(m));

        MovimientoBancario corregido = service.corregir(9L,
                new CorregirMovimientoBancarioRequest(1L, LocalDate.of(2026, 7, 2), "Deposito corregido",
                        new BigDecimal("1500.00"), 1L, new BigDecimal("1.000000"), "REF-002", 2L, "obs"));

        assertThat(corregido.getDescripcion()).isEqualTo("Deposito corregido");
        assertThat(corregido.getImporte()).isEqualByComparingTo("1500.00");
        assertThat(corregido.getCuentaContableSugerida()).isSameAs(cuentaSugerida);
    }
}

package com.montanaritech.contable.facturacion.facturacompra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.asiento.AsientoGenerado;
import com.montanaritech.contable.common.audit.AuditoriaService;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.contabilidad.asiento.Asiento;
import com.montanaritech.contable.contabilidad.asiento.AsientoService;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContableRepository;
import com.montanaritech.contable.facturacion.TipoComprobante;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTributoRepository;
import com.montanaritech.contable.facturacion.comprobantetributo.TipoTributo;
import com.montanaritech.contable.facturacion.facturacompra.dto.FacturaCompraCrearRequest;
import com.montanaritech.contable.facturacion.facturacompra.dto.FacturaCompraLineaRequest;
import com.montanaritech.contable.facturacion.facturacompra.dto.FacturaCompraTributoRequest;
import com.montanaritech.contable.maestros.jurisdiccion.JurisdiccionRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import com.montanaritech.contable.maestros.tipocosto.TipoCosto;
import com.montanaritech.contable.maestros.tipocosto.TipoCostoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Ciclo de vida de la factura de compra: cálculo de totales (incluidas percepciones), confirmación (delega en el generator + AsientoService.registrarAutomatico) y anulación. */
@ExtendWith(MockitoExtension.class)
class FacturaCompraServiceTest {

    @Mock private FacturaCompraRepository repo;
    @Mock private FacturaCompraMapper mapper;
    @Mock private AuditoriaService auditoria;
    @Mock private AsientoService asientoService;
    @Mock private FacturaCompraAsientoGenerator generator;
    @Mock private ProveedorRepository proveedorRepo;
    @Mock private ProyectoRepository proyectoRepo;
    @Mock private JurisdiccionRepository jurisdiccionRepo;
    @Mock private MonedaRepository monedaRepo;
    @Mock private CuentaContableRepository cuentaContableRepo;
    @Mock private TipoCostoRepository tipoCostoRepo;
    @Mock private ComprobanteTributoRepository comprobanteTributoRepo;

    private FacturaCompraService service;
    private Proveedor proveedor;
    private TipoCosto tipoCosto;
    private Moneda ars;

    @BeforeEach
    void setUp() {
        service = new FacturaCompraService(repo, mapper, auditoria, asientoService, generator,
                proveedorRepo, proyectoRepo, jurisdiccionRepo, monedaRepo, cuentaContableRepo, tipoCostoRepo, comprobanteTributoRepo);

        proveedor = new Proveedor();
        proveedor.setId(1L);
        proveedor.setNombre("Proveedor Test");

        tipoCosto = new TipoCosto();
        tipoCosto.setId(1L);
        tipoCosto.setNombre("Programador");

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        lenient().when(proveedorRepo.findById(1L)).thenReturn(Optional.of(proveedor));
        lenient().when(monedaRepo.findById(1L)).thenReturn(Optional.of(ars));
        lenient().when(tipoCostoRepo.findById(1L)).thenReturn(Optional.of(tipoCosto));
        lenient().when(repo.save(any(FacturaCompra.class))).thenAnswer(inv -> {
            FacturaCompra f = inv.getArgument(0);
            if (f.getId() == null) {
                f.setId(50L);
            }
            return f;
        });
    }

    private FacturaCompraLineaRequest lineaCosto(BigDecimal neto, BigDecimal alicuota) {
        return new FacturaCompraLineaRequest("Desarrollo", 1L, neto, alicuota, null);
    }

    private FacturaCompraCrearRequest requestCrear(List<FacturaCompraLineaRequest> lineas, List<FacturaCompraTributoRequest> tributos) {
        return new FacturaCompraCrearRequest(1L, null, LocalDate.of(2026, 6, 15), null,
                TipoComprobante.FACTURA_A, "0001", "00000123", 1L, new BigDecimal("1.000000"), null, lineas, tributos);
    }

    // ---- Cálculo de totales al crear ----

    @Test
    void crearBorradorRecalculaTotalesDesdeLasLineas() {
        FacturaCompra creada = service.crearBorrador(requestCrear(List.of(
                lineaCosto(new BigDecimal("100000.00"), new BigDecimal("21"))), null));

        assertThat(creada.getEstado()).isEqualTo(EstadoDocumento.BORRADOR);
        assertThat(creada.getNeto()).isEqualByComparingTo("100000.00");
        assertThat(creada.getImporteIva()).isEqualByComparingTo("21000.00");
        assertThat(creada.getImportePercepciones()).isEqualByComparingTo("0");
        assertThat(creada.getTotal()).isEqualByComparingTo("121000.00");
        assertThat(creada.getTotalArs()).isEqualByComparingTo("121000.00");
        assertThat(creada.getTipoCambio()).isEqualByComparingTo("1.000000");
    }

    @Test
    void crearBorradorSumaLasPercepcionesAlTotal() {
        FacturaCompraTributoRequest percepcion = new FacturaCompraTributoRequest(TipoTributo.PERCEPCION_IVA, null, null, null, new BigDecimal("3000.00"));

        FacturaCompra creada = service.crearBorrador(requestCrear(List.of(
                lineaCosto(new BigDecimal("100000.00"), new BigDecimal("21"))), List.of(percepcion)));

        assertThat(creada.getImportePercepciones()).isEqualByComparingTo("3000.00");
        assertThat(creada.getTotal()).isEqualByComparingTo("124000.00");
    }

    @Test
    void crearBorradorConAlicuotaInvalidaLanzaError() {
        assertThatThrownBy(() -> service.crearBorrador(requestCrear(List.of(
                lineaCosto(new BigDecimal("100.00"), new BigDecimal("15"))), null)))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("ALICUOTA_IVA_INVALIDA");
    }

    @Test
    void crearBorradorConTributoNoAplicableLanzaError() {
        FacturaCompraTributoRequest retencion = new FacturaCompraTributoRequest(TipoTributo.RETENCION_GANANCIAS, null, null, null, new BigDecimal("1000.00"));

        assertThatThrownBy(() -> service.crearBorrador(requestCrear(List.of(
                lineaCosto(new BigDecimal("100000.00"), new BigDecimal("21"))), List.of(retencion))))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("TRIBUTO_NO_APLICABLE_A_COMPRA");
    }

    // ---- Confirmación ----

    @Test
    void confirmarLlamaAlGeneradorYRegistraElAsientoAutomatico() {
        FacturaCompra f = service.crearBorrador(requestCrear(List.of(
                lineaCosto(new BigDecimal("100000.00"), new BigDecimal("21"))), null));
        when(repo.findById(50L)).thenReturn(Optional.of(f));

        AsientoGenerado asientoGenerado = new AsientoGenerado(f.getFecha(), "desc", "FACTURA_COMPRA", List.of(), "FacturaCompra", 50L);
        Asiento asientoPersistido = new Asiento();
        asientoPersistido.setId(999L);
        asientoPersistido.setNumero(7L);
        when(generator.generar(f)).thenReturn(asientoGenerado);
        when(asientoService.registrarAutomatico(asientoGenerado)).thenReturn(asientoPersistido);

        FacturaCompra confirmada = service.confirmar(50L);

        assertThat(confirmada.getEstado()).isEqualTo(EstadoDocumento.CONFIRMADO);
        assertThat(confirmada.getAsiento()).isSameAs(asientoPersistido);
        verify(generator).generar(f);
        verify(asientoService).registrarAutomatico(asientoGenerado);
    }

    @Test
    void confirmarUnaFacturaYaConfirmadaLanzaTransicionInvalida() {
        FacturaCompra f = new FacturaCompra();
        f.setId(51L);
        f.setEstado(EstadoDocumento.CONFIRMADO);
        when(repo.findById(51L)).thenReturn(Optional.of(f));

        assertThatThrownBy(() -> service.confirmar(51L))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("TRANSICION_ESTADO_INVALIDA");
    }

    // ---- Anulación ----

    @Test
    void anularUnaFacturaConfirmadaAnulaSuAsientoVinculado() {
        Asiento asiento = new Asiento();
        asiento.setId(999L);

        FacturaCompra f = new FacturaCompra();
        f.setId(52L);
        f.setEstado(EstadoDocumento.CONFIRMADO);
        f.setAsiento(asiento);
        when(repo.findById(52L)).thenReturn(Optional.of(f));

        FacturaCompra anulada = service.anular(52L, "factura duplicada");

        assertThat(anulada.getEstado()).isEqualTo(EstadoDocumento.ANULADO);
        verify(asientoService).anularPorDocumento(999L, "factura duplicada");
    }

    @Test
    void anularUnBorradorLanzaTransicionInvalida() {
        FacturaCompra f = new FacturaCompra();
        f.setId(53L);
        f.setEstado(EstadoDocumento.BORRADOR);
        when(repo.findById(53L)).thenReturn(Optional.of(f));

        assertThatThrownBy(() -> service.anular(53L, "motivo"))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("TRANSICION_ESTADO_INVALIDA");
    }

    // ---- Guarda de borrador ----

    @Test
    void eliminarUnaFacturaConfirmadaLanzaTransicionInvalida() {
        FacturaCompra f = new FacturaCompra();
        f.setId(54L);
        f.setEstado(EstadoDocumento.CONFIRMADO);
        when(repo.findById(54L)).thenReturn(Optional.of(f));

        assertThatThrownBy(() -> service.eliminarBorrador(54L))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("TRANSICION_ESTADO_INVALIDA");
    }
}

package com.montanaritech.contable.facturacion.facturaventa;

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
import com.montanaritech.contable.facturacion.facturaventa.dto.FacturaVentaCrearRequest;
import com.montanaritech.contable.facturacion.facturaventa.dto.FacturaVentaLineaRequest;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.cliente.ClienteRepository;
import com.montanaritech.contable.maestros.jurisdiccion.JurisdiccionRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proyecto.ProyectoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Ciclo de vida de la factura de venta: cálculo de totales, confirmación (delega en el generator + AsientoService.registrarAutomatico) y anulación. */
@ExtendWith(MockitoExtension.class)
class FacturaVentaServiceTest {

    @Mock private FacturaVentaRepository repo;
    @Mock private FacturaVentaMapper mapper;
    @Mock private AuditoriaService auditoria;
    @Mock private AsientoService asientoService;
    @Mock private FacturaVentaAsientoGenerator generator;
    @Mock private ClienteRepository clienteRepo;
    @Mock private ProyectoRepository proyectoRepo;
    @Mock private JurisdiccionRepository jurisdiccionRepo;
    @Mock private MonedaRepository monedaRepo;
    @Mock private CuentaContableRepository cuentaContableRepo;

    private FacturaVentaService service;
    private Cliente cliente;
    private Moneda ars;

    @BeforeEach
    void setUp() {
        service = new FacturaVentaService(repo, mapper, auditoria, asientoService, generator,
                clienteRepo, proyectoRepo, jurisdiccionRepo, monedaRepo, cuentaContableRepo);

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Cliente Test");

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        lenient().when(clienteRepo.findById(1L)).thenReturn(Optional.of(cliente));
        lenient().when(monedaRepo.findById(1L)).thenReturn(Optional.of(ars));
        lenient().when(repo.save(any(FacturaVenta.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private FacturaVentaLineaRequest lineaGravada(BigDecimal neto, BigDecimal alicuota) {
        return new FacturaVentaLineaRequest("Servicio", TipoLineaFactura.GRAVADO, neto, alicuota, TipoIngreso.VENTA, null);
    }

    private FacturaVentaCrearRequest requestCrear(List<FacturaVentaLineaRequest> lineas) {
        return new FacturaVentaCrearRequest(1L, null, LocalDate.of(2026, 6, 15), null,
                TipoComprobante.FACTURA_B, "0001", "00000123", null, 1L, new BigDecimal("1.000000"), null, lineas);
    }

    // ---- Cálculo de totales al crear ----

    @Test
    void crearBorradorRecalculaTotalesDesdeLasLineas() {
        FacturaVenta creada = service.crearBorrador(requestCrear(List.of(
                lineaGravada(new BigDecimal("100000.00"), new BigDecimal("21")))));

        assertThat(creada.getEstado()).isEqualTo(EstadoDocumento.BORRADOR);
        assertThat(creada.getNetoGravado()).isEqualByComparingTo("100000.00");
        assertThat(creada.getImporteIva()).isEqualByComparingTo("21000.00");
        assertThat(creada.getTotal()).isEqualByComparingTo("121000.00");
        assertThat(creada.getTotalArs()).isEqualByComparingTo("121000.00");
        assertThat(creada.getTipoCambio()).isEqualByComparingTo("1.000000");
    }

    @Test
    void crearBorradorConAlicuotaInvalidaLanzaError() {
        assertThatThrownBy(() -> service.crearBorrador(requestCrear(List.of(
                lineaGravada(new BigDecimal("100.00"), new BigDecimal("15"))))))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("ALICUOTA_IVA_INVALIDA");
    }

    // ---- Confirmación ----

    @Test
    void confirmarLlamaAlGeneradorYRegistraElAsientoAutomatico() {
        FacturaVenta f = service.crearBorrador(requestCrear(List.of(
                lineaGravada(new BigDecimal("100000.00"), new BigDecimal("21")))));
        f.setId(50L);
        when(repo.findById(50L)).thenReturn(Optional.of(f));

        AsientoGenerado asientoGenerado = new AsientoGenerado(f.getFecha(), "desc", "FACTURA_VENTA", List.of(), "FacturaVenta", 50L);
        Asiento asientoPersistido = new Asiento();
        asientoPersistido.setId(999L);
        asientoPersistido.setNumero(7L);
        when(generator.generar(f)).thenReturn(asientoGenerado);
        when(asientoService.registrarAutomatico(asientoGenerado)).thenReturn(asientoPersistido);

        FacturaVenta confirmada = service.confirmar(50L);

        assertThat(confirmada.getEstado()).isEqualTo(EstadoDocumento.CONFIRMADO);
        assertThat(confirmada.getAsiento()).isSameAs(asientoPersistido);
        verify(generator).generar(f);
        verify(asientoService).registrarAutomatico(asientoGenerado);
    }

    @Test
    void confirmarUnaFacturaYaConfirmadaLanzaTransicionInvalida() {
        FacturaVenta f = new FacturaVenta();
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

        FacturaVenta f = new FacturaVenta();
        f.setId(52L);
        f.setEstado(EstadoDocumento.CONFIRMADO);
        f.setAsiento(asiento);
        when(repo.findById(52L)).thenReturn(Optional.of(f));

        FacturaVenta anulada = service.anular(52L, "factura duplicada");

        assertThat(anulada.getEstado()).isEqualTo(EstadoDocumento.ANULADO);
        verify(asientoService).anularPorDocumento(999L, "factura duplicada");
    }

    @Test
    void anularUnBorradorLanzaTransicionInvalida() {
        FacturaVenta f = new FacturaVenta();
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
        FacturaVenta f = new FacturaVenta();
        f.setId(54L);
        f.setEstado(EstadoDocumento.CONFIRMADO);
        when(repo.findById(54L)).thenReturn(Optional.of(f));

        assertThatThrownBy(() -> service.eliminarBorrador(54L))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("TRANSICION_ESTADO_INVALIDA");
    }
}

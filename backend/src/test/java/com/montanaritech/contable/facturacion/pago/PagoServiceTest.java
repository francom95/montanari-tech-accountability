package com.montanaritech.contable.facturacion.pago;

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
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompra;
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompraRepository;
import com.montanaritech.contable.facturacion.pago.dto.AplicarAnticipoProveedorRequest;
import com.montanaritech.contable.facturacion.pago.dto.PagoCrearRequest;
import com.montanaritech.contable.facturacion.pago.dto.PagoImputacionRequest;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancariaRepository;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.proveedor.ProveedorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Ciclo de vida de pago: cálculo de totales, confirmación, anulación y aplicación de anticipo. */
@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock private PagoRepository repo;
    @Mock private PagoImputacionRepository pagoImputacionRepo;
    @Mock private AplicacionAnticipoProveedorRepository aplicacionAnticipoRepo;
    @Mock private PagoMapper mapper;
    @Mock private AuditoriaService auditoria;
    @Mock private AsientoService asientoService;
    @Mock private PagoAsientoGenerator generator;
    @Mock private ProveedorRepository proveedorRepo;
    @Mock private MonedaRepository monedaRepo;
    @Mock private CuentaBancariaRepository cuentaBancariaRepo;
    @Mock private FacturaCompraRepository facturaCompraRepo;

    private PagoService service;
    private Proveedor proveedor;
    private Moneda ars;
    private CuentaBancaria banco;

    @BeforeEach
    void setUp() {
        service = new PagoService(repo, pagoImputacionRepo, aplicacionAnticipoRepo, mapper, auditoria,
                asientoService, generator, proveedorRepo, monedaRepo, cuentaBancariaRepo, facturaCompraRepo);

        proveedor = new Proveedor();
        proveedor.setId(1L);
        proveedor.setNombre("Proveedor Test");

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        banco = new CuentaBancaria();
        banco.setId(1L);

        lenient().when(proveedorRepo.findById(1L)).thenReturn(Optional.of(proveedor));
        lenient().when(monedaRepo.findById(1L)).thenReturn(Optional.of(ars));
        lenient().when(cuentaBancariaRepo.findById(1L)).thenReturn(Optional.of(banco));
        lenient().when(repo.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(50L);
            }
            return p;
        });
        lenient().when(aplicacionAnticipoRepo.findByPago_IdOrderByIdAsc(any())).thenReturn(List.of());
    }

    private PagoCrearRequest requestSinLineas(BigDecimal total) {
        return new PagoCrearRequest(1L, LocalDate.of(2026, 6, 15), 1L, new BigDecimal("1.000000"), 1L, total, null, null);
    }

    @Test
    void crearBorradorSinImputacionesEsAnticipoPuro() {
        Pago creado = service.crearBorrador(requestSinLineas(new BigDecimal("500.00")));

        assertThat(creado.getEstado()).isEqualTo(EstadoDocumento.BORRADOR);
        assertThat(creado.getTotal()).isEqualByComparingTo("500.00");
        assertThat(creado.getTotalArs()).isEqualByComparingTo("500.00");
        assertThat(creado.getLineas()).isEmpty();
    }

    @Test
    void crearBorradorConImputacionesQueExcedenTotalLanzaError() {
        FacturaCompra f = new FacturaCompra();
        f.setId(10L);
        when(facturaCompraRepo.findById(10L)).thenReturn(Optional.of(f));

        PagoCrearRequest req = new PagoCrearRequest(1L, LocalDate.of(2026, 6, 15), 1L, new BigDecimal("1.000000"), 1L,
                new BigDecimal("100.00"), null, List.of(new PagoImputacionRequest(10L, new BigDecimal("150.00"))));

        assertThatThrownBy(() -> service.crearBorrador(req))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("IMPUTACIONES_EXCEDEN_TOTAL_PAGADO");
    }

    @Test
    void confirmarLlamaAlGeneradorYRegistraElAsientoAutomatico() {
        Pago p = service.crearBorrador(requestSinLineas(new BigDecimal("500.00")));
        when(repo.findById(50L)).thenReturn(Optional.of(p));

        AsientoGenerado asientoGenerado = new AsientoGenerado(p.getFecha(), "desc", "PAGO", List.of(), "Pago", 50L);
        Asiento asientoPersistido = new Asiento();
        asientoPersistido.setId(999L);
        asientoPersistido.setNumero(7L);
        when(generator.generar(p)).thenReturn(asientoGenerado);
        when(asientoService.registrarAutomatico(asientoGenerado)).thenReturn(asientoPersistido);

        Pago confirmado = service.confirmar(50L);

        assertThat(confirmado.getEstado()).isEqualTo(EstadoDocumento.CONFIRMADO);
        assertThat(confirmado.getAsiento()).isSameAs(asientoPersistido);
        verify(generator).generar(p);
        verify(asientoService).registrarAutomatico(asientoGenerado);
    }

    @Test
    void confirmarUnPagoYaConfirmadoLanzaTransicionInvalida() {
        Pago p = new Pago();
        p.setId(51L);
        p.setEstado(EstadoDocumento.CONFIRMADO);
        when(repo.findById(51L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.confirmar(51L))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("TRANSICION_ESTADO_INVALIDA");
    }

    @Test
    void anularUnPagoConfirmadoAnulaSuAsientoVinculado() {
        Asiento asiento = new Asiento();
        asiento.setId(999L);

        Pago p = new Pago();
        p.setId(52L);
        p.setEstado(EstadoDocumento.CONFIRMADO);
        p.setAsiento(asiento);
        when(repo.findById(52L)).thenReturn(Optional.of(p));

        Pago anulado = service.anular(52L, "pago duplicado");

        assertThat(anulado.getEstado()).isEqualTo(EstadoDocumento.ANULADO);
        verify(asientoService).anularPorDocumento(999L, "pago duplicado");
    }

    @Test
    void eliminarUnPagoConfirmadoLanzaTransicionInvalida() {
        Pago p = new Pago();
        p.setId(54L);
        p.setEstado(EstadoDocumento.CONFIRMADO);
        when(repo.findById(54L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.eliminarBorrador(54L))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("TRANSICION_ESTADO_INVALIDA");
    }

    @Test
    void aplicarAnticipoLlamaAlGeneradorYRegistraElAsientoDeAjuste() {
        Pago anticipo = new Pago();
        anticipo.setId(60L);
        anticipo.setEstado(EstadoDocumento.CONFIRMADO);
        anticipo.setMontoAnticipo(new BigDecimal("500.00"));
        when(repo.findById(60L)).thenReturn(Optional.of(anticipo));

        FacturaCompra factura = new FacturaCompra();
        factura.setId(70L);
        when(facturaCompraRepo.findById(70L)).thenReturn(Optional.of(factura));

        AsientoGenerado ajusteGenerado = new AsientoGenerado(LocalDate.of(2026, 7, 1), "ajuste", "AJUSTE", List.of(), "Pago", 60L);
        var resultado = new PagoAsientoGenerator.ResultadoAjusteAnticipo(ajusteGenerado, new BigDecimal("780000.00"));
        when(generator.generarAjusteAplicacionAnticipo(anticipo, factura, new BigDecimal("500.00"), LocalDate.of(2026, 7, 1)))
                .thenReturn(resultado);

        Asiento asientoAjuste = new Asiento();
        asientoAjuste.setId(888L);
        when(asientoService.registrarAutomatico(ajusteGenerado)).thenReturn(asientoAjuste);
        when(aplicacionAnticipoRepo.save(any(AplicacionAnticipoProveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        AplicacionAnticipoProveedor aplicacion = service.aplicarAnticipo(60L,
                new AplicarAnticipoProveedorRequest(70L, new BigDecimal("500.00"), LocalDate.of(2026, 7, 1)));

        assertThat(aplicacion.getMontoOriginal()).isEqualByComparingTo("500.00");
        assertThat(aplicacion.getMontoArsCancelado()).isEqualByComparingTo("780000.00");
        assertThat(aplicacion.getAsiento()).isSameAs(asientoAjuste);
    }

    @Test
    void aplicarAnticipoQueSuperaElDisponibleLanzaError() {
        Pago anticipo = new Pago();
        anticipo.setId(61L);
        anticipo.setEstado(EstadoDocumento.CONFIRMADO);
        anticipo.setMontoAnticipo(new BigDecimal("100.00"));
        when(repo.findById(61L)).thenReturn(Optional.of(anticipo));

        assertThatThrownBy(() -> service.aplicarAnticipo(61L,
                new AplicarAnticipoProveedorRequest(70L, new BigDecimal("200.00"), LocalDate.of(2026, 7, 1))))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("ANTICIPO_INSUFICIENTE");
    }
}

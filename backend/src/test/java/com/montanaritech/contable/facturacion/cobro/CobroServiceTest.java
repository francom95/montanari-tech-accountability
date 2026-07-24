package com.montanaritech.contable.facturacion.cobro;

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
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTributoRepository;
import com.montanaritech.contable.facturacion.cobro.dto.AplicarAnticipoRequest;
import com.montanaritech.contable.facturacion.cobro.dto.CobroCrearRequest;
import com.montanaritech.contable.facturacion.cobro.dto.CobroImputacionRequest;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVentaRepository;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.cliente.ClienteRepository;
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

/** Ciclo de vida de cobro: cálculo de totales, confirmación, anulación y aplicación de anticipo. */
@ExtendWith(MockitoExtension.class)
class CobroServiceTest {

    @Mock private CobroRepository repo;
    @Mock private CobroImputacionRepository cobroImputacionRepo;
    @Mock private AplicacionAnticipoClienteRepository aplicacionAnticipoRepo;
    @Mock private CobroMapper mapper;
    @Mock private AuditoriaService auditoria;
    @Mock private AsientoService asientoService;
    @Mock private CobroAsientoGenerator generator;
    @Mock private ClienteRepository clienteRepo;
    @Mock private MonedaRepository monedaRepo;
    @Mock private CuentaBancariaRepository cuentaBancariaRepo;
    @Mock private FacturaVentaRepository facturaVentaRepo;
    @Mock private ComprobanteTributoRepository comprobanteTributoRepo;

    private CobroService service;
    private Cliente cliente;
    private Moneda ars;
    private CuentaBancaria banco;

    @BeforeEach
    void setUp() {
        service = new CobroService(repo, cobroImputacionRepo, aplicacionAnticipoRepo, mapper, auditoria,
                asientoService, generator, clienteRepo, monedaRepo, cuentaBancariaRepo, facturaVentaRepo, comprobanteTributoRepo);

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Cliente Test");

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        banco = new CuentaBancaria();
        banco.setId(1L);

        lenient().when(clienteRepo.findById(1L)).thenReturn(Optional.of(cliente));
        lenient().when(monedaRepo.findById(1L)).thenReturn(Optional.of(ars));
        lenient().when(cuentaBancariaRepo.findById(1L)).thenReturn(Optional.of(banco));
        lenient().when(repo.save(any(Cobro.class))).thenAnswer(inv -> {
            Cobro c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(50L);
            }
            return c;
        });
        lenient().when(aplicacionAnticipoRepo.findByCobro_IdOrderByIdAsc(any())).thenReturn(List.of());
    }

    private CobroCrearRequest requestSinLineas(BigDecimal total) {
        return new CobroCrearRequest(1L, LocalDate.of(2026, 6, 15), 1L, new BigDecimal("1.000000"), 1L, total, null, null, null);
    }

    @Test
    void crearBorradorSinImputacionesEsAnticipoPuro() {
        Cobro creado = service.crearBorrador(requestSinLineas(new BigDecimal("500.00")));

        assertThat(creado.getEstado()).isEqualTo(EstadoDocumento.BORRADOR);
        assertThat(creado.getTotal()).isEqualByComparingTo("500.00");
        assertThat(creado.getTotalArs()).isEqualByComparingTo("500.00");
        assertThat(creado.getLineas()).isEmpty();
    }

    @Test
    void crearBorradorConImputacionesQueExcedenTotalLanzaError() {
        FacturaVenta f = new FacturaVenta();
        f.setId(10L);
        when(facturaVentaRepo.findById(10L)).thenReturn(Optional.of(f));

        CobroCrearRequest req = new CobroCrearRequest(1L, LocalDate.of(2026, 6, 15), 1L, new BigDecimal("1.000000"), 1L,
                new BigDecimal("100.00"), null, List.of(new CobroImputacionRequest(10L, new BigDecimal("150.00"), null)), null);

        assertThatThrownBy(() -> service.crearBorrador(req))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("IMPUTACIONES_EXCEDEN_TOTAL_COBRADO");
    }

    @Test
    void confirmarLlamaAlGeneradorYRegistraElAsientoAutomatico() {
        Cobro c = service.crearBorrador(requestSinLineas(new BigDecimal("500.00")));
        when(repo.findById(50L)).thenReturn(Optional.of(c));

        AsientoGenerado asientoGenerado = new AsientoGenerado(c.getFecha(), "desc", "COBRO", List.of(), "Cobro", 50L);
        Asiento asientoPersistido = new Asiento();
        asientoPersistido.setId(999L);
        asientoPersistido.setNumero(7L);
        when(generator.generar(c)).thenReturn(asientoGenerado);
        when(asientoService.registrarAutomatico(asientoGenerado)).thenReturn(asientoPersistido);

        Cobro confirmado = service.confirmar(50L);

        assertThat(confirmado.getEstado()).isEqualTo(EstadoDocumento.CONFIRMADO);
        assertThat(confirmado.getAsiento()).isSameAs(asientoPersistido);
        verify(generator).generar(c);
        verify(asientoService).registrarAutomatico(asientoGenerado);
    }

    @Test
    void confirmarUnCobroYaConfirmadoLanzaTransicionInvalida() {
        Cobro c = new Cobro();
        c.setId(51L);
        c.setEstado(EstadoDocumento.CONFIRMADO);
        when(repo.findById(51L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.confirmar(51L))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("TRANSICION_ESTADO_INVALIDA");
    }

    @Test
    void anularUnCobroConfirmadoAnulaSuAsientoVinculado() {
        Asiento asiento = new Asiento();
        asiento.setId(999L);

        Cobro c = new Cobro();
        c.setId(52L);
        c.setEstado(EstadoDocumento.CONFIRMADO);
        c.setAsiento(asiento);
        when(repo.findById(52L)).thenReturn(Optional.of(c));

        Cobro anulado = service.anular(52L, "cobro duplicado");

        assertThat(anulado.getEstado()).isEqualTo(EstadoDocumento.ANULADO);
        verify(asientoService).anularPorDocumento(999L, "cobro duplicado");
    }

    @Test
    void anularUnCobroConAplicacionesDeAnticipoLanzaError() {
        Cobro c = new Cobro();
        c.setId(53L);
        c.setEstado(EstadoDocumento.CONFIRMADO);
        when(repo.findById(53L)).thenReturn(Optional.of(c));

        AplicacionAnticipoCliente aplicacion = new AplicacionAnticipoCliente();
        aplicacion.setMontoOriginal(new BigDecimal("100.00"));
        when(aplicacionAnticipoRepo.findByCobro_IdOrderByIdAsc(53L)).thenReturn(List.of(aplicacion));

        assertThatThrownBy(() -> service.anular(53L, "motivo"))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("COBRO_CON_APLICACIONES_DE_ANTICIPO");
    }

    @Test
    void eliminarUnCobroConfirmadoLanzaTransicionInvalida() {
        Cobro c = new Cobro();
        c.setId(54L);
        c.setEstado(EstadoDocumento.CONFIRMADO);
        when(repo.findById(54L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.eliminarBorrador(54L))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("TRANSICION_ESTADO_INVALIDA");
    }

    @Test
    void aplicarAnticipoLlamaAlGeneradorYRegistraElAsientoDeAjuste() {
        Cobro anticipo = new Cobro();
        anticipo.setId(60L);
        anticipo.setEstado(EstadoDocumento.CONFIRMADO);
        anticipo.setMontoAnticipo(new BigDecimal("500.00"));
        when(repo.findById(60L)).thenReturn(Optional.of(anticipo));

        FacturaVenta factura = new FacturaVenta();
        factura.setId(70L);
        when(facturaVentaRepo.findById(70L)).thenReturn(Optional.of(factura));

        AsientoGenerado ajusteGenerado = new AsientoGenerado(LocalDate.of(2026, 7, 1), "ajuste", "AJUSTE", List.of(), "Cobro", 60L);
        var resultado = new CobroAsientoGenerator.ResultadoAjusteAnticipo(ajusteGenerado, new BigDecimal("780000.00"));
        when(generator.generarAjusteAplicacionAnticipo(anticipo, factura, new BigDecimal("500.00"), LocalDate.of(2026, 7, 1)))
                .thenReturn(resultado);

        Asiento asientoAjuste = new Asiento();
        asientoAjuste.setId(888L);
        when(asientoService.registrarAutomatico(ajusteGenerado)).thenReturn(asientoAjuste);
        when(aplicacionAnticipoRepo.save(any(AplicacionAnticipoCliente.class))).thenAnswer(inv -> inv.getArgument(0));

        AplicacionAnticipoCliente aplicacion = service.aplicarAnticipo(60L,
                new AplicarAnticipoRequest(70L, new BigDecimal("500.00"), LocalDate.of(2026, 7, 1)));

        assertThat(aplicacion.getMontoOriginal()).isEqualByComparingTo("500.00");
        assertThat(aplicacion.getMontoArsCancelado()).isEqualByComparingTo("780000.00");
        assertThat(aplicacion.getAsiento()).isSameAs(asientoAjuste);
    }

    @Test
    void aplicarAnticipoQueSuperaElDisponibleLanzaError() {
        Cobro anticipo = new Cobro();
        anticipo.setId(61L);
        anticipo.setEstado(EstadoDocumento.CONFIRMADO);
        anticipo.setMontoAnticipo(new BigDecimal("100.00"));
        when(repo.findById(61L)).thenReturn(Optional.of(anticipo));

        assertThatThrownBy(() -> service.aplicarAnticipo(61L,
                new AplicarAnticipoRequest(70L, new BigDecimal("200.00"), LocalDate.of(2026, 7, 1))))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("ANTICIPO_INSUFICIENTE");
    }
}

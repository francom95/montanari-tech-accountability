package com.montanaritech.contable.facturacion.cobro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.asiento.AsientoGenerado;
import com.montanaritech.contable.common.asiento.LineaAsientoGenerada;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.common.estado.EstadoDocumento;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ResolutorCuentas;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTipo;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTributo;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTributoRepository;
import com.montanaritech.contable.facturacion.comprobantetributo.TipoTributo;
import com.montanaritech.contable.facturacion.facturaventa.FacturaVenta;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Reproduce los casos numéricos de F4.1 §7 para cobro: CO-1..CO-5. CO-3
 * verifica la regla del residuo (F3.1 §6.3) en dos imputaciones parciales
 * sucesivas con TC distinto; CO-5 verifica la aplicación posterior de un
 * anticipo (asiento AJUSTE, sin editar el asiento original del cobro).
 */
@ExtendWith(MockitoExtension.class)
class CobroAsientoGeneratorTest {

    @Mock private ResolutorCuentas resolutorCuentas;
    @Mock private ComprobanteTributoRepository comprobanteTributoRepo;
    @Mock private CobroImputacionRepository cobroImputacionRepo;
    @Mock private AplicacionAnticipoClienteRepository aplicacionAnticipoRepo;
    @Mock private MonedaRepository monedaRepo;

    private CobroAsientoGenerator generator;
    private Cliente cliente;
    private CuentaContable cuentaCxc;
    private CuentaContable cuentaBancoArs;
    private CuentaContable cuentaBancoUsd;
    private CuentaContable cuentaDifGanada;
    private CuentaContable cuentaDifPerdida;
    private CuentaContable cuentaAnticipoCliente;
    private CuentaContable cuentaRetencionGanancias;
    private CuentaBancaria bancoArs;
    private CuentaBancaria bancoUsd;
    private Moneda ars;
    private Moneda usd;

    @BeforeEach
    void setUp() {
        generator = new CobroAsientoGenerator(resolutorCuentas, comprobanteTributoRepo, cobroImputacionRepo, aplicacionAnticipoRepo, monedaRepo);

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");
        lenient().when(monedaRepo.findByCodigo("ARS")).thenReturn(java.util.Optional.of(ars));

        usd = new Moneda();
        usd.setId(2L);
        usd.setCodigo("USD");

        cuentaCxc = cuenta(3L, "1.1.2004.01");
        cuentaBancoArs = cuenta(10L, "1.1.2001");
        cuentaBancoUsd = cuenta(11L, "1.1.2002");
        cuentaDifGanada = cuenta(12L, "6.4005");
        cuentaDifPerdida = cuenta(13L, "6.4006");
        cuentaAnticipoCliente = cuenta(14L, "2.1.2018");
        cuentaRetencionGanancias = cuenta(15L, "1.1.2011");

        bancoArs = new CuentaBancaria();
        bancoArs.setId(100L);
        bancoArs.setCuentaContable(cuentaBancoArs);

        bancoUsd = new CuentaBancaria();
        bancoUsd.setId(101L);
        bancoUsd.setCuentaContable(cuentaBancoUsd);

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Valvecchia Gerardo");
        cliente.setCuentaCxc(cuentaCxc);

        lenient().when(comprobanteTributoRepo.findByComprobanteTipoAndComprobanteIdOrderByIdAsc(eq(ComprobanteTipo.COBRO), any()))
                .thenReturn(List.of());
        lenient().when(cobroImputacionRepo.findByFacturaVenta_IdAndCobro_EstadoOrderByIdAsc(any(), eq(EstadoDocumento.CONFIRMADO)))
                .thenReturn(List.of());
        lenient().when(aplicacionAnticipoRepo.findByFacturaVenta_IdOrderByIdAsc(any())).thenReturn(List.of());
    }

    private CuentaContable cuenta(Long id, String codigo) {
        CuentaContable c = new CuentaContable();
        c.setId(id);
        c.setCodigo(codigo);
        c.setImputable(true);
        c.setActivo(true);
        return c;
    }

    private FacturaVenta factura(Long id, Moneda moneda, BigDecimal tc, BigDecimal total, BigDecimal totalArs) {
        FacturaVenta f = new FacturaVenta();
        f.setId(id);
        f.setNumero("00001-" + id);
        f.setCliente(cliente);
        f.setMoneda(moneda);
        f.setTipoCambio(tc);
        f.setTotal(total);
        f.setTotalArs(totalArs);
        f.setEstado(EstadoDocumento.CONFIRMADO);
        return f;
    }

    private Cobro cobro(Long id, Moneda moneda, BigDecimal tc, CuentaBancaria banco, BigDecimal total, CobroImputacion... imputaciones) {
        Cobro c = new Cobro();
        c.setId(id);
        c.setCliente(cliente);
        c.setFecha(LocalDate.of(2026, 6, 15));
        c.setMoneda(moneda);
        c.setTipoCambio(tc);
        c.setCuentaBancaria(banco);
        c.setTotal(total);
        for (CobroImputacion imp : imputaciones) {
            imp.setCobro(c);
            c.getLineas().add(imp);
        }
        return c;
    }

    private CobroImputacion imputacion(FacturaVenta factura, BigDecimal monto) {
        CobroImputacion i = new CobroImputacion();
        i.setFacturaVenta(factura);
        i.setMontoImputadoOriginal(monto);
        return i;
    }

    private CobroImputacion previaConfirmada(BigDecimal montoOriginal, BigDecimal montoArs) {
        CobroImputacion i = new CobroImputacion();
        i.setMontoImputadoOriginal(montoOriginal);
        i.setMontoArsCancelado(montoArs);
        return i;
    }

    private void assertBalancea(AsientoGenerado generado) {
        BigDecimal debe = generado.lineas().stream().map(LineaAsientoGenerada::debe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal haber = generado.lineas().stream().map(LineaAsientoGenerada::haber).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debe).isEqualByComparingTo(haber);
    }

    // ---- CO-1 (=CP-07): USD, mismo TC — sin diferencia ----

    @Test
    void co1_usdMismoTcSinDiferencia() {
        when(resolutorCuentas.resolver(ConceptoContable.CREDITO_POR_VENTA)).thenReturn(cuentaCxc);
        FacturaVenta f = factura(10L, usd, new BigDecimal("1500.000000"), new BigDecimal("1000.00"), new BigDecimal("1500000.00"));
        cliente.setCuentaCxc(null);

        Cobro c = cobro(1L, usd, new BigDecimal("1500.000000"), bancoUsd, new BigDecimal("1000.00"),
                imputacion(f, new BigDecimal("1000.00")));

        AsientoGenerado generado = generator.generar(c);

        assertThat(generado.lineas()).hasSize(2);
        assertThat(generado.lineas().get(0).cuentaCodigo()).isEqualTo("1.1.2002");
        assertThat(generado.lineas().get(0).debe()).isEqualByComparingTo("1500000.00");
        assertThat(generado.lineas().get(1).cuentaCodigo()).isEqualTo("1.1.2004.01");
        assertThat(generado.lineas().get(1).haber()).isEqualByComparingTo("1500000.00");
        assertBalancea(generado);
    }

    // ---- CO-2 (=CP-08): USD, TC mayor — ganancia ----

    @Test
    void co2_usdTcMayorGanancia() {
        when(resolutorCuentas.resolver(ConceptoContable.DIF_CAMBIO_GANADA)).thenReturn(cuentaDifGanada);
        FacturaVenta f = factura(11L, usd, new BigDecimal("1500.000000"), new BigDecimal("1000.00"), new BigDecimal("1500000.00"));

        Cobro c = cobro(2L, usd, new BigDecimal("1550.000000"), bancoUsd, new BigDecimal("1000.00"),
                imputacion(f, new BigDecimal("1000.00")));

        AsientoGenerado generado = generator.generar(c);

        assertThat(generado.lineas()).hasSize(3);
        assertThat(generado.lineas().get(0).debe()).isEqualByComparingTo("1550000.00");
        assertThat(generado.lineas().get(1).cuentaCodigo()).isEqualTo("1.1.2004.01");
        assertThat(generado.lineas().get(1).haber()).isEqualByComparingTo("1500000.00");
        assertThat(generado.lineas().get(2).cuentaCodigo()).isEqualTo("6.4005");
        assertThat(generado.lineas().get(2).haber()).isEqualByComparingTo("50000.00");
        assertBalancea(generado);
    }

    // ---- CO-3 (=CP-09): dos cobros parciales con TC distinto + regla del residuo ----

    @Test
    void co3_primerCobroParcialGanancia() {
        when(resolutorCuentas.resolver(ConceptoContable.DIF_CAMBIO_GANADA)).thenReturn(cuentaDifGanada);
        FacturaVenta f = factura(12L, usd, new BigDecimal("1500.000000"), new BigDecimal("1000.00"), new BigDecimal("1500000.00"));

        Cobro c = cobro(3L, usd, new BigDecimal("1520.000000"), bancoUsd, new BigDecimal("400.00"),
                imputacion(f, new BigDecimal("400.00")));

        AsientoGenerado generado = generator.generar(c);

        assertThat(generado.lineas().get(0).debe()).isEqualByComparingTo("608000.00");
        assertThat(generado.lineas().get(1).haber()).isEqualByComparingTo("600000.00");
        assertThat(generado.lineas().get(2).haber()).isEqualByComparingTo("8000.00");
        assertBalancea(generado);
    }

    @Test
    void co3_segundoCobroCierraSaldoConResiduoYPerdida() {
        when(resolutorCuentas.resolver(ConceptoContable.DIF_CAMBIO_PERDIDA)).thenReturn(cuentaDifPerdida);
        FacturaVenta f = factura(13L, usd, new BigDecimal("1500.000000"), new BigDecimal("1000.00"), new BigDecimal("1500000.00"));

        when(cobroImputacionRepo.findByFacturaVenta_IdAndCobro_EstadoOrderByIdAsc(eq(13L), eq(EstadoDocumento.CONFIRMADO)))
                .thenReturn(List.of(previaConfirmada(new BigDecimal("400.00"), new BigDecimal("600000.00"))));

        Cobro c = cobro(4L, usd, new BigDecimal("1490.000000"), bancoUsd, new BigDecimal("600.00"),
                imputacion(f, new BigDecimal("600.00")));

        AsientoGenerado generado = generator.generar(c);

        assertThat(generado.lineas().get(0).debe()).isEqualByComparingTo("894000.00");
        assertThat(generado.lineas().get(1).cuentaCodigo()).isEqualTo("1.1.2004.01");
        assertThat(generado.lineas().get(1).haber()).isEqualByComparingTo("900000.00");
        assertThat(generado.lineas().get(2).cuentaCodigo()).isEqualTo("6.4006");
        assertThat(generado.lineas().get(2).debe()).isEqualByComparingTo("6000.00");
        assertBalancea(generado);
    }

    // ---- CO-4: ARS con retención de Ganancias sufrida ----

    @Test
    void co4_arsConRetencionDeGananciasSufrida() {
        when(resolutorCuentas.resolver(ConceptoContable.RETENCION_GANANCIAS_SUFRIDA)).thenReturn(cuentaRetencionGanancias);
        FacturaVenta f = factura(14L, ars, new BigDecimal("1.000000"), new BigDecimal("121000.00"), new BigDecimal("121000.00"));

        Cobro c = cobro(5L, ars, new BigDecimal("1.000000"), bancoArs, new BigDecimal("121000.00"),
                imputacion(f, new BigDecimal("121000.00")));

        ComprobanteTributo retencion = new ComprobanteTributo();
        retencion.setTipo(TipoTributo.RETENCION_GANANCIAS);
        retencion.setImporte(new BigDecimal("2000.00"));
        when(comprobanteTributoRepo.findByComprobanteTipoAndComprobanteIdOrderByIdAsc(ComprobanteTipo.COBRO, 5L))
                .thenReturn(List.of(retencion));

        AsientoGenerado generado = generator.generar(c);

        assertThat(generado.lineas()).hasSize(3);
        assertThat(generado.lineas().get(0).cuentaCodigo()).isEqualTo("1.1.2001");
        assertThat(generado.lineas().get(0).debe()).isEqualByComparingTo("119000.00");
        assertThat(generado.lineas().get(1).cuentaCodigo()).isEqualTo("1.1.2011");
        assertThat(generado.lineas().get(1).debe()).isEqualByComparingTo("2000.00");
        assertThat(generado.lineas().get(2).haber()).isEqualByComparingTo("121000.00");
        assertBalancea(generado);
    }

    // ---- CO-5: cobro 100% anticipo (sin facturas) ----

    @Test
    void co5_cobroSinFacturasEsAnticipoPuro() {
        when(resolutorCuentas.resolver(ConceptoContable.ANTICIPO_CLIENTE)).thenReturn(cuentaAnticipoCliente);

        Cobro c = cobro(6L, usd, new BigDecimal("1500.000000"), bancoUsd, new BigDecimal("500.00"));

        AsientoGenerado generado = generator.generar(c);

        assertThat(generado.lineas()).hasSize(2);
        assertThat(generado.lineas().get(0).debe()).isEqualByComparingTo("750000.00");
        assertThat(generado.lineas().get(1).cuentaCodigo()).isEqualTo("2.1.2018");
        assertThat(generado.lineas().get(1).haber()).isEqualByComparingTo("750000.00");
        assertThat(c.getMontoAnticipo()).isEqualByComparingTo("500.00");
        assertBalancea(generado);
    }

    // ---- CO-5: aplicación posterior del anticipo (asiento AJUSTE) ----

    @Test
    void co5_aplicacionPosteriorDeAnticipoGeneraAjusteConDifCambioPerdida() {
        when(resolutorCuentas.resolver(ConceptoContable.ANTICIPO_CLIENTE)).thenReturn(cuentaAnticipoCliente);
        when(resolutorCuentas.resolver(ConceptoContable.DIF_CAMBIO_PERDIDA)).thenReturn(cuentaDifPerdida);

        Cobro anticipo = cobro(6L, usd, new BigDecimal("1500.000000"), bancoUsd, new BigDecimal("500.00"));
        anticipo.setMontoAnticipo(new BigDecimal("500.00"));
        FacturaVenta f = factura(15L, usd, new BigDecimal("1560.000000"), new BigDecimal("500.00"), new BigDecimal("780000.00"));

        var resultado = generator.generarAjusteAplicacionAnticipo(anticipo, f, new BigDecimal("500.00"), LocalDate.of(2026, 7, 1));

        assertThat(resultado.asientoGenerado().origen()).isEqualTo("AJUSTE");
        assertThat(resultado.asientoGenerado().documentoOrigenTipo()).isEqualTo("Cobro");
        assertThat(resultado.asientoGenerado().documentoOrigenId()).isEqualTo(6L);
        List<LineaAsientoGenerada> lineas = resultado.asientoGenerado().lineas();
        assertThat(lineas.get(0).cuentaCodigo()).isEqualTo("2.1.2018");
        assertThat(lineas.get(0).debe()).isEqualByComparingTo("750000.00");
        assertThat(lineas.get(1).cuentaCodigo()).isEqualTo("6.4006");
        assertThat(lineas.get(1).debe()).isEqualByComparingTo("30000.00");
        assertThat(lineas.get(2).cuentaCodigo()).isEqualTo("1.1.2004.01");
        assertThat(lineas.get(2).haber()).isEqualByComparingTo("780000.00");
        assertThat(resultado.montoArsCancelado()).isEqualByComparingTo("780000.00");

        BigDecimal debe = lineas.stream().map(LineaAsientoGenerada::debe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal haber = lineas.stream().map(LineaAsientoGenerada::haber).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debe).isEqualByComparingTo(haber);
    }

    // ---- Validaciones ----

    @Test
    void imputacionQueSuperaElSaldoLanzaError() {
        FacturaVenta f = factura(16L, usd, new BigDecimal("1500.000000"), new BigDecimal("1000.00"), new BigDecimal("1500000.00"));
        Cobro c = cobro(7L, usd, new BigDecimal("1500.000000"), bancoUsd, new BigDecimal("1200.00"),
                imputacion(f, new BigDecimal("1200.00")));

        assertThatThrownBy(() -> generator.generar(c))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("IMPUTACION_EXCEDE_SALDO");
    }

    @Test
    void imputacionContraFacturaEnOtraMonedaLanzaError() {
        FacturaVenta f = factura(17L, ars, new BigDecimal("1.000000"), new BigDecimal("1000.00"), new BigDecimal("1000.00"));
        Cobro c = cobro(8L, usd, new BigDecimal("1500.000000"), bancoUsd, new BigDecimal("500.00"),
                imputacion(f, new BigDecimal("500.00")));

        assertThatThrownBy(() -> generator.generar(c))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("IMPUTACION_MONEDA_NO_COINCIDE");
    }

    @Test
    void tributoNoAplicableAlCobroLanzaError() {
        FacturaVenta f = factura(18L, ars, new BigDecimal("1.000000"), new BigDecimal("1000.00"), new BigDecimal("1000.00"));
        Cobro c = cobro(9L, ars, new BigDecimal("1.000000"), bancoArs, new BigDecimal("1000.00"),
                imputacion(f, new BigDecimal("1000.00")));

        ComprobanteTributo percepcion = new ComprobanteTributo();
        percepcion.setTipo(TipoTributo.PERCEPCION_IVA);
        percepcion.setImporte(new BigDecimal("10.00"));
        when(comprobanteTributoRepo.findByComprobanteTipoAndComprobanteIdOrderByIdAsc(ComprobanteTipo.COBRO, 9L))
                .thenReturn(List.of(percepcion));

        assertThatThrownBy(() -> generator.generar(c))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("TRIBUTO_NO_APLICABLE_A_COBRO");
    }

    @Test
    void cobroSinImporteLanzaError() {
        Cobro c = new Cobro();
        c.setId(99L);
        c.setCliente(cliente);
        c.setMoneda(ars);
        c.setTipoCambio(BigDecimal.ONE);
        c.setCuentaBancaria(bancoArs);
        c.setTotal(BigDecimal.ZERO);

        assertThatThrownBy(() -> generator.generar(c))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("COBRO_SIN_IMPORTE");
    }
}

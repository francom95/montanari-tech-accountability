package com.montanaritech.contable.facturacion.pago;

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
import com.montanaritech.contable.facturacion.facturacompra.FacturaCompra;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.moneda.MonedaRepository;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Reproduce los casos numéricos de F4.1 §7 para pago: PA-1..PA-3. Mismo
 * {@code CalculoImputacion} que cobro, con el signo de diferencia de cambio
 * invertido (F4.1 §6.3: TC subió ⇒ pérdida en un pago).
 */
@ExtendWith(MockitoExtension.class)
class PagoAsientoGeneratorTest {

    @Mock private ResolutorCuentas resolutorCuentas;
    @Mock private PagoImputacionRepository pagoImputacionRepo;
    @Mock private AplicacionAnticipoProveedorRepository aplicacionAnticipoRepo;
    @Mock private MonedaRepository monedaRepo;

    private PagoAsientoGenerator generator;
    private Proveedor proveedor;
    private CuentaContable cuentaCxp;
    private CuentaContable cuentaBancoArs;
    private CuentaContable cuentaBancoUsd;
    private CuentaContable cuentaDifGanada;
    private CuentaContable cuentaDifPerdida;
    private CuentaBancaria bancoArs;
    private CuentaBancaria bancoUsd;
    private Moneda ars;
    private Moneda usd;

    @BeforeEach
    void setUp() {
        generator = new PagoAsientoGenerator(resolutorCuentas, pagoImputacionRepo, aplicacionAnticipoRepo, monedaRepo);

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");
        lenient().when(monedaRepo.findByCodigo("ARS")).thenReturn(java.util.Optional.of(ars));

        usd = new Moneda();
        usd.setId(2L);
        usd.setCodigo("USD");

        cuentaCxp = cuenta(3L, "2.1.2002");
        cuentaBancoArs = cuenta(10L, "1.1.2001");
        cuentaBancoUsd = cuenta(11L, "1.1.2002");
        cuentaDifGanada = cuenta(12L, "6.4005");
        cuentaDifPerdida = cuenta(13L, "6.4006");

        bancoArs = new CuentaBancaria();
        bancoArs.setId(100L);
        bancoArs.setCuentaContable(cuentaBancoArs);

        bancoUsd = new CuentaBancaria();
        bancoUsd.setId(101L);
        bancoUsd.setCuentaContable(cuentaBancoUsd);

        proveedor = new Proveedor();
        proveedor.setId(1L);
        proveedor.setNombre("Juan Programador");
        proveedor.setCuentaCxp(cuentaCxp);

        lenient().when(pagoImputacionRepo.findByFacturaCompra_IdAndPago_EstadoOrderByIdAsc(any(), eq(EstadoDocumento.CONFIRMADO)))
                .thenReturn(List.of());
        lenient().when(aplicacionAnticipoRepo.findByFacturaCompra_IdOrderByIdAsc(any())).thenReturn(List.of());
    }

    private CuentaContable cuenta(Long id, String codigo) {
        CuentaContable c = new CuentaContable();
        c.setId(id);
        c.setCodigo(codigo);
        c.setImputable(true);
        c.setActivo(true);
        return c;
    }

    private FacturaCompra factura(Long id, Moneda moneda, BigDecimal tc, BigDecimal total, BigDecimal totalArs) {
        FacturaCompra f = new FacturaCompra();
        f.setId(id);
        f.setNumero("00001-" + id);
        f.setProveedor(proveedor);
        f.setMoneda(moneda);
        f.setTipoCambio(tc);
        f.setTotal(total);
        f.setTotalArs(totalArs);
        f.setEstado(EstadoDocumento.CONFIRMADO);
        return f;
    }

    private Pago pago(Long id, Moneda moneda, BigDecimal tc, CuentaBancaria banco, BigDecimal total, PagoImputacion... imputaciones) {
        Pago p = new Pago();
        p.setId(id);
        p.setProveedor(proveedor);
        p.setFecha(LocalDate.of(2026, 6, 15));
        p.setMoneda(moneda);
        p.setTipoCambio(tc);
        p.setCuentaBancaria(banco);
        p.setTotal(total);
        for (PagoImputacion imp : imputaciones) {
            imp.setPago(p);
            p.getLineas().add(imp);
        }
        return p;
    }

    private PagoImputacion imputacion(FacturaCompra factura, BigDecimal monto) {
        PagoImputacion i = new PagoImputacion();
        i.setFacturaCompra(factura);
        i.setMontoImputadoOriginal(monto);
        return i;
    }

    private void assertBalancea(AsientoGenerado generado) {
        BigDecimal debe = generado.lineas().stream().map(LineaAsientoGenerada::debe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal haber = generado.lineas().stream().map(LineaAsientoGenerada::haber).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debe).isEqualByComparingTo(haber);
    }

    // ---- PA-1 (=CP-10): USD, TC mayor — pérdida ----

    @Test
    void pa1_usdTcMayorPerdida() {
        when(resolutorCuentas.resolver(ConceptoContable.DIF_CAMBIO_PERDIDA)).thenReturn(cuentaDifPerdida);
        FacturaCompra f = factura(10L, usd, new BigDecimal("1500.000000"), new BigDecimal("200.00"), new BigDecimal("300000.00"));

        Pago p = pago(1L, usd, new BigDecimal("1540.000000"), bancoUsd, new BigDecimal("200.00"),
                imputacion(f, new BigDecimal("200.00")));

        AsientoGenerado generado = generator.generar(p);

        assertThat(generado.lineas()).hasSize(3);
        assertThat(generado.lineas().get(0).cuentaCodigo()).isEqualTo("2.1.2002");
        assertThat(generado.lineas().get(0).debe()).isEqualByComparingTo("300000.00");
        assertThat(generado.lineas().get(1).cuentaCodigo()).isEqualTo("6.4006");
        assertThat(generado.lineas().get(1).debe()).isEqualByComparingTo("8000.00");
        assertThat(generado.lineas().get(2).cuentaCodigo()).isEqualTo("1.1.2002");
        assertThat(generado.lineas().get(2).haber()).isEqualByComparingTo("308000.00");
        assertBalancea(generado);
    }

    // ---- PA-2: ARS total, sin diferencia ----

    @Test
    void pa2_arsTotalSinDiferencia() {
        FacturaCompra f = factura(11L, ars, new BigDecimal("1.000000"), new BigDecimal("121000.00"), new BigDecimal("121000.00"));

        Pago p = pago(2L, ars, new BigDecimal("1.000000"), bancoArs, new BigDecimal("121000.00"),
                imputacion(f, new BigDecimal("121000.00")));

        AsientoGenerado generado = generator.generar(p);

        assertThat(generado.lineas()).hasSize(2);
        assertThat(generado.lineas().get(0).debe()).isEqualByComparingTo("121000.00");
        assertThat(generado.lineas().get(1).haber()).isEqualByComparingTo("121000.00");
        assertBalancea(generado);
    }

    // ---- PA-3: USD parcial, TC menor — ganancia ----

    @Test
    void pa3_usdParcialTcMenorGanancia() {
        when(resolutorCuentas.resolver(ConceptoContable.DIF_CAMBIO_GANADA)).thenReturn(cuentaDifGanada);
        FacturaCompra f = factura(12L, usd, new BigDecimal("1500.000000"), new BigDecimal("200.00"), new BigDecimal("300000.00"));

        Pago p = pago(3L, usd, new BigDecimal("1460.000000"), bancoUsd, new BigDecimal("100.00"),
                imputacion(f, new BigDecimal("100.00")));

        AsientoGenerado generado = generator.generar(p);

        assertThat(generado.lineas().get(0).debe()).isEqualByComparingTo("150000.00");
        assertThat(generado.lineas().get(1).cuentaCodigo()).isEqualTo("6.4005");
        assertThat(generado.lineas().get(1).haber()).isEqualByComparingTo("4000.00");
        assertThat(generado.lineas().get(2).haber()).isEqualByComparingTo("146000.00");
        assertBalancea(generado);
    }

    // ---- Anticipo puro (pago sin facturas) ----

    @Test
    void pagoSinFacturasEsAnticipoPuro() {
        when(resolutorCuentas.resolver(ConceptoContable.ANTICIPO_PROVEEDOR)).thenReturn(cuenta(20L, "1.1.2013"));

        Pago p = pago(4L, usd, new BigDecimal("1500.000000"), bancoUsd, new BigDecimal("500.00"));

        AsientoGenerado generado = generator.generar(p);

        assertThat(generado.lineas()).hasSize(2);
        assertThat(generado.lineas().get(0).cuentaCodigo()).isEqualTo("1.1.2013");
        assertThat(generado.lineas().get(0).debe()).isEqualByComparingTo("750000.00");
        assertThat(generado.lineas().get(1).haber()).isEqualByComparingTo("750000.00");
        assertThat(p.getMontoAnticipo()).isEqualByComparingTo("500.00");
        assertBalancea(generado);
    }

    // ---- Validaciones ----

    @Test
    void imputacionQueSuperaElSaldoLanzaError() {
        FacturaCompra f = factura(13L, usd, new BigDecimal("1500.000000"), new BigDecimal("200.00"), new BigDecimal("300000.00"));
        Pago p = pago(5L, usd, new BigDecimal("1500.000000"), bancoUsd, new BigDecimal("250.00"),
                imputacion(f, new BigDecimal("250.00")));

        assertThatThrownBy(() -> generator.generar(p))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("IMPUTACION_EXCEDE_SALDO");
    }

    @Test
    void pagoSinImporteLanzaError() {
        Pago p = new Pago();
        p.setId(99L);
        p.setProveedor(proveedor);
        p.setMoneda(ars);
        p.setTipoCambio(BigDecimal.ONE);
        p.setCuentaBancaria(bancoArs);
        p.setTotal(BigDecimal.ZERO);

        assertThatThrownBy(() -> generator.generar(p))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("PAGO_SIN_IMPORTE");
    }
}

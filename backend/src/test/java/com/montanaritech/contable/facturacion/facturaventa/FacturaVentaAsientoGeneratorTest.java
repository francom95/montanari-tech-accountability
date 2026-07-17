package com.montanaritech.contable.facturacion.facturaventa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.asiento.AsientoGenerado;
import com.montanaritech.contable.common.asiento.LineaAsientoGenerada;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ResolutorCuentas;
import com.montanaritech.contable.facturacion.TipoComprobante;
import com.montanaritech.contable.maestros.cliente.Cliente;
import com.montanaritech.contable.maestros.moneda.Moneda;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Reproduce los casos numéricos de F4.1 §7: FV-1 (ARS con IVA), FV-2 (USD
 * sin IVA, =CP-07), FV-3 (ARS "otra venta" con IVA). Cada caso verifica el
 * asiento completo (cuentas, lados, importes) y que balancea exacto.
 */
@ExtendWith(MockitoExtension.class)
class FacturaVentaAsientoGeneratorTest {

    @Mock private ResolutorCuentas resolutorCuentas;

    private FacturaVentaAsientoGenerator generator;
    private Cliente clienteConCuentaPropia;
    private Cliente clienteSinCuentaPropia;
    private CuentaContable cuentaCxc;
    private CuentaContable cuentaIngresoVenta;
    private CuentaContable cuentaIngresoOtraVenta;
    private CuentaContable cuentaIvaDebito;
    private Moneda ars;
    private Moneda usd;

    @BeforeEach
    void setUp() {
        generator = new FacturaVentaAsientoGenerator(resolutorCuentas);

        cuentaCxc = cuenta(3L, "1.1.2004.01");
        cuentaIngresoVenta = cuenta(46L, "4.1.2001");
        cuentaIngresoOtraVenta = cuenta(47L, "4.1.2002");
        cuentaIvaDebito = cuenta(20L, "2.1.2008");

        clienteConCuentaPropia = new Cliente();
        clienteConCuentaPropia.setId(1L);
        clienteConCuentaPropia.setNombre("Valvecchia Gerardo");
        clienteConCuentaPropia.setCuentaCxc(cuentaCxc);

        clienteSinCuentaPropia = new Cliente();
        clienteSinCuentaPropia.setId(2L);
        clienteSinCuentaPropia.setNombre("Cliente Nuevo");

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        usd = new Moneda();
        usd.setId(2L);
        usd.setCodigo("USD");
    }

    private CuentaContable cuenta(Long id, String codigo) {
        CuentaContable c = new CuentaContable();
        c.setId(id);
        c.setCodigo(codigo);
        c.setImputable(true);
        c.setActivo(true);
        return c;
    }

    private FacturaVentaLinea linea(String descripcion, BigDecimal neto, BigDecimal alicuota, TipoIngreso tipoIngreso) {
        FacturaVentaLinea l = new FacturaVentaLinea();
        l.setDescripcion(descripcion);
        l.setTipo(TipoLineaFactura.GRAVADO);
        l.setImporteNeto(neto);
        l.setAlicuotaIva(alicuota);
        l.setImporteIva(neto.multiply(alicuota).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        l.setTipoIngreso(tipoIngreso);
        return l;
    }

    private FacturaVenta factura(Cliente cliente, Moneda moneda, BigDecimal tc, TipoComprobante tipo, List<FacturaVentaLinea> lineas) {
        FacturaVenta f = new FacturaVenta();
        f.setId(100L);
        f.setCliente(cliente);
        f.setFecha(LocalDate.of(2026, 6, 15));
        f.setTipoComprobante(tipo);
        f.setNumero("00001-00000123");
        f.setMoneda(moneda);
        f.setTipoCambio(tc);

        BigDecimal neto = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;
        int orden = 1;
        for (FacturaVentaLinea l : lineas) {
            l.setFacturaVenta(f);
            l.setOrden(orden++);
            f.getLineas().add(l);
            neto = neto.add(l.getImporteNeto());
            iva = iva.add(l.getImporteIva());
        }
        f.setNetoGravado(neto);
        f.setImporteIva(iva);
        f.setTotal(neto.add(iva));
        return f;
    }

    private void assertBalancea(AsientoGenerado generado) {
        BigDecimal debe = generado.lineas().stream().map(LineaAsientoGenerada::debe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal haber = generado.lineas().stream().map(LineaAsientoGenerada::haber).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debe).isEqualByComparingTo(haber);
    }

    // ---- FV-1: ARS con IVA 21% ----

    @Test
    void fv1_facturaArsConIva21Balancea() {
        when(resolutorCuentas.resolver(ConceptoContable.INGRESO_VENTA, "TIPO_INGRESO", "VENTA")).thenReturn(cuentaIngresoVenta);
        when(resolutorCuentas.resolver(ConceptoContable.IVA_DEBITO_FISCAL)).thenReturn(cuentaIvaDebito);

        FacturaVenta f = factura(clienteConCuentaPropia, ars, new BigDecimal("1.000000"), TipoComprobante.FACTURA_B,
                List.of(linea("Servicio", new BigDecimal("100000.00"), new BigDecimal("21"), TipoIngreso.VENTA)));

        AsientoGenerado generado = generator.generar(f);

        assertThat(generado.origen()).isEqualTo("FACTURA_VENTA");
        assertThat(generado.documentoOrigenTipo()).isEqualTo("FacturaVenta");
        assertThat(generado.documentoOrigenId()).isEqualTo(100L);
        assertThat(generado.lineas()).hasSize(3);

        LineaAsientoGenerada ingreso = generado.lineas().get(0);
        assertThat(ingreso.cuentaCodigo()).isEqualTo("4.1.2001");
        assertThat(ingreso.haber()).isEqualByComparingTo("100000.00");
        assertThat(ingreso.debe()).isEqualByComparingTo("0");
        assertThat(ingreso.clienteId()).isEqualTo(1L);

        LineaAsientoGenerada iva = generado.lineas().get(1);
        assertThat(iva.cuentaCodigo()).isEqualTo("2.1.2008");
        assertThat(iva.haber()).isEqualByComparingTo("21000.00");

        LineaAsientoGenerada cxc = generado.lineas().get(2);
        assertThat(cxc.cuentaCodigo()).isEqualTo("1.1.2004.01");
        assertThat(cxc.debe()).isEqualByComparingTo("121000.00");
        assertThat(cxc.haber()).isEqualByComparingTo("0");

        assertBalancea(generado);
    }

    // ---- FV-2 (=CP-07 parte factura): USD exportación sin IVA ----

    @Test
    void fv2_facturaUsdSinIvaResuelveCxcPorDefectoCuandoClienteNoTieneCuentaPropia() {
        when(resolutorCuentas.resolver(ConceptoContable.INGRESO_VENTA, "TIPO_INGRESO", "VENTA")).thenReturn(cuentaIngresoVenta);
        when(resolutorCuentas.resolver(ConceptoContable.CREDITO_POR_VENTA)).thenReturn(cuentaCxc);

        FacturaVenta f = factura(clienteSinCuentaPropia, usd, new BigDecimal("1500.000000"), TipoComprobante.FACTURA_E,
                List.of(linea("Exportación de servicios", new BigDecimal("1000.00"), BigDecimal.ZERO, TipoIngreso.VENTA)));

        AsientoGenerado generado = generator.generar(f);

        assertThat(generado.lineas()).hasSize(2);
        LineaAsientoGenerada ingreso = generado.lineas().get(0);
        assertThat(ingreso.haber()).isEqualByComparingTo("1500000.00");
        assertThat(ingreso.importeOriginal()).isEqualByComparingTo("1000.00");
        assertThat(ingreso.tipoCambio()).isEqualByComparingTo("1500.000000");

        LineaAsientoGenerada cxc = generado.lineas().get(1);
        assertThat(cxc.cuentaCodigo()).isEqualTo("1.1.2004.01");
        assertThat(cxc.debe()).isEqualByComparingTo("1500000.00");

        assertBalancea(generado);
    }

    // ---- FV-3: ARS "otra venta" con IVA ----

    @Test
    void fv3_otraVentaArsConIva() {
        when(resolutorCuentas.resolver(ConceptoContable.INGRESO_VENTA, "TIPO_INGRESO", "OTRA_VENTA")).thenReturn(cuentaIngresoOtraVenta);
        when(resolutorCuentas.resolver(ConceptoContable.IVA_DEBITO_FISCAL)).thenReturn(cuentaIvaDebito);

        FacturaVenta f = factura(clienteConCuentaPropia, ars, new BigDecimal("1.000000"), TipoComprobante.FACTURA_B,
                List.of(linea("Otro servicio", new BigDecimal("50000.00"), new BigDecimal("21"), TipoIngreso.OTRA_VENTA)));

        AsientoGenerado generado = generator.generar(f);

        assertThat(generado.lineas().get(0).cuentaCodigo()).isEqualTo("4.1.2002");
        assertThat(generado.lineas().get(0).haber()).isEqualByComparingTo("50000.00");
        assertThat(generado.lineas().get(1).haber()).isEqualByComparingTo("10500.00");
        assertThat(generado.lineas().get(2).debe()).isEqualByComparingTo("60500.00");
        assertBalancea(generado);
    }

    // ---- Nota de crédito: mismo modelo, lados invertidos (ADR-13) ----

    @Test
    void notaDeCreditoInvierteLosLadosRespectoDeUnaFacturaNormal() {
        when(resolutorCuentas.resolver(ConceptoContable.INGRESO_VENTA, "TIPO_INGRESO", "VENTA")).thenReturn(cuentaIngresoVenta);
        when(resolutorCuentas.resolver(ConceptoContable.IVA_DEBITO_FISCAL)).thenReturn(cuentaIvaDebito);

        FacturaVenta f = factura(clienteConCuentaPropia, ars, new BigDecimal("1.000000"), TipoComprobante.NOTA_CREDITO_B,
                List.of(linea("Anulación parcial", new BigDecimal("100000.00"), new BigDecimal("21"), TipoIngreso.VENTA)));

        AsientoGenerado generado = generator.generar(f);

        LineaAsientoGenerada ingreso = generado.lineas().get(0);
        assertThat(ingreso.debe()).isEqualByComparingTo("100000.00");
        assertThat(ingreso.haber()).isEqualByComparingTo("0");

        LineaAsientoGenerada cxc = generado.lineas().get(2);
        assertThat(cxc.haber()).isEqualByComparingTo("121000.00");
        assertThat(cxc.debe()).isEqualByComparingTo("0");

        assertBalancea(generado);
    }

    // ---- Mapeo de cuenta faltante ----

    @Test
    void sinCuentaCxcDelClienteYSinMapeoPorDefectoLanzaMapeoCuentaFaltante() {
        lenient().when(resolutorCuentas.resolver(ConceptoContable.INGRESO_VENTA, "TIPO_INGRESO", "VENTA")).thenReturn(cuentaIngresoVenta);
        when(resolutorCuentas.resolver(ConceptoContable.CREDITO_POR_VENTA))
                .thenThrow(new NegocioException("MAPEO_CUENTA_FALTANTE", "No hay cuenta configurada"));

        FacturaVenta f = factura(clienteSinCuentaPropia, ars, new BigDecimal("1.000000"), TipoComprobante.FACTURA_B,
                List.of(linea("Servicio", new BigDecimal("100.00"), BigDecimal.ZERO, TipoIngreso.VENTA)));

        assertThatThrownBy(() -> generator.generar(f))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("MAPEO_CUENTA_FALTANTE");
    }

    @Test
    void facturaSinImporteLanzaFacturaSinImporte() {
        FacturaVenta f = factura(clienteConCuentaPropia, ars, new BigDecimal("1.000000"), TipoComprobante.FACTURA_B, List.of());

        assertThatThrownBy(() -> generator.generar(f))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("FACTURA_SIN_IMPORTE");
    }
}

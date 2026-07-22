package com.montanaritech.contable.facturacion.facturacompra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.asiento.AsientoGenerado;
import com.montanaritech.contable.common.asiento.LineaAsientoGenerada;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ResolutorCuentas;
import com.montanaritech.contable.facturacion.TipoComprobante;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTipo;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTributo;
import com.montanaritech.contable.facturacion.comprobantetributo.ComprobanteTributoRepository;
import com.montanaritech.contable.facturacion.comprobantetributo.TipoTributo;
import com.montanaritech.contable.maestros.proveedor.CondicionIva;
import com.montanaritech.contable.maestros.proveedor.Proveedor;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.tipocosto.TipoCosto;
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
 * Reproduce los casos numéricos de F4.1 §7: FC-1 (ARS con crédito fiscal),
 * FC-2 (=CP-10 parte factura, USD sin IVA), FC-3 (ARS con percepción de IVA
 * sufrida), FC-4 (proveedor monotributista, IVA no computable).
 */
@ExtendWith(MockitoExtension.class)
class FacturaCompraAsientoGeneratorTest {

    @Mock private ResolutorCuentas resolutorCuentas;
    @Mock private ComprobanteTributoRepository comprobanteTributoRepo;

    private FacturaCompraAsientoGenerator generator;
    private Proveedor proveedorConCuentaPropia;
    private Proveedor proveedorSinCuentaPropia;
    private Proveedor proveedorMonotributista;
    private CuentaContable cuentaCxp;
    private CuentaContable cuentaCostoProgramador;
    private CuentaContable cuentaCostoPrestacionServicios;
    private CuentaContable cuentaIvaCf;
    private CuentaContable cuentaPercepcionIva;
    private TipoCosto tipoCostoProgramador;
    private TipoCosto tipoCostoPrestacionServicios;
    private Moneda ars;
    private Moneda usd;

    @BeforeEach
    void setUp() {
        generator = new FacturaCompraAsientoGenerator(resolutorCuentas, comprobanteTributoRepo);

        cuentaCxp = cuenta(3L, "2.1.2002");
        cuentaCostoProgramador = cuenta(50L, "5.1.2002");
        cuentaCostoPrestacionServicios = cuenta(51L, "5.1.2001");
        cuentaIvaCf = cuenta(20L, "1.1.2006");
        cuentaPercepcionIva = cuenta(21L, "1.1.2007");

        tipoCostoProgramador = new TipoCosto();
        tipoCostoProgramador.setId(1L);
        tipoCostoProgramador.setNombre("Programador");

        tipoCostoPrestacionServicios = new TipoCosto();
        tipoCostoPrestacionServicios.setId(2L);
        tipoCostoPrestacionServicios.setNombre("Costo de Prestación de Servicios");

        proveedorConCuentaPropia = new Proveedor();
        proveedorConCuentaPropia.setId(1L);
        proveedorConCuentaPropia.setNombre("Juan Programador");
        proveedorConCuentaPropia.setCuentaCxp(cuentaCxp);
        proveedorConCuentaPropia.setCondicionIva(CondicionIva.RESPONSABLE_INSCRIPTO);

        proveedorSinCuentaPropia = new Proveedor();
        proveedorSinCuentaPropia.setId(2L);
        proveedorSinCuentaPropia.setNombre("Proveedor del Exterior");
        proveedorSinCuentaPropia.setCondicionIva(CondicionIva.RESPONSABLE_INSCRIPTO);

        proveedorMonotributista = new Proveedor();
        proveedorMonotributista.setId(3L);
        proveedorMonotributista.setNombre("Monotributista SA");
        proveedorMonotributista.setCuentaCxp(cuentaCxp);
        proveedorMonotributista.setCondicionIva(CondicionIva.MONOTRIBUTISTA);

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        usd = new Moneda();
        usd.setId(2L);
        usd.setCodigo("USD");

        lenient().when(comprobanteTributoRepo.findByComprobanteTipoAndComprobanteIdOrderByIdAsc(eq(ComprobanteTipo.FACTURA_COMPRA), any()))
                .thenReturn(List.of());
    }

    private CuentaContable cuenta(Long id, String codigo) {
        CuentaContable c = new CuentaContable();
        c.setId(id);
        c.setCodigo(codigo);
        c.setImputable(true);
        c.setActivo(true);
        return c;
    }

    private FacturaCompraLinea linea(String descripcion, BigDecimal neto, BigDecimal alicuota, TipoCosto tipoCosto) {
        FacturaCompraLinea l = new FacturaCompraLinea();
        l.setDescripcion(descripcion);
        l.setTipoCosto(tipoCosto);
        l.setImporteNeto(neto);
        l.setAlicuotaIva(alicuota);
        l.setImporteIva(neto.multiply(alicuota).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        return l;
    }

    private FacturaCompra factura(Proveedor proveedor, Moneda moneda, BigDecimal tc, TipoComprobante tipo, List<FacturaCompraLinea> lineas) {
        FacturaCompra f = new FacturaCompra();
        f.setId(100L);
        f.setProveedor(proveedor);
        f.setFecha(LocalDate.of(2026, 6, 15));
        f.setTipoComprobante(tipo);
        f.setNumero("00001-00000123");
        f.setMoneda(moneda);
        f.setTipoCambio(tc);

        BigDecimal neto = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;
        int orden = 1;
        for (FacturaCompraLinea l : lineas) {
            l.setFacturaCompra(f);
            l.setOrden(orden++);
            f.getLineas().add(l);
            neto = neto.add(l.getImporteNeto());
            iva = iva.add(l.getImporteIva());
        }
        f.setNeto(neto);
        f.setImporteIva(iva);
        f.setTotal(neto.add(iva));
        return f;
    }

    private void stubTributos(FacturaCompra f, ComprobanteTributo... tributos) {
        when(comprobanteTributoRepo.findByComprobanteTipoAndComprobanteIdOrderByIdAsc(ComprobanteTipo.FACTURA_COMPRA, f.getId()))
                .thenReturn(List.of(tributos));
    }

    private void assertBalancea(AsientoGenerado generado) {
        BigDecimal debe = generado.lineas().stream().map(LineaAsientoGenerada::debe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal haber = generado.lineas().stream().map(LineaAsientoGenerada::haber).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debe).isEqualByComparingTo(haber);
    }

    // ---- FC-1: ARS con crédito fiscal ----

    @Test
    void fc1_facturaArsConCreditoFiscalBalancea() {
        when(resolutorCuentas.resolver(ConceptoContable.COSTO_GASTO, "CATEGORIA", "Programador")).thenReturn(cuentaCostoProgramador);
        when(resolutorCuentas.resolver(ConceptoContable.IVA_CREDITO_FISCAL)).thenReturn(cuentaIvaCf);

        FacturaCompra f = factura(proveedorConCuentaPropia, ars, new BigDecimal("1.000000"), TipoComprobante.FACTURA_A,
                List.of(linea("Desarrollo", new BigDecimal("100000.00"), new BigDecimal("21"), tipoCostoProgramador)));

        AsientoGenerado generado = generator.generar(f);

        assertThat(generado.origen()).isEqualTo("FACTURA_COMPRA");
        assertThat(generado.documentoOrigenTipo()).isEqualTo("FacturaCompra");
        assertThat(generado.documentoOrigenId()).isEqualTo(100L);
        assertThat(generado.lineas()).hasSize(3);

        LineaAsientoGenerada costo = generado.lineas().get(0);
        assertThat(costo.cuentaCodigo()).isEqualTo("5.1.2002");
        assertThat(costo.debe()).isEqualByComparingTo("100000.00");
        assertThat(costo.proveedorId()).isEqualTo(1L);

        LineaAsientoGenerada iva = generado.lineas().get(1);
        assertThat(iva.cuentaCodigo()).isEqualTo("1.1.2006");
        assertThat(iva.debe()).isEqualByComparingTo("21000.00");

        LineaAsientoGenerada cxp = generado.lineas().get(2);
        assertThat(cxp.cuentaCodigo()).isEqualTo("2.1.2002");
        assertThat(cxp.haber()).isEqualByComparingTo("121000.00");
        assertThat(cxp.debe()).isEqualByComparingTo("0");

        assertBalancea(generado);
    }

    // ---- FC-2 (=CP-10 parte factura): USD exterior sin IVA, sin cuenta propia ----

    @Test
    void fc2_facturaUsdSinIvaResuelveCxpPorDefectoCuandoProveedorNoTieneCuentaPropia() {
        when(resolutorCuentas.resolver(ConceptoContable.COSTO_GASTO, "CATEGORIA", "Costo de Prestación de Servicios"))
                .thenReturn(cuentaCostoPrestacionServicios);
        when(resolutorCuentas.resolver(ConceptoContable.DEUDA_COMERCIAL)).thenReturn(cuentaCxp);

        FacturaCompra f = factura(proveedorSinCuentaPropia, usd, new BigDecimal("1500.000000"), TipoComprobante.FACTURA_E,
                List.of(linea("Servicios del exterior", new BigDecimal("200.00"), BigDecimal.ZERO, tipoCostoPrestacionServicios)));

        AsientoGenerado generado = generator.generar(f);

        assertThat(generado.lineas()).hasSize(2);
        LineaAsientoGenerada costo = generado.lineas().get(0);
        assertThat(costo.cuentaCodigo()).isEqualTo("5.1.2001");
        assertThat(costo.debe()).isEqualByComparingTo("300000.00");
        assertThat(costo.importeOriginal()).isEqualByComparingTo("200.00");
        assertThat(costo.tipoCambio()).isEqualByComparingTo("1500.000000");

        LineaAsientoGenerada cxp = generado.lineas().get(1);
        assertThat(cxp.cuentaCodigo()).isEqualTo("2.1.2002");
        assertThat(cxp.haber()).isEqualByComparingTo("300000.00");

        assertBalancea(generado);
    }

    // ---- FC-3: ARS con percepción de IVA sufrida ----

    @Test
    void fc3_facturaArsConPercepcionDeIvaSufrida() {
        when(resolutorCuentas.resolver(ConceptoContable.COSTO_GASTO, "CATEGORIA", "Programador")).thenReturn(cuentaCostoProgramador);
        when(resolutorCuentas.resolver(ConceptoContable.IVA_CREDITO_FISCAL)).thenReturn(cuentaIvaCf);
        when(resolutorCuentas.resolver(ConceptoContable.PERCEPCION_IVA_SUFRIDA)).thenReturn(cuentaPercepcionIva);

        FacturaCompra f = factura(proveedorConCuentaPropia, ars, new BigDecimal("1.000000"), TipoComprobante.FACTURA_A,
                List.of(linea("Desarrollo", new BigDecimal("100000.00"), new BigDecimal("21"), tipoCostoProgramador)));
        f.setTotal(f.getTotal().add(new BigDecimal("3000.00")));
        ComprobanteTributo percepcion = new ComprobanteTributo();
        percepcion.setTipo(TipoTributo.PERCEPCION_IVA);
        percepcion.setImporte(new BigDecimal("3000.00"));
        stubTributos(f, percepcion);

        AsientoGenerado generado = generator.generar(f);

        assertThat(generado.lineas()).hasSize(4);
        assertThat(generado.lineas().get(0).debe()).isEqualByComparingTo("100000.00");
        assertThat(generado.lineas().get(1).debe()).isEqualByComparingTo("21000.00");

        LineaAsientoGenerada percepcionLinea = generado.lineas().get(2);
        assertThat(percepcionLinea.cuentaCodigo()).isEqualTo("1.1.2007");
        assertThat(percepcionLinea.debe()).isEqualByComparingTo("3000.00");

        LineaAsientoGenerada cxp = generado.lineas().get(3);
        assertThat(cxp.haber()).isEqualByComparingTo("124000.00");

        assertBalancea(generado);
    }

    // ---- FC-4: proveedor monotributista, IVA no computable (se absorbe en el costo) ----

    @Test
    void fc4_proveedorMonotributistaNoComputaCreditoFiscal() {
        when(resolutorCuentas.resolver(ConceptoContable.COSTO_GASTO, "CATEGORIA", "Programador")).thenReturn(cuentaCostoProgramador);

        FacturaCompra f = factura(proveedorMonotributista, ars, new BigDecimal("1.000000"), TipoComprobante.FACTURA_C,
                List.of(linea("Desarrollo", new BigDecimal("50000.00"), BigDecimal.ZERO, tipoCostoProgramador)));

        AsientoGenerado generado = generator.generar(f);

        assertThat(generado.lineas()).hasSize(2);
        assertThat(generado.lineas().get(0).debe()).isEqualByComparingTo("50000.00");
        assertThat(generado.lineas().get(1).haber()).isEqualByComparingTo("50000.00");
        assertBalancea(generado);
    }

    // ---- El IVA discriminado se absorbe en el costo cuando no computa crédito fiscal ----

    @Test
    void sinCreditoFiscalElIvaDeLaLineaSeAbsorbeEnElCosto() {
        when(resolutorCuentas.resolver(ConceptoContable.COSTO_GASTO, "CATEGORIA", "Programador")).thenReturn(cuentaCostoProgramador);

        FacturaCompra f = factura(proveedorMonotributista, ars, new BigDecimal("1.000000"), TipoComprobante.FACTURA_A,
                List.of(linea("Desarrollo", new BigDecimal("100000.00"), new BigDecimal("21"), tipoCostoProgramador)));

        AsientoGenerado generado = generator.generar(f);

        assertThat(generado.lineas()).hasSize(2);
        assertThat(generado.lineas().get(0).debe()).isEqualByComparingTo("121000.00");
        assertThat(generado.lineas().get(1).haber()).isEqualByComparingTo("121000.00");
        assertBalancea(generado);
    }

    // ---- Comprobante tipo C fuerza no-crédito-fiscal aunque el proveedor sea RI ----

    @Test
    void facturaCConProveedorResponsableInscriptoNoComputaCreditoFiscal() {
        when(resolutorCuentas.resolver(ConceptoContable.COSTO_GASTO, "CATEGORIA", "Programador")).thenReturn(cuentaCostoProgramador);

        FacturaCompra f = factura(proveedorConCuentaPropia, ars, new BigDecimal("1.000000"), TipoComprobante.FACTURA_C,
                List.of(linea("Desarrollo", new BigDecimal("100000.00"), new BigDecimal("21"), tipoCostoProgramador)));

        AsientoGenerado generado = generator.generar(f);

        assertThat(generado.lineas()).hasSize(2);
        assertThat(generado.lineas().get(0).debe()).isEqualByComparingTo("121000.00");
        assertBalancea(generado);
    }

    // ---- Nota de crédito: mismo modelo, lados invertidos (ADR-13) ----

    @Test
    void notaDeCreditoInvierteLosLadosRespectoDeUnaFacturaNormal() {
        when(resolutorCuentas.resolver(ConceptoContable.COSTO_GASTO, "CATEGORIA", "Programador")).thenReturn(cuentaCostoProgramador);
        when(resolutorCuentas.resolver(ConceptoContable.IVA_CREDITO_FISCAL)).thenReturn(cuentaIvaCf);

        FacturaCompra f = factura(proveedorConCuentaPropia, ars, new BigDecimal("1.000000"), TipoComprobante.NOTA_CREDITO_A,
                List.of(linea("Anulación parcial", new BigDecimal("100000.00"), new BigDecimal("21"), tipoCostoProgramador)));

        AsientoGenerado generado = generator.generar(f);

        LineaAsientoGenerada costo = generado.lineas().get(0);
        assertThat(costo.haber()).isEqualByComparingTo("100000.00");
        assertThat(costo.debe()).isEqualByComparingTo("0");

        LineaAsientoGenerada cxp = generado.lineas().get(2);
        assertThat(cxp.debe()).isEqualByComparingTo("121000.00");
        assertThat(cxp.haber()).isEqualByComparingTo("0");

        assertBalancea(generado);
    }

    // ---- Mapeo de cuenta faltante ----

    @Test
    void sinCuentaCxpDelProveedorYSinMapeoPorDefectoLanzaMapeoCuentaFaltante() {
        lenient().when(resolutorCuentas.resolver(ConceptoContable.COSTO_GASTO, "CATEGORIA", "Costo de Prestación de Servicios"))
                .thenReturn(cuentaCostoPrestacionServicios);
        when(resolutorCuentas.resolver(ConceptoContable.DEUDA_COMERCIAL))
                .thenThrow(new NegocioException("MAPEO_CUENTA_FALTANTE", "No hay cuenta configurada"));

        FacturaCompra f = factura(proveedorSinCuentaPropia, ars, new BigDecimal("1.000000"), TipoComprobante.FACTURA_A,
                List.of(linea("Servicio", new BigDecimal("100.00"), BigDecimal.ZERO, tipoCostoPrestacionServicios)));

        assertThatThrownBy(() -> generator.generar(f))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("MAPEO_CUENTA_FALTANTE");
    }

    @Test
    void tributoDeTipoNoAplicableALaCompraLanzaTributoNoAplicable() {
        when(resolutorCuentas.resolver(ConceptoContable.COSTO_GASTO, "CATEGORIA", "Programador")).thenReturn(cuentaCostoProgramador);
        when(resolutorCuentas.resolver(ConceptoContable.IVA_CREDITO_FISCAL)).thenReturn(cuentaIvaCf);

        FacturaCompra f = factura(proveedorConCuentaPropia, ars, new BigDecimal("1.000000"), TipoComprobante.FACTURA_A,
                List.of(linea("Desarrollo", new BigDecimal("100000.00"), new BigDecimal("21"), tipoCostoProgramador)));
        ComprobanteTributo retencion = new ComprobanteTributo();
        retencion.setTipo(TipoTributo.RETENCION_GANANCIAS);
        retencion.setImporte(new BigDecimal("1000.00"));
        stubTributos(f, retencion);

        assertThatThrownBy(() -> generator.generar(f))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("TRIBUTO_NO_APLICABLE_A_COMPRA");
    }

    @Test
    void facturaSinImporteLanzaFacturaSinImporte() {
        FacturaCompra f = factura(proveedorConCuentaPropia, ars, new BigDecimal("1.000000"), TipoComprobante.FACTURA_A, List.of());

        assertThatThrownBy(() -> generator.generar(f))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("FACTURA_SIN_IMPORTE");
    }
}

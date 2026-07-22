package com.montanaritech.contable.bancos.conciliacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.bancos.movimientobancario.OrigenImportacionMovimiento;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ConceptoContable;
import com.montanaritech.contable.contabilidad.mapeocuenta.ResolutorCuentas;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Imputación rápida por descripción (F5.3): patrones conocidos + fallback vacío cuando no hay mapeo_cuenta. */
@ExtendWith(MockitoExtension.class)
class ClasificadorMovimientoBancarioTest {

    @Mock private ResolutorCuentas resolutorCuentas;

    private ClasificadorMovimientoBancario clasificador;
    private CuentaContable comisionesBancarias;
    private CuentaContable comisionesMercadoPago;
    private CuentaContable impuesto25413;
    private CuentaContable percepcionIibb;
    private CuentaContable percepcionIva;

    @BeforeEach
    void setUp() {
        clasificador = new ClasificadorMovimientoBancario(resolutorCuentas);

        comisionesBancarias = cuenta("6.4003", "Comisiones bancarias");
        comisionesMercadoPago = cuenta("6.4004", "Comisiones Mercado Pago");
        impuesto25413 = cuenta("5.3.2012", "Impuesto Ley 25413");
        percepcionIibb = cuenta("1.1.2008", "Percepciones IIBB sufridas");
        percepcionIva = cuenta("1.1.2007", "Percepciones IVA sufridas");

        lenient().when(resolutorCuentas.resolver(ConceptoContable.COMISION_BANCARIA)).thenReturn(comisionesBancarias);
        lenient().when(resolutorCuentas.resolver(eq(ConceptoContable.COMISION_BANCARIA), eq("ORIGEN_IMPORTACION"), eq("MERCADO_PAGO")))
                .thenReturn(comisionesMercadoPago);
        lenient().when(resolutorCuentas.resolver(eq(ConceptoContable.COMISION_BANCARIA), eq("ORIGEN_IMPORTACION"), eq("GALICIA")))
                .thenReturn(comisionesBancarias);
        lenient().when(resolutorCuentas.resolver(eq(ConceptoContable.IMPUESTO_DEBITOS_CREDITOS_BANCARIOS), eq("ORIGEN_IMPORTACION"), eq("GALICIA")))
                .thenReturn(impuesto25413);
        lenient().when(resolutorCuentas.resolver(eq(ConceptoContable.PERCEPCION_IIBB_SUFRIDA), eq("ORIGEN_IMPORTACION"), eq("GALICIA")))
                .thenReturn(percepcionIibb);
        lenient().when(resolutorCuentas.resolver(eq(ConceptoContable.PERCEPCION_IVA_SUFRIDA), eq("ORIGEN_IMPORTACION"), eq("GALICIA")))
                .thenReturn(percepcionIva);
    }

    private CuentaContable cuenta(String codigo, String nombre) {
        CuentaContable c = new CuentaContable();
        c.setId((long) codigo.hashCode());
        c.setCodigo(codigo);
        c.setNombre(nombre);
        return c;
    }

    @Test
    void comisionMatcheaSinImportarMayusculasNiTilde() {
        var resultado = clasificador.clasificar("Comision Mantenimiento Cta. Cte/cce", OrigenImportacionMovimiento.GALICIA);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().concepto()).isEqualTo(ConceptoContable.COMISION_BANCARIA);
        assertThat(resultado.get().cuenta().getCodigo()).isEqualTo("6.4003");
    }

    @Test
    void comisionDeMercadoPagoUsaLaCuentaEspecificaDeMercadoPago() {
        var resultado = clasificador.clasificar("Comision por venta", OrigenImportacionMovimiento.MERCADO_PAGO);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().cuenta().getCodigo()).isEqualTo("6.4004");
    }

    @Test
    void impuestoLey25413Matchea() {
        var resultado = clasificador.clasificar("IMP. DEB. LEY 25413 GRAL.", OrigenImportacionMovimiento.GALICIA);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().concepto()).isEqualTo(ConceptoContable.IMPUESTO_DEBITOS_CREDITOS_BANCARIOS);
        assertThat(resultado.get().cuenta().getCodigo()).isEqualTo("5.3.2012");
    }

    @Test
    void sircrebReusaElConceptoDePercepcionIibbSufrida() {
        var resultado = clasificador.clasificar("ING. BRUTOS S/ CRED REG.RECAU.SIRCREB", OrigenImportacionMovimiento.GALICIA);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().concepto()).isEqualTo(ConceptoContable.PERCEPCION_IIBB_SUFRIDA);
    }

    @Test
    void percepcionIvaMatchea() {
        var resultado = clasificador.clasificar("PERCEP.IVA RG2408 3,0% B.", OrigenImportacionMovimiento.GALICIA);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().concepto()).isEqualTo(ConceptoContable.PERCEPCION_IVA_SUFRIDA);
    }

    @Test
    void descripcionSinPatronConocidoNoSugiereNada() {
        var resultado = clasificador.clasificar("TRF INMED PROVEED Alejo Del Gobbo", OrigenImportacionMovimiento.GALICIA);

        assertThat(resultado).isEmpty();
    }

    @Test
    void matcheaPeroSinMapeoCuentaConfiguradoNoSugiereNada() {
        when(resolutorCuentas.resolver(eq(ConceptoContable.COMISION_BANCARIA), eq("ORIGEN_IMPORTACION"), eq("TARJETA_CREDITO")))
                .thenThrow(new NegocioException("MAPEO_CUENTA_FALTANTE", "sin mapeo"));

        var resultado = clasificador.clasificar("Comision", OrigenImportacionMovimiento.TARJETA_CREDITO);

        assertThat(resultado).isEmpty();
    }

    @Test
    void descripcionNulaNoRompe() {
        assertThat(clasificador.clasificar(null, OrigenImportacionMovimiento.GALICIA)).isEmpty();
    }
}

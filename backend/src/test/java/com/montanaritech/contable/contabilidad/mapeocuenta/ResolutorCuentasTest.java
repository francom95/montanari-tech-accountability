package com.montanaritech.contable.contabilidad.mapeocuenta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Algoritmo de resolución de F4.1 §1.2: fila específica -> fila por defecto -> MAPEO_CUENTA_FALTANTE. */
@ExtendWith(MockitoExtension.class)
class ResolutorCuentasTest {

    @Mock private MapeoCuentaRepository repo;
    private ResolutorCuentas resolutor;

    private CuentaContable cuentaImputableActiva;
    private CuentaContable cuentaMadre;
    private CuentaContable cuentaInactiva;

    @BeforeEach
    void setUp() {
        resolutor = new ResolutorCuentas(repo);

        cuentaImputableActiva = new CuentaContable();
        cuentaImputableActiva.setId(1L);
        cuentaImputableActiva.setCodigo("2.1.2008");
        cuentaImputableActiva.setImputable(true);
        cuentaImputableActiva.setActivo(true);

        cuentaMadre = new CuentaContable();
        cuentaMadre.setId(2L);
        cuentaMadre.setCodigo("1.1");
        cuentaMadre.setImputable(false);
        cuentaMadre.setActivo(true);

        cuentaInactiva = new CuentaContable();
        cuentaInactiva.setId(3L);
        cuentaInactiva.setCodigo("4.1.2001");
        cuentaInactiva.setImputable(true);
        cuentaInactiva.setActivo(false);
    }

    private MapeoCuenta mapeo(CuentaContable cuenta) {
        MapeoCuenta m = new MapeoCuenta();
        m.setCuentaContable(cuenta);
        return m;
    }

    @Test
    void resuelvePorFilaEspecificaAntesQuePorDefecto() {
        when(repo.findByConceptoAndDiscriminadorTipoAndDiscriminadorValorAndActivoTrue(
                ConceptoContable.INGRESO_VENTA, "TIPO_INGRESO", "VENTA"))
                .thenReturn(Optional.of(mapeo(cuentaImputableActiva)));

        CuentaContable resuelta = resolutor.resolver(ConceptoContable.INGRESO_VENTA, "TIPO_INGRESO", "VENTA");

        assertThat(resuelta.getCodigo()).isEqualTo("2.1.2008");
    }

    @Test
    void caeALaFilaPorDefectoSiNoHayFilaEspecifica() {
        when(repo.findByConceptoAndDiscriminadorTipoAndDiscriminadorValorAndActivoTrue(
                ConceptoContable.IVA_DEBITO_FISCAL, "X", "Y")).thenReturn(Optional.empty());
        when(repo.findByConceptoAndDiscriminadorTipoIsNullAndActivoTrue(ConceptoContable.IVA_DEBITO_FISCAL))
                .thenReturn(Optional.of(mapeo(cuentaImputableActiva)));

        CuentaContable resuelta = resolutor.resolver(ConceptoContable.IVA_DEBITO_FISCAL, "X", "Y");

        assertThat(resuelta.getCodigo()).isEqualTo("2.1.2008");
    }

    @Test
    void sinFilaEspecificaNiPorDefectoLanzaMapeoCuentaFaltante() {
        when(repo.findByConceptoAndDiscriminadorTipoIsNullAndActivoTrue(ConceptoContable.CREDITO_POR_VENTA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolutor.resolver(ConceptoContable.CREDITO_POR_VENTA))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("MAPEO_CUENTA_FALTANTE");
    }

    @Test
    void cuentaMapeadaMadreLanzaCuentaNoImputable() {
        when(repo.findByConceptoAndDiscriminadorTipoIsNullAndActivoTrue(ConceptoContable.CREDITO_POR_VENTA))
                .thenReturn(Optional.of(mapeo(cuentaMadre)));

        assertThatThrownBy(() -> resolutor.resolver(ConceptoContable.CREDITO_POR_VENTA))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("CUENTA_NO_IMPUTABLE");
    }

    @Test
    void cuentaMapeadaInactivaLanzaCuentaInactiva() {
        when(repo.findByConceptoAndDiscriminadorTipoIsNullAndActivoTrue(ConceptoContable.INGRESO_VENTA))
                .thenReturn(Optional.of(mapeo(cuentaInactiva)));

        assertThatThrownBy(() -> resolutor.resolver(ConceptoContable.INGRESO_VENTA))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("CUENTA_INACTIVA");
    }
}

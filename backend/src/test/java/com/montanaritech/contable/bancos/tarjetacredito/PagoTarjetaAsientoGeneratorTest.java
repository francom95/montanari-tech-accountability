package com.montanaritech.contable.bancos.tarjetacredito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.montanaritech.contable.common.asiento.AsientoGenerado;
import com.montanaritech.contable.common.error.NegocioException;
import com.montanaritech.contable.contabilidad.cuentacontable.CuentaContable;
import com.montanaritech.contable.maestros.cuentabancaria.CuentaBancaria;
import com.montanaritech.contable.maestros.moneda.Moneda;
import com.montanaritech.contable.maestros.tarjetacredito.TarjetaCredito;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Asiento del pago de resumen de tarjeta (F5.4 §3): 2 líneas, cuenta bancaria seteada para que F5.3 lo pueda conciliar gratis. */
class PagoTarjetaAsientoGeneratorTest {

    private final PagoTarjetaAsientoGenerator generator = new PagoTarjetaAsientoGenerator();

    private CuentaContable cuentaTarjeta;
    private CuentaContable cuentaFondos;
    private CuentaBancaria cuentaBancariaDebito;
    private TarjetaCredito tarjeta;
    private Moneda ars;

    @BeforeEach
    void setUp() {
        cuentaTarjeta = new CuentaContable();
        cuentaTarjeta.setId(1L);
        cuentaTarjeta.setCodigo("2.1.2019");

        cuentaFondos = new CuentaContable();
        cuentaFondos.setId(2L);
        cuentaFondos.setCodigo("1.1.2001");

        cuentaBancariaDebito = new CuentaBancaria();
        cuentaBancariaDebito.setId(10L);
        cuentaBancariaDebito.setCuentaContable(cuentaFondos);

        ars = new Moneda();
        ars.setId(1L);
        ars.setCodigo("ARS");

        tarjeta = new TarjetaCredito();
        tarjeta.setId(5L);
        tarjeta.setEntidad("Visa Banco Galicia");
        tarjeta.setCuentaContable(cuentaTarjeta);
        tarjeta.setCuentaBancariaDebito(cuentaBancariaDebito);
    }

    private PagoTarjeta pago(BigDecimal importe) {
        PagoTarjeta p = new PagoTarjeta();
        p.setId(100L);
        p.setTarjetaCredito(tarjeta);
        p.setFecha(LocalDate.of(2026, 7, 5));
        p.setImporte(importe);
        p.setImporteArs(importe);
        p.setMoneda(ars);
        p.setTipoCambio(BigDecimal.ONE);
        return p;
    }

    @Test
    void generaDosLineasDebeTarjetaHaberFondosConCuentaBancariaSeteada() {
        AsientoGenerado generado = generator.generar(pago(new BigDecimal("50000.00")));

        assertThat(generado.origen()).isEqualTo("RESUMEN_TARJETA");
        assertThat(generado.documentoOrigenTipo()).isEqualTo("PagoTarjeta");
        assertThat(generado.documentoOrigenId()).isEqualTo(100L);
        assertThat(generado.lineas()).hasSize(2);

        var lineaTarjeta = generado.lineas().get(0);
        assertThat(lineaTarjeta.cuentaCodigo()).isEqualTo("2.1.2019");
        assertThat(lineaTarjeta.debe()).isEqualByComparingTo("50000.00");
        assertThat(lineaTarjeta.haber()).isEqualByComparingTo("0");

        var lineaFondos = generado.lineas().get(1);
        assertThat(lineaFondos.cuentaCodigo()).isEqualTo("1.1.2001");
        assertThat(lineaFondos.haber()).isEqualByComparingTo("50000.00");
        assertThat(lineaFondos.debe()).isEqualByComparingTo("0");
        // La clave para que F5.3 pueda sugerir este asiento como match: cuentaBancariaId seteado en la línea de fondos.
        assertThat(lineaFondos.cuentaBancariaId()).isEqualTo(10L);
    }

    @Test
    void importeCeroONegativoLanzaError() {
        assertThatThrownBy(() -> generator.generar(pago(BigDecimal.ZERO)))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("PAGO_TARJETA_SIN_IMPORTE");
    }

    @Test
    void tarjetaSinCuentaContableConfiguradaLanzaError() {
        tarjeta.setCuentaContable(null);

        assertThatThrownBy(() -> generator.generar(pago(new BigDecimal("1000.00"))))
                .isInstanceOf(NegocioException.class)
                .extracting(e -> ((NegocioException) e).getCodigo())
                .isEqualTo("TARJETA_SIN_CUENTA_CONTABLE");
    }

    @Test
    void pagoParcialGeneraElAsientoPorElImporteIndicadoNoElTotal() {
        AsientoGenerado generado = generator.generar(pago(new BigDecimal("10000.00")));

        assertThat(generado.lineas().get(0).debe()).isEqualByComparingTo("10000.00");
        assertThat(generado.lineas().get(1).haber()).isEqualByComparingTo("10000.00");
    }
}

package com.montanaritech.contable.impuestos.iva;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Las dos etapas del art. 24 de la Ley 23.349 (F6.1 §1.3), validadas con el
 * contador. Lo que se prueba acá es la diferencia que motivó separarlas: un
 * saldo técnico y un saldo de libre disponibilidad no son intercambiables aunque
 * sumen lo mismo.
 */
class ResultadoIvaTest {

    /** (etapa, aporte ya con signo) */
    private record Aporte(TipoComponenteIva.Etapa etapa, BigDecimal valor) {
    }

    @Test
    void mesConSaldoAPagar() {
        // débito 210.000 − crédito 84.000 = 126.000, sin ingresos directos
        ResultadoIva r = calcular(tecnica("210000"), tecnica("-84000"));

        assertThat(r.saldoAPagar()).isEqualByComparingTo("126000.00");
        assertThat(r.saldoTecnico()).isEqualByComparingTo("0.00");
        assertThat(r.saldoLibreDisponibilidad()).isEqualByComparingTo("0.00");
    }

    @Test
    void masCreditoQueDebitoDejaSaldoTecnicoYNoLibreDisponibilidad() {
        // el excedente de crédito fiscal queda cautivo: solo contra IVA futuro
        ResultadoIva r = calcular(tecnica("100000"), tecnica("-160000"));

        assertThat(r.saldoTecnico()).isEqualByComparingTo("60000.00");
        assertThat(r.saldoLibreDisponibilidad()).as("un excedente técnico nunca es de libre disponibilidad")
                .isEqualByComparingTo("0.00");
        assertThat(r.saldoAPagar()).isEqualByComparingTo("0.00");
    }

    @Test
    void percepcionesQueSuperanElImpuestoDejanLibreDisponibilidadYNoTecnico() {
        // determinado 60.000 contra 75.000 de percepciones -> 15.000 compensables
        ResultadoIva r = calcular(tecnica("100000"), tecnica("-40000"), directo("-75000"));

        assertThat(r.saldoLibreDisponibilidad()).isEqualByComparingTo("15000.00");
        assertThat(r.saldoTecnico()).isEqualByComparingTo("0.00");
        assertThat(r.saldoAPagar()).isEqualByComparingTo("0.00");
    }

    /**
     * El caso que justifica todo el cambio: con un solo acumulador estos 90.000
     * salían como un saldo a favor indistinto, volviendo compensable con otros
     * impuestos un excedente técnico que por ley no lo es.
     */
    @Test
    void saldoTecnicoYLibreDisponibilidadPuedenCoexistirEnElMismoMes() {
        // crédito supera al débito (técnico 60.000) y además hubo percepciones (30.000)
        ResultadoIva r = calcular(tecnica("100000"), tecnica("-160000"), directo("-30000"));

        assertThat(r.saldoTecnico()).isEqualByComparingTo("60000.00");
        assertThat(r.saldoLibreDisponibilidad()).isEqualByComparingTo("30000.00");
        assertThat(r.saldoAPagar()).isEqualByComparingTo("0.00");
    }

    @Test
    void lasPercepcionesNoSeUsanParaAbsorberElExcedenteTecnico() {
        // si se mezclaran, los 30.000 de percepciones "taparían" parte del técnico
        // y el total a favor seguiría siendo 90.000 pero mal compuesto
        ResultadoIva r = calcular(tecnica("100000"), tecnica("-160000"), directo("-30000"));

        assertThat(r.saldoTecnico().add(r.saldoLibreDisponibilidad()))
                .as("el total a favor coincide con el del acumulador único")
                .isEqualByComparingTo("90000.00");
        assertThat(r.saldoTecnico()).as("pero la composición es la que manda")
                .isNotEqualByComparingTo(r.saldoLibreDisponibilidad());
    }

    @Test
    void elArrastreTecnicoSoloJuegaEnLaPrimeraEtapa() {
        // débito 120.000 − crédito 90.000 − arrastre técnico 50.000 = −20.000
        ResultadoIva r = calcular(tecnica("120000"), tecnica("-90000"), tecnica("-50000"));

        assertThat(r.saldoTecnico()).isEqualByComparingTo("20000.00");
        assertThat(r.saldoAPagar()).isEqualByComparingTo("0.00");
    }

    @Test
    void elArrastreDeLibreDisponibilidadSeSumaALosIngresosDirectos() {
        // determinado 50.000; percepciones 20.000 + libre disp. anterior 40.000 = 60.000
        ResultadoIva r = calcular(tecnica("50000"), directo("-20000"), directo("-40000"));

        assertThat(r.saldoLibreDisponibilidad()).isEqualByComparingTo("10000.00");
        assertThat(r.saldoAPagar()).isEqualByComparingTo("0.00");
    }

    @Test
    void notaDeCreditoEmitidaAumentaElCreditoFiscalEnVezDeReducirElDebito() {
        // ventas 210.000, NC emitida 21.000 (restitución de crédito), compras 84.000
        ResultadoIva r = calcular(tecnica("210000"), tecnica("-21000"), tecnica("-84000"));

        assertThat(r.saldoAPagar()).isEqualByComparingTo("105000.00");
    }

    @Test
    void periodoSinMovimientosDaTodoEnCero() {
        ResultadoIva r = calcular();

        assertThat(r.saldoAPagar()).isEqualByComparingTo("0.00");
        assertThat(r.saldoTecnico()).isEqualByComparingTo("0.00");
        assertThat(r.saldoLibreDisponibilidad()).isEqualByComparingTo("0.00");
    }

    // --- helpers ---

    private ResultadoIva calcular(Aporte... aportes) {
        return ResultadoIva.calcular(new ArrayList<>(List.of(aportes)), Aporte::etapa, Aporte::valor);
    }

    private Aporte tecnica(String valor) {
        return new Aporte(TipoComponenteIva.Etapa.TECNICA, new BigDecimal(valor));
    }

    private Aporte directo(String valor) {
        return new Aporte(TipoComponenteIva.Etapa.INGRESOS_DIRECTOS, new BigDecimal(valor));
    }
}

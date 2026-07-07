package com.montanaritech.contable.maestros.proyecto.comision;

import static org.assertj.core.api.Assertions.assertThat;

import com.montanaritech.contable.maestros.proyecto.Proyecto;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ComisionCalculoServiceTest {

    private final ComisionCalculoService service = new ComisionCalculoService();

    @Test
    void calculaDiezPorCientoDelMontoTotal() {
        Proyecto p = new Proyecto();
        p.setMontoTotal(BigDecimal.valueOf(4239000));

        BigDecimal estimado = service.calcularEstimado(p, ComisionProyecto.BaseCalculo.MONTO_TOTAL, BigDecimal.valueOf(10));

        assertThat(estimado).isEqualByComparingTo("423900.00");
    }

    @Test
    void calculaVeintePorCientoParaComisionEspecial() {
        Proyecto p = new Proyecto();
        p.setMontoTotal(BigDecimal.valueOf(100000));

        BigDecimal estimado = service.calcularEstimado(p, ComisionProyecto.BaseCalculo.MONTO_TOTAL, BigDecimal.valueOf(20));

        assertThat(estimado).isEqualByComparingTo("20000.00");
    }

    @Test
    void montoCobradoTodaviaDaCeroPorFaltarF4_4() {
        Proyecto p = new Proyecto();
        p.setMontoTotal(BigDecimal.valueOf(100000));

        BigDecimal estimado = service.calcularEstimado(p, ComisionProyecto.BaseCalculo.MONTO_COBRADO, BigDecimal.valueOf(10));

        assertThat(estimado).isEqualByComparingTo("0.00");
    }

    @Test
    void redondeaAdosDecimales() {
        Proyecto p = new Proyecto();
        p.setMontoTotal(BigDecimal.valueOf(100));

        BigDecimal estimado = service.calcularEstimado(p, ComisionProyecto.BaseCalculo.MONTO_TOTAL, BigDecimal.valueOf(33.333));

        assertThat(estimado).isEqualByComparingTo("33.33");
    }
}

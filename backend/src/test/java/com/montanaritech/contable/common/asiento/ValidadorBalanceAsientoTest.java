package com.montanaritech.contable.common.asiento;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.montanaritech.contable.common.error.NegocioException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidadorBalanceAsientoTest {

    @Test
    void noLanzaCuandoDebeIgualaHaber() {
        List<LineaAsientoGenerada> lineas = List.of(
                new LineaAsientoGenerada("1.1.1.01", BigDecimal.TEN, BigDecimal.ZERO, "debe"),
                new LineaAsientoGenerada("1.1.1.02", BigDecimal.ZERO, BigDecimal.TEN, "haber"));

        assertThatCode(() -> ValidadorBalanceAsiento.validar(lineas)).doesNotThrowAnyException();
    }

    @Test
    void lanzaNegocioExceptionCuandoNoBalancea() {
        List<LineaAsientoGenerada> lineas = List.of(
                new LineaAsientoGenerada("1.1.1.01", BigDecimal.TEN, BigDecimal.ZERO, "debe"),
                new LineaAsientoGenerada("1.1.1.02", BigDecimal.ZERO, BigDecimal.ONE, "haber"));

        assertThatThrownBy(() -> ValidadorBalanceAsiento.validar(lineas))
                .isInstanceOf(NegocioException.class)
                .satisfies(ex -> {
                    NegocioException negocioException = (NegocioException) ex;
                    org.assertj.core.api.Assertions.assertThat(negocioException.getCodigo())
                            .isEqualTo("ASIENTO_NO_BALANCEA");
                });
    }
}

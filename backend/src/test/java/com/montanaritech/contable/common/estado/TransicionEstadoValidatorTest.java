package com.montanaritech.contable.common.estado;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.montanaritech.contable.common.error.NegocioException;
import org.junit.jupiter.api.Test;

class TransicionEstadoValidatorTest {

    @Test
    void permiteBorradorAConfirmado() {
        assertThatCode(() -> TransicionEstadoValidator.validar(EstadoDocumento.BORRADOR, EstadoDocumento.CONFIRMADO))
                .doesNotThrowAnyException();
    }

    @Test
    void permiteConfirmadoAAnulado() {
        assertThatCode(() -> TransicionEstadoValidator.validar(EstadoDocumento.CONFIRMADO, EstadoDocumento.ANULADO))
                .doesNotThrowAnyException();
    }

    @Test
    void rechazaBorradorDirectoAAnulado() {
        assertThatThrownBy(() -> TransicionEstadoValidator.validar(EstadoDocumento.BORRADOR, EstadoDocumento.ANULADO))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void anuladoEsTerminal() {
        assertThatThrownBy(() -> TransicionEstadoValidator.validar(EstadoDocumento.ANULADO, EstadoDocumento.CONFIRMADO))
                .isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> TransicionEstadoValidator.validar(EstadoDocumento.ANULADO, EstadoDocumento.BORRADOR))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void rechazaConfirmarUnConfirmado() {
        assertThatThrownBy(() -> TransicionEstadoValidator.validar(EstadoDocumento.CONFIRMADO, EstadoDocumento.CONFIRMADO))
                .isInstanceOf(NegocioException.class);
    }
}

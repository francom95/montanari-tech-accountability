package com.montanaritech.contable.common.error;

import lombok.Getter;

/**
 * Regla de negocio violada (HTTP 422). {@code codigo} es estable y forma
 * parte del catálogo documentado en F1.1 §1.3 (ASIENTO_NO_BALANCEA,
 * CUENTA_NO_IMPUTABLE, ESTADO_INVALIDO, etc.) para que el frontend pueda
 * reaccionar por código y no por el texto del mensaje.
 */
@Getter
public class NegocioException extends RuntimeException {

    private final String codigo;

    public NegocioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }
}

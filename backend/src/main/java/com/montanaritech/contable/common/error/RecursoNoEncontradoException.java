package com.montanaritech.contable.common.error;

import lombok.Getter;

/** Recurso inexistente (HTTP 404). */
@Getter
public class RecursoNoEncontradoException extends RuntimeException {

    private final String codigo;

    public RecursoNoEncontradoException(String mensaje) {
        this("RECURSO_NO_ENCONTRADO", mensaje);
    }

    public RecursoNoEncontradoException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }
}

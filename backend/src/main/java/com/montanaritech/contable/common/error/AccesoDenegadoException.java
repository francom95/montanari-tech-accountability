package com.montanaritech.contable.common.error;

import lombok.Getter;

/** Regla de autorización de negocio violada (HTTP 403), distinta del 401 de autenticación. */
@Getter
public class AccesoDenegadoException extends RuntimeException {

    private final String codigo;

    public AccesoDenegadoException(String mensaje) {
        this("ACCESO_DENEGADO", mensaje);
    }

    public AccesoDenegadoException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }
}

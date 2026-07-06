package com.montanaritech.contable.common.error;

import lombok.Getter;

/**
 * Conflicto de estado (HTTP 409): borrado con movimientos asociados (PL-1),
 * edición concurrente, duplicados, etc.
 */
@Getter
public class ConflictoException extends RuntimeException {

    private final String codigo;

    public ConflictoException(String mensaje) {
        this("CONFLICTO", mensaje);
    }

    public ConflictoException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }
}

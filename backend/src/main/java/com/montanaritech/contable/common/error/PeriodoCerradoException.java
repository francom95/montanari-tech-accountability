package com.montanaritech.contable.common.error;

/**
 * Escritura contable sobre un período cerrado sin la autorización requerida
 * (F1.1 §5: solo admin, con motivo y auditoría reforzada). Código fijo
 * {@code PERIODO_CERRADO}.
 */
public class PeriodoCerradoException extends NegocioException {

    public PeriodoCerradoException(String mensaje) {
        super("PERIODO_CERRADO", mensaje);
    }
}

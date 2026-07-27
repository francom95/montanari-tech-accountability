package com.montanaritech.contable.periodo;

/**
 * Estado de un {@link Periodo} contable mensual (F9.3). {@code EN_REVISION}
 * es puramente informativo (decisión del usuario): se comporta exactamente
 * como {@code ABIERTO} a efectos de bloqueo de escritura.
 */
public enum EstadoPeriodo {
    ABIERTO,
    EN_REVISION,
    CERRADO
}

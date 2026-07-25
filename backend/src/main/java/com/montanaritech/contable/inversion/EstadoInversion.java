package com.montanaritech.contable.inversion;

/** Estado de negocio de la inversión (F8.4) — independiente de {@code activo} (soft-delete de PL-1). */
public enum EstadoInversion {
    ACTIVA,
    RESCATADA_TOTAL,
    CANCELADA
}
